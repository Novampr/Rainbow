package org.geysermc.rainbow.datagen;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.item.ClientItem;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.pack.BedrockPack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class RainbowModelProvider extends FabricModelProvider {
    private final HolderLookup.Provider registries;
    private final PackOutput.PathProvider bedrockPackPathProvider;
    private Map<Item, ClientItem> itemInfosMap;
    private Map<ResourceLocation, ModelInstance> models;

    public RainbowModelProvider(FabricDataOutput output, HolderLookup.Provider registries) {
        super(output);
        this.registries = registries;
        bedrockPackPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "bedrock");
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput output) {
        CompletableFuture<?> vanillaModels = super.run(output);

        BedrockPack pack = BedrockPack.builder("rainbow", Path.of("geyser_mappings"), Path.of("pack"),
                new Serializer(output, registries, bedrockPackPathProvider), new ModelResolver(itemInfosMap, models)).build();

        for (Item item : itemInfosMap.keySet()) {
            pack.map(getVanillaItem(item).builtInRegistryHolder(), getVanillaDataComponentPatch(item));
        }

        return CompletableFuture.allOf(vanillaModels, pack.save());
    }

    protected abstract Item getVanillaItem(Item modded);

    protected DataComponentPatch getVanillaDataComponentPatch(Item modded) {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        modded.components().forEach(builder::set);
        return builder.build();
    }

    public void setItemInfosMap(Map<Item, ClientItem> itemInfosMap) {
        this.itemInfosMap = itemInfosMap;
    }

    public void setModels(Map<ResourceLocation, ModelInstance> models) {
        this.models = models;
    }

    private record Serializer(CachedOutput output, HolderLookup.Provider registries, PackOutput.PathProvider provider) implements PackSerializer {

        @Override
        public <T> CompletableFuture<?> saveJson(Codec<T> codec, T object, Path path) {
            ResourceLocation location = ResourceLocation.withDefaultNamespace(path.toString());
            return DataProvider.saveStable(output, registries, codec, object, provider.json(location));
        }

        @Override
        public CompletableFuture<?> saveTexture(ResourceLocation texture, Path path) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class ModelResolver implements AssetResolver {
        private final Map<ResourceLocation, ClientItem> itemInfosMap;
        private final Map<ResourceLocation, ModelInstance> models;

        private ModelResolver(Map<Item, ClientItem> itemInfosMap, Map<ResourceLocation, ModelInstance> models) {
            this.itemInfosMap = new HashMap<>();
            for (Map.Entry<Item, ClientItem> entry : itemInfosMap.entrySet()) {
                this.itemInfosMap.put(entry.getKey().builtInRegistryHolder().key().location(), entry.getValue());
            }
            this.models = models;
        }

        @Override
        public Optional<ResolvedModel> getResolvedModel(ResourceLocation location) {
            return Optional.ofNullable(models.get(location))
                    .map(instance -> BlockModel.fromStream(new StringReader(instance.get().toString())))
                    .map(model -> new ResolvedModel() {
                        @Override
                        public @NotNull UnbakedModel wrapped() {
                            return model;
                        }

                        @Override
                        public @Nullable ResolvedModel parent() {
                            return null;
                        }

                        @Override
                        public @NotNull String debugName() {
                            return location.toString();
                        }
                    }); // Not perfect since we're not resolving parents, not sure how to manage that
        }

        @Override
        public Optional<ClientItem> getClientItem(ResourceLocation location) {
            return Optional.ofNullable(itemInfosMap.get(location));
        }

        @Override
        public Optional<EquipmentClientInfo> getEquipmentInfo(ResourceKey<EquipmentAsset> key) {
            return Optional.empty(); // TODO
        }
    }
}
