package org.geysermc.packgenerator.mapping.attachable;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.geysermc.packgenerator.mixin.EntityRenderDispatcherAccessor;
import org.geysermc.packgenerator.pack.attachable.BedrockAttachable;

import java.util.List;
import java.util.Optional;

public class AttachableMapper {

    public static Optional<BedrockAttachable> mapItem(ItemStack stack, ResourceLocation bedrockIdentifier) {
        // Crazy optional statement
        return Optional.ofNullable(stack.get(DataComponents.EQUIPPABLE))
                .flatMap(equippable -> {
                    EquipmentAssetManager equipmentAssets = ((EntityRenderDispatcherAccessor) Minecraft.getInstance().getEntityRenderDispatcher()).getEquipmentAssets();
                    return equippable.assetId().map(asset -> Pair.of(equippable.slot(), equipmentAssets.get(asset)));
                })
                .filter(assetInfo -> assetInfo.getSecond() != EquipmentAssetManager.MISSING)
                .map(assetInfo -> assetInfo
                        .mapSecond(info -> info.getLayers(getLayer(assetInfo.getFirst()))))
                .filter(assetInfo -> !assetInfo.getSecond().isEmpty())
                .map(assetInfo -> BedrockAttachable.equipment(bedrockIdentifier, assetInfo.getFirst(), getTexture(assetInfo.getSecond(), getLayer(assetInfo.getFirst()))));
    }

    private static EquipmentClientInfo.LayerType getLayer(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
    }

    private static String getTexture(List<EquipmentClientInfo.Layer> info, EquipmentClientInfo.LayerType layer) {
        return "entity/equipment/" + layer.getSerializedName() + "/" + info.getFirst().textureId().getPath();
    }
}
