package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mixin.SpriteContentsAccessor;
import org.geysermc.rainbow.mixin.SpriteLoaderAccessor;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record StitchedTextures(Map<String, TextureAtlasSprite> sprites, Supplier<NativeImage> stitched, int width, int height) {

    public Optional<TextureAtlasSprite> getSprite(String key) {
        if (TextureSlotsAccessor.invokeIsTextureReference(key)) {
            key = key.substring(1);
        }
        return Optional.ofNullable(sprites.get(key));
    }

    public static StitchedTextures stitchModelTextures(TextureSlots textures, PackContext context) {
        Map<String, Material> materials = ((TextureSlotsAccessor) textures).getResolvedValues();
        SpriteLoader.Preparations preparations = prepareStitching(materials.values().stream().map(Material::texture), context);

        Map<String, TextureAtlasSprite> sprites = new HashMap<>();
        for (Map.Entry<String, Material> material : materials.entrySet()) {
            sprites.put(material.getKey(), preparations.getSprite(material.getValue().texture()));
        }
        return new StitchedTextures(Map.copyOf(sprites), () -> stitchTextureAtlas(preparations), preparations.width(), preparations.height());
    }

    private static SpriteLoader.Preparations prepareStitching(Stream<ResourceLocation> textures, PackContext context) {
        // Atlas ID doesn't matter much here, but BLOCKS is the most appropriate
        // Not sure if 1024 should be the max supported texture size, but it seems to work
        SpriteLoader spriteLoader = new SpriteLoader(AtlasIds.BLOCKS, 1024, 16, 16);
        List<SpriteContents> sprites = textures.distinct()
                .map(texture -> readSpriteContents(texture, context))
                .<SpriteContents>mapMulti(Optional::ifPresent)
                .toList();
        return  ((SpriteLoaderAccessor) spriteLoader).invokeStitch(sprites, 0, Util.backgroundExecutor());
    }

    private static Optional<SpriteContents> readSpriteContents(ResourceLocation location, PackContext context) {
        return RainbowIO.safeIO(() -> {
            try (TextureResource texture = context.assetResolver().getBlockTexture(location).orElse(null)) {
                if (texture != null) {
                    return new SpriteContents(location, texture.sizeOfFrame(), texture.getFirstFrame(true));
                }
            }
            return null;
        });
    }

    private static NativeImage stitchTextureAtlas(SpriteLoader.Preparations preparations) {
        NativeImage stitched = new NativeImage(preparations.width(), preparations.height(), true);
        for (TextureAtlasSprite sprite : preparations.regions().values()) {
            try (SpriteContents contents = sprite.contents()) {
                ((SpriteContentsAccessor) contents).getOriginalImage().copyRect(stitched, 0, 0,
                        sprite.getX(), sprite.getY(), contents.width(), contents.height(), false, false);
            }
        }
        return stitched;
    }
}
