package org.geysermc.rainbow.client;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.io.InputStream;
import java.util.Optional;

public class MinecraftAssetResolver implements AssetResolver {

    @Override
    public Optional<ResolvedModel> getResolvedModel(ResourceLocation location) {
        return Optional.empty();
    }

    @Override
    public Optional<ClientItem> getClientItem(ResourceLocation location) {
        return Optional.empty();
    }

    @Override
    public EquipmentClientInfo getEquipmentInfo(ResourceKey<EquipmentAsset> key) {
        return null;
    }

    @Override
    public InputStream getTexture(ResourceLocation location) {
        return null;
    }

    @Override
    public HolderLookup.Provider registries() {
        return null;
    }
}
