package org.geysermc.rainbow.datagen.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.DataProvider;
import org.geysermc.rainbow.datagen.RainbowModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelProvider.class)
public abstract class ModelProviderMixin implements DataProvider {

    @WrapOperation(method = "run", at = @At(value = "NEW", target = "Lnet/minecraft/client/data/models/ModelProvider$ItemInfoCollector;"))
    public Object setItemInfosInRainbowModelProvider(Operation<?> original) {
        Object itemInfoCollector = original.call();
        if ((Object) this instanceof RainbowModelProvider rainbowModelProvider) {
            rainbowModelProvider.setItemInfosMap(((ItemInfoCollectorAccessor) itemInfoCollector).getItemInfos());
        }
        return itemInfoCollector;
    }

    @WrapOperation(method = "run", at = @At(value = "NEW", target = "Lnet/minecraft/client/data/models/ModelProvider$SimpleModelCollector;"))
    public Object setModelsInRainbowModelProvider(Operation<?> original) {
        Object simpleModelCollector = original.call();
        if ((Object) this instanceof RainbowModelProvider rainbowModelProvider) {
            rainbowModelProvider.setModels(((SimpleModelCollectorAccessor) simpleModelCollector).getModels());
        }
        return simpleModelCollector;
    }
}
