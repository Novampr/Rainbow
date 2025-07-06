package org.geysermc.packgenerator.mapping.geometry;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.packgenerator.pack.geometry.BedrockGeometry;

public record BedrockGeometryContext(BedrockGeometry geometry, ResourceLocation texture) {}
