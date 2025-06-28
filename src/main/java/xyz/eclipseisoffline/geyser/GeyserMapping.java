package xyz.eclipseisoffline.geyser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// TODO predicates, etc.
public record GeyserMapping(ResourceLocation model, ResourceLocation bedrockIdentifier, Optional<String> displayName,
                            BedrockOptions bedrockOptions, DataComponentPatch components) {
    private static final List<DataComponentType<?>> SUPPORTED_COMPONENTS = List.of(DataComponents.CONSUMABLE, DataComponents.EQUIPPABLE, DataComponents.FOOD,
            DataComponents.MAX_DAMAGE, DataComponents.MAX_STACK_SIZE, DataComponents.USE_COOLDOWN, DataComponents.ENCHANTABLE, DataComponents.ENCHANTMENT_GLINT_OVERRIDE);

    private static final Codec<DataComponentPatch> FILTERED_COMPONENT_MAP_CODEC = DataComponentPatch.CODEC.xmap(Function.identity(), patch -> {
        DataComponentPatch.Builder filtered = DataComponentPatch.builder();
        patch.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty() || SUPPORTED_COMPONENTS.contains(entry.getKey()))
                .forEach(entry -> {
                    if (entry.getValue().isPresent()) {
                        filtered.set((DataComponentType) entry.getKey(), entry.getValue().orElseThrow());
                    } else {
                        filtered.remove(entry.getKey());
                    }
                });
        return filtered.build();
    });

    public static final Codec<GeyserMapping> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("type").forGetter(mapping -> "definition"),
                    ResourceLocation.CODEC.fieldOf("model").forGetter(GeyserMapping::model),
                    ResourceLocation.CODEC.fieldOf("bedrock_identifier").forGetter(GeyserMapping::bedrockIdentifier),
                    Codec.STRING.optionalFieldOf("display_name").forGetter(GeyserMapping::displayName),
                    BedrockOptions.CODEC.fieldOf("bedrock_options").forGetter(GeyserMapping::bedrockOptions),
                    FILTERED_COMPONENT_MAP_CODEC.fieldOf("components").forGetter(GeyserMapping::components)
            ).apply(instance, (type, model, bedrockIdentifier, displayName, bedrockOptions, components)
                    -> new GeyserMapping(model, bedrockIdentifier, displayName, bedrockOptions, components))
    );

    public record BedrockOptions(Optional<String> icon, boolean allowOffhand, boolean displayHandheld, int protectionValue) {
        public static final Codec<BedrockOptions> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("icon").forGetter(BedrockOptions::icon),
                        Codec.BOOL.optionalFieldOf("allow_offhand", true).forGetter(BedrockOptions::allowOffhand),
                        Codec.BOOL.optionalFieldOf("display_handheld", false).forGetter(BedrockOptions::displayHandheld),
                        Codec.INT.optionalFieldOf("protection_value", 0).forGetter(BedrockOptions::protectionValue)
                ).apply(instance, BedrockOptions::new)
        );
    }
}
