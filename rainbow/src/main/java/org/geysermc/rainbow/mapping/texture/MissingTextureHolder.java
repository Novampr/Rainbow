package org.geysermc.rainbow.mapping.texture;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.util.Optional;

public class MissingTextureHolder extends TextureHolder {

    public MissingTextureHolder(ResourceLocation location) {
        super(location);
    }

    @Override
    public Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter) {
        reportMissing(reporter);
        return Optional.empty();
    }
}
