package org.geysermc.rainbow.pack;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.pack.animation.BedrockAnimation;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public record BedrockItem(ResourceLocation identifier, String textureName, ResourceLocation texture, boolean exportTexture, Optional<BedrockAttachable> attachable,
                          Optional<BedrockGeometry> geometry, Optional<BedrockAnimation> animation) {

    public void save(Path attachableDirectory, Path geometryDirectory, Path animationDirectory) throws IOException {
        if (attachable.isPresent()) {
            attachable.get().save(attachableDirectory);
        }
        if (geometry.isPresent()) {
            geometry.get().save(geometryDirectory);
        }
        if (animation.isPresent()) {
            animation.get().save(animationDirectory, Rainbow.fileSafeResourceLocation(identifier));
        }
    }
}
