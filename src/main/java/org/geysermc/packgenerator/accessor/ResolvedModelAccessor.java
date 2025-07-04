package org.geysermc.packgenerator.accessor;

import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

// Implemented on ModelManager, since this class doesn't keep the resolved models after baking, we have to store it manually
public interface ResolvedModelAccessor {

    Optional<ResolvedModel> geyser_mappings_generator$getResolvedModel(ResourceLocation location);
}
