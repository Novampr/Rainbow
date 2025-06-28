package xyz.eclipseisoffline.geyser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record GeyserMapping(ResourceLocation model, ResourceLocation bedrockIdentifier, BedrockOptions bedrockOptions, DataComponentMap components) {
    public static final Codec<GeyserMapping> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("type").forGetter(mapping -> "definition"),
                    ResourceLocation.CODEC.fieldOf("model").forGetter(GeyserMapping::model),
                    ResourceLocation.CODEC.fieldOf("bedrock_identifier").forGetter(GeyserMapping::bedrockIdentifier),
                    BedrockOptions.CODEC.fieldOf("bedrock_options").forGetter(GeyserMapping::bedrockOptions),
                    DataComponentMap.CODEC.fieldOf("components").forGetter(GeyserMapping::components)
            ).apply(instance, (type, model, bedrockIdentifier, bedrockOptions, components)
                    -> new GeyserMapping(model, bedrockIdentifier, bedrockOptions, components))
    );

    public record BedrockOptions(Optional<String> icon, boolean allowOffhand, boolean displayHandheld, int protectionValue) {
        public static final Codec<BedrockOptions> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("icon").forGetter(BedrockOptions::icon),
                        Codec.BOOL.fieldOf("allow_offhand").forGetter(BedrockOptions::allowOffhand),
                        Codec.BOOL.fieldOf("display_handheld").forGetter(BedrockOptions::displayHandheld),
                        Codec.INT.fieldOf("protection_value").forGetter(BedrockOptions::protectionValue)
                ).apply(instance, BedrockOptions::new)
        );
    }
}
