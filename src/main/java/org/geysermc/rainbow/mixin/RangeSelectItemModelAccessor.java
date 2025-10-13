package org.geysermc.rainbow.mixin;

import net.minecraft.client.renderer.item.RangeSelectItemModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RangeSelectItemModel.class)
public interface RangeSelectItemModelAccessor {

    @Invoker
    static int invokeLastIndexLessOrEqual(float[] thresholds, float value) {
        throw new AssertionError();
    }
}
