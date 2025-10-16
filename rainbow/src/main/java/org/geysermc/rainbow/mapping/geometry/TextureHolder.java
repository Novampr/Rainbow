package org.geysermc.rainbow.mapping.geometry;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.image.NativeImageUtil;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

public record TextureHolder(ResourceLocation location, Optional<Supplier<NativeImage>> supplier, boolean existsInResources) {

    public Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter) {
        if (existsInResources) {
            return RainbowIO.safeIO(() -> {
                try (InputStream texture = assetResolver.openAsset(Rainbow.decorateTextureLocation(location))) {
                    return texture.readAllBytes();
                }
            });
        } else if (supplier.isPresent()) {
            return RainbowIO.safeIO(() -> NativeImageUtil.writeToByteArray(supplier.get().get()));
        }
        reporter.report(() -> "missing texture for " + location + "; please provide it manually");
        return Optional.empty();
    }

    public static TextureHolder createProvided(ResourceLocation location, Supplier<NativeImage> supplier) {
        return new TextureHolder(location, Optional.of(supplier), false);
    }

    public static TextureHolder createFromResources(ResourceLocation location) {
        return new TextureHolder(location, Optional.empty(), true);
    }

    public static TextureHolder createNonExistent(ResourceLocation location) {
        return new TextureHolder(location, Optional.empty(), false);
    }
}
