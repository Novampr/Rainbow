package org.geysermc.rainbow.definition;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface GeyserItemDefinition extends GeyserMapping {

    GeyserBaseDefinition base();

    boolean conflictsWith(Optional<ResourceLocation> parentModel, GeyserItemDefinition other);
}
