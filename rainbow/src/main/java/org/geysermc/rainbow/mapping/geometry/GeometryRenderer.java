package org.geysermc.rainbow.mapping.geometry;

import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;

public interface GeometryRenderer {

    boolean render(ItemStack stack, Path path);
}
