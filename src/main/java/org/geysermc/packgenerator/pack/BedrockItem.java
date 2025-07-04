package org.geysermc.packgenerator.pack;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.packgenerator.pack.attachable.BedrockAttachable;
import org.geysermc.packgenerator.pack.geometry.BedrockGeometry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public record BedrockItem(ResourceLocation identifier, String textureName, ResourceLocation texture, Optional<BedrockAttachable> attachable, Optional<BedrockGeometry> geometry) {

    public void save(Path attachableDirectory, Path geometryDirectory) throws IOException {
        if (attachable.isPresent()) {
            attachable.get().save(attachableDirectory);
        }
        if (geometry.isPresent()) {
            geometry.get().save(geometryDirectory);
        }
    }
}
