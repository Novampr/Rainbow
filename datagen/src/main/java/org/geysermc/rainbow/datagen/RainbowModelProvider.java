package org.geysermc.rainbow.datagen;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.Util;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.texture.TextureResource;
import org.geysermc.rainbow.pack.BedrockPack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class RainbowModelProvider extends FabricModelProvider {
    private static final Logger PROBLEM_LOGGER = LoggerFactory.getLogger(Rainbow.MOD_ID);

    private final CompletableFuture<HolderLookup.Provider> registries;
    private final Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos;
    private final Path outputRoot;
    private Map<Item, ClientItem> itemInfos;
    private Map<ResourceLocation, ModelInstance> models;

    protected RainbowModelProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos, ResourceLocation outputRoot) {
        super(output);
        this.registries = registries;
        this.equipmentInfos = equipmentInfos;
        this.outputRoot = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, outputRoot.getPath())
                .file(outputRoot, "").getParent();
    }

    protected RainbowModelProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos) {
        this(output, registries, equipmentInfos, ResourceLocation.withDefaultNamespace("bedrock"));
    }

    protected RainbowModelProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this(output, registries, Map.of());
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput output) {
        CompletableFuture<?> vanillaModels = super.run(output);

        CompletableFuture<BedrockPack> bedrockPack = ClientPackLoader.openClientResources()
                .thenCompose(resourceManager -> registries.thenApply(registries -> {
                    try (resourceManager) {
                        BedrockPack pack = createBedrockPack(outputRoot, new Serializer(output, registries),
                                new DatagenResolver(resourceManager, equipmentInfos, itemInfos, models)).build();

                        for (Item item : itemInfos.keySet()) {
                            pack.map(getVanillaItem(item).builtInRegistryHolder(), getVanillaDataComponentPatch(item));
                        }
                        return pack;
                    }
                }));

        return CompletableFuture.allOf(vanillaModels, bedrockPack.thenCompose(BedrockPack::save));
    }

    protected BedrockPack.Builder createBedrockPack(Path outputRoot, PackSerializer serializer, AssetResolver resolver) {
        return BedrockPack.builder("rainbow", outputRoot.resolve("geyser_mappings.json"), outputRoot.resolve("pack"), serializer, resolver)
                .withReporter(path -> new ProblemReporter.ScopedCollector(path, PROBLEM_LOGGER));
    }

    protected abstract Item getVanillaItem(Item modded);

    protected DataComponentPatch getVanillaDataComponentPatch(Item modded) {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        modded.components().forEach(builder::set);
        return builder.build();
    }

    @ApiStatus.Internal
    public void setItemInfos(Map<Item, ClientItem> itemInfos) {
        this.itemInfos = itemInfos;
    }

    @ApiStatus.Internal
    public void setModels(Map<ResourceLocation, ModelInstance> models) {
        this.models = models;
    }

    private record Serializer(CachedOutput output, HolderLookup.Provider registries) implements PackSerializer {

        @Override
        public <T> CompletableFuture<?> saveJson(Codec<T> codec, T object, Path path) {
            return DataProvider.saveStable(output, registries, codec, object, path);
        }

        @Override
        public CompletableFuture<?> saveTexture(byte[] texture, Path path) {
            return CompletableFuture.runAsync(() -> {
                try {
                    output.writeIfNeeded(path, texture, Hashing.sha1().hashBytes(texture));
                } catch (IOException exception) {
                    LOGGER.error("Failed to save texture to {}", path, exception);
                }
            }, Util.backgroundExecutor().forName("PackSerializer-saveTexture"));
        }
    }

    private static class DatagenResolver implements AssetResolver {
        private final ResourceManager resourceManager;
        private final Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos;
        private final Map<ResourceLocation, ClientItem> itemInfos;
        private final Map<ResourceLocation, ModelInstance> models;
        private final Map<ResourceLocation, Optional<ResolvedModel>> resolvedModelCache = new HashMap<>();

        private DatagenResolver(ResourceManager resourceManager, Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos,
                                Map<Item, ClientItem> itemInfos, Map<ResourceLocation, ModelInstance> models) {
            this.resourceManager = resourceManager;
            this.equipmentInfos = equipmentInfos;
            this.itemInfos = new HashMap<>();
            for (Map.Entry<Item, ClientItem> entry : itemInfos.entrySet()) {
                this.itemInfos.put(entry.getKey().builtInRegistryHolder().key().location(), entry.getValue());
            }
            this.models = models;
        }

        @Override
        public Optional<ResolvedModel> getResolvedModel(ResourceLocation location) {
            return resolvedModelCache.computeIfAbsent(location, key -> Optional.ofNullable(models.get(location))
                    .<UnbakedModel>map(instance -> BlockModel.fromStream(new StringReader(instance.get().toString())))
                    .or(() -> {
                        if (location.equals(ItemModelGenerator.GENERATED_ITEM_MODEL_ID)) {
                            return Optional.of(new ItemModelGenerator());
                        }
                        return Optional.empty();
                    })
                    .or(() -> RainbowIO.safeIO(() -> {
                        try (BufferedReader reader = resourceManager.openAsReader(location.withPrefix("models/").withSuffix(".json"))) {
                            return BlockModel.fromStream(reader);
                        }
                    }))
                    .map(model -> new ResolvedModel() {
                        @Override
                        public @NotNull UnbakedModel wrapped() {
                            return model;
                        }

                        @Override
                        public @Nullable ResolvedModel parent() {
                            return Optional.ofNullable(model.parent()).flatMap(parent -> getResolvedModel(parent)).orElse(null);
                        }

                        @Override
                        public @NotNull String debugName() {
                            return location.toString();
                        }
                    }));
        }

        @Override
        public Optional<ClientItem> getClientItem(ResourceLocation location) {
            return Optional.ofNullable(itemInfos.get(location));
        }

        @Override
        public Optional<EquipmentClientInfo> getEquipmentInfo(ResourceKey<EquipmentAsset> key) {
            return Optional.ofNullable(equipmentInfos.get(key));
        }

        @Override
        public Optional<TextureResource> getBlockTexture(ResourceLocation location) {
            return resourceManager.getResource(Rainbow.decorateTextureLocation(location))
                    .flatMap(resource -> RainbowIO.safeIO(() -> {
                        Optional<AnimationMetadataSection> animationMetadata = resource.metadata().getSection(AnimationMetadataSection.TYPE);
                        try (InputStream textureStream = resource.open()) {
                            NativeImage texture = NativeImage.read(textureStream);
                            return new TextureResource(texture, animationMetadata.map(animation -> animation.calculateFrameSize(texture.getWidth(), texture.getHeight())));
                        }
                    }));
        }
    }
}
