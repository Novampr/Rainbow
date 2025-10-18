package org.geysermc.rainbow.mapping.geometry;

import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public record MappedGeometryInstance(BedrockGeometry geometry, TextureHolder stitchedTextures, TextureHolder icon) implements MappedGeometry {

    @Override
    public String identifier() {
        return geometry.definitions().getFirst().info().identifier();
    }

    @Override
    public CompletableFuture<?> save(PackSerializer serializer, Path geometryDirectory, Function<TextureHolder, CompletableFuture<?>> textureSaver) {
        return CompletableFuture.allOf(
                geometry.save(serializer, geometryDirectory),
                textureSaver.apply(stitchedTextures)
        );
    }
}
