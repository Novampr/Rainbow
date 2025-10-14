package org.geysermc.rainbow;

import net.kyori.adventure.key.Key;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("PatternValidation")
public interface KeyUtil {

    static Key resourceLocationToKey(ResourceLocation location) {
        return Key.key(location.getNamespace(), location.getPath());
    }

    static ResourceLocation keyToResourceLocation(Key key) {
        return ResourceLocation.fromNamespaceAndPath(key.namespace(), key.value());
    }
}
