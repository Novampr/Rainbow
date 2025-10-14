package org.geysermc.rainbow.datagen.mixin;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(targets = "net.minecraft.client.data.models.ModelProvider$ItemInfoCollector")
public interface ItemInfoCollectorAccessor {

    @Accessor
    Map<Item, ClientItem> getItemInfos();
}
