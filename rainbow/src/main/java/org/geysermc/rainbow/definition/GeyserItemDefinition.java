package org.geysermc.rainbow.definition;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface GeyserItemDefinition extends GeyserMapping {

    GeyserBaseDefinition base();

    boolean conflictsWith(Optional<ResourceLocation> parentModel, GeyserItemDefinition other);

    @Override
    default int compareTo(@NotNull GeyserMapping other) {
        if (other instanceof GeyserItemDefinition itemDefinition) {
            return base().bedrockIdentifier().compareTo(itemDefinition.base().bedrockIdentifier());
        }
        return -1; // Groups are always greater than individual mappings
    }
}
