package org.geysermc.rainbow.mapping.geometry;

import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;

public class NoopGeometryRenderer implements GeometryRenderer {
    public static final NoopGeometryRenderer INSTANCE = new NoopGeometryRenderer();

    @Override
    public boolean render(ItemStack stack, Path path) {
        return false;
    }
}
