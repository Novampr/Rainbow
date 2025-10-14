package org.geysermc.rainbow;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class Rainbow {

    public static final String MOD_ID = "rainbow";
    public static final String MOD_NAME = "Rainbow";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation getModdedLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String fileSafeResourceLocation(ResourceLocation location) {
        return location.toString().replace(':', '.').replace('/', '_');
    }
}
