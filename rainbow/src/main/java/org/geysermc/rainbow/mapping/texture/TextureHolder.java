package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class TextureHolder {
    protected final ResourceLocation location;

    public TextureHolder(ResourceLocation location) {
        this.location = location;
    }

    public abstract Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter);

    public CompletableFuture<?> save(AssetResolver assetResolver, PackSerializer serializer, Path path, ProblemReporter reporter) {
        return load(assetResolver, reporter)
                .map(bytes -> serializer.saveTexture(bytes, path))
                .orElse(CompletableFuture.completedFuture(null));
    }

    public static TextureHolder createCustom(ResourceLocation location, Supplier<NativeImage> supplier) {
        return new CustomTextureHolder(location, supplier);
    }

    public static TextureHolder createBuiltIn(ResourceLocation location, ResourceLocation atlas, ResourceLocation source) {
        return new BuiltInTextureHolder(location, atlas, source);
    }

    public static TextureHolder createBuiltIn(ResourceLocation atlas, ResourceLocation location) {
        return createBuiltIn(location, atlas, location);
    }

    public static TextureHolder createNonExistent(ResourceLocation location) {
        return new MissingTextureHolder(location);
    }

    public static TextureHolder createCopy(TextureHolder original) {
        return new CopyTextureHolder(original.location);
    }

    public ResourceLocation location() {
        return location;
    }

    protected void reportMissing(ProblemReporter reporter) {
        reporter.report(() -> "missing texture for " + location + "; please provide it manually");
    }
}
