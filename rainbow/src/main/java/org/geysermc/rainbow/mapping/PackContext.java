package org.geysermc.rainbow.mapping;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.mapping.geyser.GeyserMappings;

import java.nio.file.Path;
import java.util.function.Consumer;

public record PackContext(GeyserMappings mappings, Path packPath, BedrockItemConsumer itemConsumer, AssetResolver assetResolver,
                          GeometryRenderer geometryRenderer, Consumer<ResourceLocation> additionalTextureConsumer) {
}
