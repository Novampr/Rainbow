package org.geysermc.rainbow.mixin;

import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FaceBakery.class)
public interface FaceBakeryAccessor {

    @Invoker
    static BlockElementFace.UVs invokeDefaultFaceUV(Vector3fc posFrom, Vector3fc posTo, Direction facing) {
        throw new AssertionError();
    }
}
