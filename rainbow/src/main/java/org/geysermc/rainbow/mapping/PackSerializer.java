package org.geysermc.rainbow.mapping;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public interface PackSerializer {

    <T> CompletableFuture<?> saveJson(Codec<T> codec, T object, Path path);

    CompletableFuture<?> saveTexture(ResourceLocation texture, Path path);
}
