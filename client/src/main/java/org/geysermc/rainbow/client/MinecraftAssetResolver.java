package org.geysermc.rainbow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.geysermc.rainbow.client.accessor.ResolvedModelAccessor;
import org.geysermc.rainbow.client.mixin.EntityRenderDispatcherAccessor;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.texture.TextureResource;
import org.geysermc.rainbow.mixin.SpriteContentsAccessor;

import java.util.Optional;

public class MinecraftAssetResolver implements AssetResolver {
    private final ModelManager modelManager;
    private final EquipmentAssetManager equipmentAssetManager;
    private final AtlasManager atlasManager;

    public MinecraftAssetResolver(Minecraft minecraft) {
        modelManager = minecraft.getModelManager();
        equipmentAssetManager = ((EntityRenderDispatcherAccessor) minecraft.getEntityRenderDispatcher()).getEquipmentAssets();
        atlasManager = minecraft.getAtlasManager();
    }

    @Override
    public Optional<ResolvedModel> getResolvedModel(ResourceLocation location) {
        return ((ResolvedModelAccessor) modelManager).rainbow$getResolvedModel(location);
    }

    @Override
    public Optional<ClientItem> getClientItem(ResourceLocation location) {
        return ((ResolvedModelAccessor) modelManager).rainbow$getClientItem(location);
    }

    @Override
    public Optional<EquipmentClientInfo> getEquipmentInfo(ResourceKey<EquipmentAsset> key) {
        return Optional.of(equipmentAssetManager.get(key));
    }

    @Override
    public Optional<TextureResource> getBlockTexture(ResourceLocation location) {
        SpriteContents contents = atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(location).contents();
        return Optional.of(new TextureResource(((SpriteContentsAccessor) contents).getOriginalImage(), contents.width(), contents.height()));
    }
}
