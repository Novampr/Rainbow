package org.geysermc.rainbow.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface NativeImageAccessor {

    @Invoker
    void invokeCheckAllocated();

    @Invoker
    boolean invokeWriteToChannel(WritableByteChannel channel) throws IOException;
}
