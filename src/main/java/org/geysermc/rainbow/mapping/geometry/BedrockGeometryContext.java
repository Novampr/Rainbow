package org.geysermc.rainbow.mapping.geometry;

import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

public record BedrockGeometryContext(BedrockGeometry geometry, ResourceLocation texture) {}
