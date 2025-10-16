package org.geysermc.rainbow.mapping;

import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.definition.GeyserMappings;
import org.geysermc.rainbow.pack.PackPaths;

import java.util.Optional;

public record PackContext(GeyserMappings mappings, PackPaths paths, BedrockItemConsumer itemConsumer, AssetResolver assetResolver,
                          Optional<GeometryRenderer> geometryRenderer, boolean reportSuccesses) {}
