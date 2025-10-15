package org.geysermc.rainbow.pack;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public record BedrockItem(ResourceLocation identifier, String textureName, BedrockGeometryContext geometry, Optional<BedrockAttachable> attachable) {

    public List<CompletableFuture<?>> save(PackSerializer serializer, Path attachableDirectory, Path geometryDirectory, Path animationDirectory) {
        return Stream.concat(
                attachable.stream().map(present -> present.save(serializer, attachableDirectory)),
                Stream.concat(
                        geometry.geometry().stream().map(present -> present.save(serializer, geometryDirectory)),
                        geometry.animation().stream().map(context -> context.animation().save(serializer, animationDirectory, Rainbow.fileSafeResourceLocation(identifier)))
                )
        ).toList();
    }
}
