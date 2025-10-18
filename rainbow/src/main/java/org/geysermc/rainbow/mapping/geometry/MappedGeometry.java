package org.geysermc.rainbow.mapping.geometry;

import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface MappedGeometry {

    String identifier();

    TextureHolder stitchedTextures();

    TextureHolder icon();

    CompletableFuture<?> save(PackSerializer serializer, Path geometryDirectory, Function<TextureHolder, CompletableFuture<?>> textureSaver);

    default CachedGeometry cachedCopy() {
        if (this instanceof CachedGeometry cached) {
            return cached;
        }
        return new CachedGeometry(identifier(), TextureHolder.createCopy(stitchedTextures()), TextureHolder.createCopy(icon()));
    }

    record CachedGeometry(String identifier, TextureHolder stitchedTextures, TextureHolder icon) implements MappedGeometry {

        @Override
        public CompletableFuture<?> save(PackSerializer serializer, Path geometryDirectory, Function<TextureHolder, CompletableFuture<?>> textureSaver) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
