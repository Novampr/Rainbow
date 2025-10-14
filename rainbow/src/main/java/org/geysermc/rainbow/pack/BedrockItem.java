package org.geysermc.rainbow.pack;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.pack.animation.BedrockAnimation;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public record BedrockItem(ResourceLocation identifier, String textureName, ResourceLocation texture, boolean exportTexture, Optional<BedrockAttachable> attachable,
                          Optional<BedrockGeometry> geometry, Optional<BedrockAnimation> animation) {

    public List<CompletableFuture<?>> save(PackSerializer serializer, Path attachableDirectory, Path geometryDirectory, Path animationDirectory) {
        return Stream.concat(
                attachable.stream().map(present -> present.save(serializer, attachableDirectory)),
                Stream.concat(
                        geometry.stream().map(present -> present.save(serializer, geometryDirectory)),
                        animation.stream().map(present -> present.save(serializer, animationDirectory, Rainbow.fileSafeResourceLocation(identifier)))
                )
        ).toList();
    }
}
