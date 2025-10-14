package org.geysermc.rainbow.mapping;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.equipment.EquipmentAsset;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface AssetResolver {

    Optional<ResolvedModel> getResolvedModel(ResourceLocation location);

    Optional<ClientItem> getClientItem(ResourceLocation location);

    Optional<EquipmentClientInfo> getEquipmentInfo(ResourceKey<EquipmentAsset> key);

    InputStream getTexture(ResourceLocation location) throws IOException;

    HolderLookup.Provider registries();
}
