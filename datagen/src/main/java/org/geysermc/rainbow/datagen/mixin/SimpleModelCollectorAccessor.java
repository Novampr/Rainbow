package org.geysermc.rainbow.datagen.mixin;

import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelProvider.SimpleModelCollector.class)
public interface SimpleModelCollectorAccessor {

    @Accessor
    Map<ResourceLocation, ModelInstance> getModels();
}
