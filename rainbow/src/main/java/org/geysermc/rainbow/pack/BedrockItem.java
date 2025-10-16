package org.geysermc.rainbow.pack;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.attachable.AttachableMapper;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.geometry.TextureHolder;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public record BedrockItem(ResourceLocation identifier, String textureName, BedrockGeometryContext geometryContext, AttachableMapper.AttachableCreator attachableCreator) {

    public CompletableFuture<?> save(PackSerializer serializer, Path attachableDirectory, Path geometryDirectory, Path animationDirectory,
                                     Function<TextureHolder, CompletableFuture<?>> textureSaver) {
        return CompletableFuture.supplyAsync(() -> geometryContext.geometry().map(Supplier::get))
                .thenCompose(stitchedGeometry -> {
                    List<TextureHolder> attachableTextures = new ArrayList<>();
                    Optional<BedrockAttachable> createdAttachable = attachableCreator.create(identifier, stitchedGeometry, attachableTextures::add);
                    return CompletableFuture.allOf(
                            textureSaver.apply(geometryContext.icon()),
                            createdAttachable.map(attachable -> attachable.save(serializer, attachableDirectory)).orElse(noop()),
                            CompletableFuture.allOf(attachableTextures.stream().map(textureSaver).toArray(CompletableFuture[]::new)),
                            stitchedGeometry.map(BedrockGeometryContext.StitchedGeometry::geometry).map(geometry -> geometry.save(serializer, geometryDirectory)).orElse(noop()),
                            stitchedGeometry.map(BedrockGeometryContext.StitchedGeometry::stitchedTextures).map(textureSaver).orElse(noop()),
                            geometryContext.animation().map(context -> context.animation().save(serializer, animationDirectory, Rainbow.fileSafeResourceLocation(identifier))).orElse(noop())
                    );
                });
    }

    private static <T> CompletableFuture<T> noop() {
        return CompletableFuture.completedFuture(null);
    }
}
