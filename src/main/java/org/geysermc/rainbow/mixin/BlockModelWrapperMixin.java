package org.geysermc.rainbow.mixin;

import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.accessor.BlockModelWrapperLocationAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelWrapper.class)
public abstract class BlockModelWrapperMixin implements ItemModel, BlockModelWrapperLocationAccessor {

    @Unique
    private ResourceLocation modelOrigin;

    @Override
    public ResourceLocation geyser_mappings_generator$getModelOrigin() {
        return modelOrigin;
    }

    @Override
    public void geyser_mappings_generator$setModelOrigin(ResourceLocation model) {
        modelOrigin = model;
    }

    @Mixin(BlockModelWrapper.Unbaked.class)
    public abstract static class UnbakedMixin implements Unbaked {

        @Shadow
        @Final
        private ResourceLocation model;

        @Inject(method = "bake", at = @At("TAIL"))
        public void setModelOrigin(BakingContext context, CallbackInfoReturnable<ItemModel> callbackInfoReturnable) {
            ((BlockModelWrapperLocationAccessor) callbackInfoReturnable.getReturnValue()).geyser_mappings_generator$setModelOrigin(model);
        }
    }
}
