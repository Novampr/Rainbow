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

    // TODO rename remove file
    public static String fileSafeResourceLocation(ResourceLocation location) {
        return location.toString().replace(':', '.').replace('/', '_');
    }

    public static ResourceLocation decorateResourceLocation(ResourceLocation location, String type, String extension) {
        return location.withPath(path -> type + "/" + path + "." + extension);
    }

    public static ResourceLocation decorateTextureLocation(ResourceLocation location) {
        return decorateResourceLocation(location, "textures", "png");
    }
}
