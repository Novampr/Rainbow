package org.geysermc.rainbow.datagen.mixin;

import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelProvider.ItemInfoCollector.class)
public interface ItemInfoCollectorAccessor {

    @Accessor
    Map<Item, ClientItem> getItemInfos();
}
