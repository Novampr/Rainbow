package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.image.NativeImageUtil;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.util.Objects;
import java.util.Optional;

public class BuiltInTextureHolder extends TextureHolder {
    private final ResourceLocation atlas;
    private final ResourceLocation source;

    public BuiltInTextureHolder(ResourceLocation location, ResourceLocation atlas, ResourceLocation source) {
        super(location);
        this.atlas = atlas;
        this.source = source;
    }

    @Override
    public Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter) {
        return RainbowIO.safeIO(() -> {
            try (TextureResource texture = assetResolver.getTexture(atlas, source).orElse(null)) {
                Objects.requireNonNull(texture);
                try (NativeImage firstFrame = texture.getFirstFrame(false)) {
                    return NativeImageUtil.writeToByteArray(firstFrame);
                }
            } catch (NullPointerException exception) {
                reportMissing(reporter);
                return null;
            }
        });
    }
}
