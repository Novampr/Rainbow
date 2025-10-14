package org.geysermc.rainbow.mapping;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.definition.GeyserMappings;
import org.geysermc.rainbow.pack.PackPaths;

import java.util.function.Consumer;

public record PackContext(GeyserMappings mappings, PackPaths paths, BedrockItemConsumer itemConsumer, AssetResolver assetResolver,
                          GeometryRenderer geometryRenderer, Consumer<ResourceLocation> additionalTextureConsumer,
                          boolean reportSuccesses) {
}
