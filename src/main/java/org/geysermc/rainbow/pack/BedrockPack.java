package org.geysermc.rainbow.pack;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.io.IOUtils;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.PackConstants;
import org.geysermc.rainbow.mapping.BedrockItemMapper;
import org.geysermc.rainbow.mapping.geyser.GeyserMappings;
import org.geysermc.rainbow.mixin.SplashRendererAccessor;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BedrockPack {
    private static final Path EXPORT_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("rainbow");
    private static final Path PACK_DIRECTORY = Path.of("pack");
    private static final Path ATTACHABLES_DIRECTORY = Path.of("attachables");
    private static final Path GEOMETRY_DIRECTORY = Path.of("models/entity");
    private static final Path ANIMATION_DIRECTORY = Path.of("animations");

    private static final Path MAPPINGS_FILE = Path.of("geyser_mappings.json");
    private static final Path MANIFEST_FILE = Path.of("manifest.json");
    private static final Path ITEM_ATLAS_FILE = Path.of("textures/item_texture.json");

    private static final Path REPORT_FILE = Path.of("report.txt");

    private final String name;
    private final Path exportPath;
    private final Path packPath;
    private final PackManifest manifest;
    private final GeyserMappings mappings;
    private final BedrockTextures.Builder itemTextures;

    private final Set<BedrockItem> bedrockItems = new HashSet<>();
    private final Set<ResourceLocation> texturesToExport = new HashSet<>();
    private final Set<ResourceLocation> modelsMapped = new HashSet<>();

    private final ProblemReporter.Collector reporter;

    public BedrockPack(String name) throws IOException {
        this.name = name;

        exportPath = createPackDirectory(name);
        packPath = exportPath.resolve(PACK_DIRECTORY);
        mappings = CodecUtil.readOrCompute(GeyserMappings.CODEC, exportPath.resolve(MAPPINGS_FILE), GeyserMappings::new);
        manifest = CodecUtil.readOrCompute(PackManifest.CODEC, packPath.resolve(MANIFEST_FILE), () -> defaultManifest(name)).increment();
        itemTextures = CodecUtil.readOrCompute(BedrockTextureAtlas.ITEM_ATLAS_CODEC, packPath.resolve(ITEM_ATLAS_FILE),
                () -> BedrockTextureAtlas.itemAtlas(name, BedrockTextures.builder())).textures().toBuilder();

        reporter = new ProblemReporter.Collector(() -> "Bedrock pack " + name);
    }

    public String name() {
        return name;
    }

    public Optional<Boolean> map(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        Optional<? extends ResourceLocation> patchedModel = stack.getComponentsPatch().get(DataComponents.ITEM_MODEL);
        //noinspection OptionalAssignedToNull - annoying Mojang
        if (patchedModel == null || patchedModel.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation model = patchedModel.get();
        if (!modelsMapped.add(model)) {
            return Optional.empty();
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

        BedrockItemMapper.tryMapStack(stack, model, mapReporter, mappings, packPath, bedrockItem -> {
            itemTextures.withItemTexture(bedrockItem);
            if (bedrockItem.exportTexture()) {
                texturesToExport.add(bedrockItem.texture());
            }
            bedrockItems.add(bedrockItem);
        }, texturesToExport::add);
        return Optional.of(problems.get());
    }

    public boolean save() {
        boolean success = true;

        try {
            CodecUtil.trySaveJson(GeyserMappings.CODEC, mappings, exportPath.resolve(MAPPINGS_FILE));
            CodecUtil.trySaveJson(PackManifest.CODEC, manifest, packPath.resolve(MANIFEST_FILE));
            CodecUtil.trySaveJson(BedrockTextureAtlas.CODEC, BedrockTextureAtlas.itemAtlas(name, itemTextures), packPath.resolve(ITEM_ATLAS_FILE));
        } catch (IOException exception) {
            reporter.forChild(() -> "saving Geyser mappings, pack manifest, and texture atlas ").report(() -> "failed to save to pack: " + exception);
            success = false;
        }
        for (BedrockItem item : bedrockItems) {
            try {
                item.save(packPath.resolve(ATTACHABLES_DIRECTORY), packPath.resolve(GEOMETRY_DIRECTORY), packPath.resolve(ANIMATION_DIRECTORY));
            } catch (IOException exception) {
                reporter.forChild(() -> "files for bedrock item " + item.identifier() + " ").report(() -> "failed to save to pack: " + exception);
                success = false;
            }
        }

        for (ResourceLocation texture : texturesToExport) {
            texture = texture.withPath(path -> "textures/" + path + ".png");
            try (InputStream inputTexture = Minecraft.getInstance().getResourceManager().open(texture)) {
                Path texturePath = packPath.resolve(texture.getPath());
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

        try {
            Files.writeString(exportPath.resolve(REPORT_FILE), createPackSummary());
        } catch (IOException exception) {
            // TODO log
        }
        return success;
    }

    private String createPackSummary() {
        return """
-- PACK GENERATION REPORT --
// %s

Generated pack: %s
Mappings written: %d
Item texture atlas size: %d
Attachables: %d
Textures tried to export: %d

-- PROBLEM REPORT --
%s
""".formatted(randomSummaryComment(), name, mappings.size(), itemTextures.build().size(),
                bedrockItems.size(), texturesToExport.size(), reporter.getTreeReport());
    }

    private static String randomSummaryComment() {
        SplashRenderer splash = Minecraft.getInstance().getSplashManager().getSplash();
        if (splash == null) {
            return "Undefined Undefined :(";
        }
        return ((SplashRendererAccessor) splash).getSplash();
    }

    private static Path createPackDirectory(String name) throws IOException {
        Path path = EXPORT_DIRECTORY.resolve(name);
        CodecUtil.ensureDirectoryExists(path);
        return path;
    }

    private static PackManifest defaultManifest(String name) {
        return new PackManifest(new PackManifest.Header(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(0), PackConstants.ENGINE_VERSION),
                List.of(new PackManifest.Module(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(0))));
    }
}
