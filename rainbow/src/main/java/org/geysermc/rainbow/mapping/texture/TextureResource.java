package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;

import java.util.Optional;

public record TextureResource(NativeImage texture, Optional<FrameSize> frameSize) implements AutoCloseable {

    public TextureResource(NativeImage texture, int width, int height) {
        this(texture, texture.getWidth() != width || texture.getHeight() != height ? Optional.of(new FrameSize(width, height)) : Optional.empty());
    }

    public NativeImage getFirstFrame(boolean copy) {
        if (frameSize.isEmpty() && !copy) {
            return texture;
        }
        FrameSize size = sizeOfFrame();
        NativeImage firstFrame = new NativeImage(size.width(), size.height(), false);
        texture.copyRect(firstFrame, 0, 0, 0, 0, size.width(), size.height(), false, false);
        return firstFrame;
    }

    public FrameSize sizeOfFrame() {
        return frameSize.orElseGet(() -> new FrameSize(texture.getWidth(), texture.getHeight()));
    }

    @Override
    public void close() {
        texture.close();
    }
}
