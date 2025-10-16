package org.geysermc.rainbow.mapping.geometry;

import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

public record StitchedGeometry(BedrockGeometry geometry, TextureHolder stitchedTextures) {}
