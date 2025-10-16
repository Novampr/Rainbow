package org.geysermc.rainbow.mapping.geometry;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.world.item.ItemStack;

public interface GeometryRenderer {

    NativeImage render(ItemStack stack);
}
