package org.geysermc.packgenerator.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BedrockTextureAtlas(String resourcePackName, String atlasName, BedrockTextures textures) {
    public static final String ITEM_ATLAS = "atlas.items";
    public static final Codec<BedrockTextureAtlas> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("resource_pack_name").forGetter(BedrockTextureAtlas::resourcePackName),
                    Codec.STRING.fieldOf("texture_name").forGetter(BedrockTextureAtlas::atlasName),
                    BedrockTextures.CODEC.fieldOf("texture_data").forGetter(BedrockTextureAtlas::textures)
            ).apply(instance, BedrockTextureAtlas::new)
    );
    public static final Codec<BedrockTextureAtlas> ITEM_ATLAS_CODEC = CODEC.validate(atlas -> {
        if (!ITEM_ATLAS.equals(atlas.atlasName)) {
            return DataResult.error(() -> "Expected atlas to be " + ITEM_ATLAS + ", got " + atlas.atlasName);
        }
        return DataResult.success(atlas);
    });

    public static BedrockTextureAtlas itemAtlas(String resourcePackName, BedrockTextures.Builder textures) {
        return new BedrockTextureAtlas(resourcePackName, ITEM_ATLAS, textures.build());
    }
}
