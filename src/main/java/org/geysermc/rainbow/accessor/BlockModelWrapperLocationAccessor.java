package org.geysermc.rainbow.accessor;

import net.minecraft.resources.ResourceLocation;

// Implemented on BlockModelWrapper, since this class doesn't store its model after baking, we have to store it manually
public interface BlockModelWrapperLocationAccessor {

    ResourceLocation rainbow$getModelOrigin();

    void rainbow$setModelOrigin(ResourceLocation model);
}
