package org.geysermc.rainbow.mapping.geometry;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Supplier;

public record TextureHolder(ResourceLocation location, Optional<Supplier<NativeImage>> supplier) {

    public TextureHolder(ResourceLocation location, Supplier<NativeImage> supplier) {
        this(location, Optional.of(supplier));
    }

    public TextureHolder(ResourceLocation location) {
        this(location, Optional.empty());
    }
}
