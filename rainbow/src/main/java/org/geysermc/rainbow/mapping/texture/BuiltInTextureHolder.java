package org.geysermc.rainbow.mapping.texture;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

public class BuiltInTextureHolder extends TextureHolder {
    private final ResourceLocation source;

    public BuiltInTextureHolder(ResourceLocation location, ResourceLocation source) {
        super(location);
        this.source = source;
    }

    @Override
    public Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter) {
        return RainbowIO.safeIO(() -> {
            try (InputStream texture = assetResolver.openAsset(Rainbow.decorateTextureLocation(source))) {
                return texture.readAllBytes();
            } catch (FileNotFoundException | NullPointerException exception) {
                reportMissing(reporter);
                return null;
            }
        });
    }
}
