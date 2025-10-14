package org.geysermc.rainbow.pack;

import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.apache.commons.io.IOUtils;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.PackConstants;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.BedrockItemMapper;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.mapping.geometry.NoopGeometryRenderer;
import org.geysermc.rainbow.definition.GeyserMappings;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

public class BedrockPack {
    private final String name;
    private final PackPaths paths;

    private final BedrockTextures.Builder itemTextures = BedrockTextures.builder();
    private final Set<BedrockItem> bedrockItems = new HashSet<>();
    private final Set<ResourceLocation> texturesToExport = new HashSet<>();
    private final Set<ResourceLocation> modelsMapped = new HashSet<>();
    private final IntSet customModelDataMapped = new IntOpenHashSet();

    private final PackContext context;
    private final PackManifest manifest;
    private final ProblemReporter.Collector reporter;

    public BedrockPack(String name, PackPaths paths, AssetResolver assetResolver,
                       GeometryRenderer geometryRenderer, ProblemReporter.Collector reporter,
                       boolean reportSuccesses) {
        this.name = name;
        this.paths = paths;

        // Not reading existing item mappings/texture atlas for now since that doesn't work all that well yet
        this.context = new PackContext(new GeyserMappings(), paths, item -> {
            itemTextures.withItemTexture(item);
            if (item.exportTexture()) {
                texturesToExport.add(item.texture());
            }
            bedrockItems.add(item);
        }, assetResolver, geometryRenderer, texturesToExport::add, reportSuccesses);
        manifest = defaultManifest(name);
        this.reporter = reporter;
    }

    public String name() {
        return name;
    }

    public MappingResult map(ItemStack stack) {
        if (stack.isEmpty()) {
            return MappingResult.NONE_MAPPED;
        }

        AtomicBoolean problems = new AtomicBoolean();
        ProblemReporter mapReporter = new ProblemReporter() {

            @Override
            public @NotNull ProblemReporter forChild(PathElement child) {
                return reporter.forChild(child);
            }

            @Override
            public void report(Problem problem) {
                problems.set(true);
                reporter.report(problem);
            }
        };

        Optional<? extends ResourceLocation> patchedModel = stack.getComponentsPatch().get(DataComponents.ITEM_MODEL);
        //noinspection OptionalAssignedToNull - annoying Mojang
        if (patchedModel == null || patchedModel.isEmpty()) {
            CustomModelData customModelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            Float firstNumber;
            if (customModelData == null || (firstNumber = customModelData.getFloat(0)) == null
                    || !customModelDataMapped.add((firstNumber.intValue()))) {
                return MappingResult.NONE_MAPPED;
            }

            BedrockItemMapper.tryMapStack(stack, firstNumber.intValue(), mapReporter, context);
        } else {
            ResourceLocation model = patchedModel.get();
            if (!modelsMapped.add(model)) {
                return MappingResult.NONE_MAPPED;
            }

            BedrockItemMapper.tryMapStack(stack, model, mapReporter, context);
        }

        return problems.get() ? MappingResult.PROBLEMS_OCCURRED : MappingResult.MAPPED_SUCCESSFULLY;
    }

    public MappingResult map(Holder<Item> item, DataComponentPatch patch) {
        ItemStack stack = new ItemStack(item);
        stack.applyComponents(patch);
        return map(stack);
    }

    public boolean save() {
        boolean success = true;

        try {
            CodecUtil.trySaveJson(GeyserMappings.CODEC, context.mappings(), paths.mappings(), RegistryOps.create(JsonOps.INSTANCE, context.assetResolver().registries()));
            CodecUtil.trySaveJson(PackManifest.CODEC, manifest, paths.manifest());
            CodecUtil.trySaveJson(BedrockTextureAtlas.CODEC, BedrockTextureAtlas.itemAtlas(name, itemTextures), paths.itemAtlas());
        } catch (IOException | NullPointerException exception) {
            reporter.forChild(() -> "saving Geyser mappings, pack manifest, and texture atlas ").report(() -> "failed to save to pack: " + exception);
            success = false;
        }
        for (BedrockItem item : bedrockItems) {
            try {
                item.save(paths.attachables(), paths.geometry(), paths.animation());
            } catch (IOException exception) {
                reporter.forChild(() -> "files for bedrock item " + item.identifier() + " ").report(() -> "failed to save to pack: " + exception);
                success = false;
            }
        }

        for (ResourceLocation texture : texturesToExport) {
            texture = texture.withPath(path -> "textures/" + path + ".png");
            try (InputStream inputTexture = context.assetResolver().getTexture(texture)) {
                Path texturePath = paths.packRoot().resolve(texture.getPath());
                CodecUtil.ensureDirectoryExists(texturePath.getParent());
                try (OutputStream outputTexture = new FileOutputStream(texturePath.toFile())) {
                    IOUtils.copy(inputTexture, outputTexture);
                }
            } catch (IOException exception) {
                ResourceLocation finalTexture = texture;
                reporter.forChild(() -> "texture " + finalTexture + " ").report(() -> "failed to save to pack: " + exception);
                success = false;
            }
        }

        if (paths.zipOutput().isPresent()) {
            try {
                CodecUtil.tryZipDirectory(paths.packRoot(), paths.zipOutput().get());
            } catch (IOException exception) {
                success = false;
            }
        }

        return success;
    }

    public int getMappings() {
        return context.mappings().size();
    }

    public Set<BedrockItem> getBedrockItems() {
        return Set.copyOf(bedrockItems);
    }

    public int getItemTextureAtlasSize() {
        return itemTextures.build().size();
    }

    public int getAdditionalExportedTextures() {
        return texturesToExport.size();
    }

    public ProblemReporter.Collector getReporter() {
        return reporter;
    }

    private static PackManifest defaultManifest(String name) {
        return new PackManifest(new PackManifest.Header(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(0), PackConstants.ENGINE_VERSION),
                List.of(new PackManifest.Module(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(0))));
    }

    public static Builder builder(String name, Path mappingsPath, Path packRootPath, AssetResolver assetResolver) {
        return new Builder(name, mappingsPath, packRootPath, assetResolver);
    }

    public static class Builder {
        private static final Path ATTACHABLES_DIRECTORY = Path.of("attachables");
        private static final Path GEOMETRY_DIRECTORY = Path.of("models/entity");
        private static final Path ANIMATION_DIRECTORY = Path.of("animations");

        private static final Path MANIFEST_FILE = Path.of("manifest.json");
        private static final Path ITEM_ATLAS_FILE = Path.of("textures/item_texture.json");

        private final String name;
        private final Path mappingsPath;
        private final Path packRootPath;
        private final AssetResolver assetResolver;
        private UnaryOperator<Path> attachablesPath = resolve(ATTACHABLES_DIRECTORY);
        private UnaryOperator<Path> geometryPath = resolve(GEOMETRY_DIRECTORY);
        private UnaryOperator<Path> animationPath = resolve(ANIMATION_DIRECTORY);
        private UnaryOperator<Path> manifestPath = resolve(MANIFEST_FILE);
        private UnaryOperator<Path> itemAtlasPath = resolve(ITEM_ATLAS_FILE);
        private Path packZipFile = null;
        private GeometryRenderer geometryRenderer = NoopGeometryRenderer.INSTANCE;
        private ProblemReporter.Collector reporter;
        private boolean reportSuccesses = false;

        public Builder(String name, Path mappingsPath, Path packRootPath, AssetResolver assetResolver) {
            this.name = name;
            this.mappingsPath = mappingsPath;
            this.packRootPath = packRootPath;
            this.reporter = new ProblemReporter.Collector(() -> "Bedrock pack " + name + " ");
            this.assetResolver = assetResolver;
        }

        public Builder withAttachablesPath(Path absolute) {
            return withAttachablesPath(path -> absolute);
        }

        public Builder withAttachablesPath(UnaryOperator<Path> path) {
            attachablesPath = path;
            return this;
        }

        public Builder withGeometryPath(Path absolute) {
            return withGeometryPath(path -> absolute);
        }

        public Builder withGeometryPath(UnaryOperator<Path> path) {
            geometryPath = path;
            return this;
        }

        public Builder withAnimationPath(Path absolute) {
            return withAnimationPath(path -> absolute);
        }

        public Builder withAnimationPath(UnaryOperator<Path> path) {
            animationPath = path;
            return this;
        }

        public Builder withManifestPath(Path absolute) {
            return withManifestPath(path -> absolute);
        }

        public Builder withManifestPath(UnaryOperator<Path> path) {
            manifestPath = path;
            return this;
        }

        public Builder withItemAtlasPath(Path absolute) {
            return withItemAtlasPath(path -> absolute);
        }

        public Builder withItemAtlasPath(UnaryOperator<Path> path) {
            itemAtlasPath = path;
            return this;
        }

        public Builder withPackZipFile(Path absolute) {
            packZipFile = absolute;
            return this;
        }

        public Builder withGeometryRenderer(GeometryRenderer renderer) {
            geometryRenderer = renderer;
            return this;
        }

        public Builder withReporter(ProblemReporter.Collector reporter) {
            this.reporter = reporter;
            return this;
        }

        public Builder reportSuccesses() {
            this.reportSuccesses = true;
            return this;
        }

        public BedrockPack build() {
            PackPaths paths = new PackPaths(mappingsPath, packRootPath, attachablesPath.apply(packRootPath),
                    geometryPath.apply(packRootPath), animationPath.apply(packRootPath), manifestPath.apply(packRootPath),
                    itemAtlasPath.apply(packRootPath), Optional.ofNullable(packZipFile));
            return new BedrockPack(name, paths, assetResolver, geometryRenderer, reporter, reportSuccesses);
        }

        private static UnaryOperator<Path> resolve(Path child) {
            return root -> root.resolve(child);
        }
    }

    public enum MappingResult {
        NONE_MAPPED,
        MAPPED_SUCCESSFULLY,
        PROBLEMS_OCCURRED
    }
}
