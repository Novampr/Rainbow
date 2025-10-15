package org.geysermc.rainbow.mapping;

import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.definition.GeyserMappings;
import org.geysermc.rainbow.mapping.geometry.TextureHolder;
import org.geysermc.rainbow.pack.PackPaths;

import java.util.function.Consumer;

public record PackContext(GeyserMappings mappings, PackPaths paths, BedrockItemConsumer itemConsumer, AssetResolver assetResolver,
                          GeometryRenderer geometryRenderer, Consumer<TextureHolder> additionalTextureConsumer,
                          boolean reportSuccesses) {
}
