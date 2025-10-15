package org.geysermc.rainbow.image;

import com.mojang.blaze3d.platform.NativeImage;
import org.geysermc.rainbow.mixin.NativeImageAccessor;
import org.lwjgl.stb.STBImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class NativeImageUtil {

    // Adjusted NativeImage#writeToFile
    @SuppressWarnings("DataFlowIssue")
    public static byte[] writeToByteArray(NativeImage image) throws IOException {
        if (!image.format().supportedByStb()) {
            throw new UnsupportedOperationException("Don't know how to write format " + image.format());
        } else {
            ((NativeImageAccessor) (Object) image).invokeCheckAllocated();
            Pipe pipe = Pipe.open();
            try (WritableByteChannel outputChannel = pipe.sink()) {
                if (!((NativeImageAccessor) (Object) image).invokeWriteToChannel(outputChannel)) {
                    throw new IOException("Could not write image to pipe: " + STBImage.stbi_failure_reason());
                }
            }

            try (ReadableByteChannel inputChannel = pipe.source()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                while (inputChannel.read(buffer) != -1) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        bytes.write(buffer.get());
                    }
                    buffer.clear();
                }
                return bytes.toByteArray();
            }
        }
    }
}
