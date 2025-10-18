package org.geysermc.rainbow.mapping.geometry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

public interface GeometryRenderer {

    TextureHolder render(ResourceLocation location, ItemStack stack);
}
