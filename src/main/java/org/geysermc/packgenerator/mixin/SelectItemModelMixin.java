package org.geysermc.packgenerator.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import org.geysermc.packgenerator.accessor.SelectItemModelCaseAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SelectItemModel.class)
public abstract class SelectItemModelMixin<T> implements ItemModel, SelectItemModelCaseAccessor<T> {

    @Unique
    private Object2ObjectMap<T, ItemModel> cases;

    @Override
    public Object2ObjectMap<T, ItemModel> geyser_mappings_generator$getCases() {
        return cases;
    }

    @Override
    public void geyser_mappings_generator$setCases(Object2ObjectMap<T, ItemModel> cases) {
        this.cases = cases;
    }

    @Mixin(SelectItemModel.UnbakedSwitch.class)
    public abstract static class UnbakedSwitchMixin<P extends SelectItemModelProperty<T>, T> {

        @Inject(method = "bake", at = @At("TAIL"))
        public void setCases(BakingContext bakingContext, ItemModel model, CallbackInfoReturnable<ItemModel> callbackInfoReturnable,
                             @Local Object2ObjectMap<T, ItemModel> cases) {
            //noinspection unchecked
            ((SelectItemModelCaseAccessor<T>) callbackInfoReturnable.getReturnValue()).geyser_mappings_generator$setCases(cases);
        }
    }
}
