package org.geysermc.rainbow.mixin;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RangeSelectItemModel.class)
public interface RangeSelectItemModelAccessor {

    @Accessor
    RangeSelectItemModelProperty getProperty();

    @Accessor
    float getScale();

    @Accessor
    float[] getThresholds();

    @Accessor
    ItemModel[] getModels();

    @Accessor
    ItemModel getFallback();

    @Invoker
    static int invokeLastIndexLessOrEqual(float[] thresholds, float value) {
        throw new AssertionError();
    }
}
