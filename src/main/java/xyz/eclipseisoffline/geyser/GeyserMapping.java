package xyz.eclipseisoffline.geyser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record GeyserMapping(ResourceLocation model, ResourceLocation bedrockIdentifier, BedrockOptions bedrockOptions, DataComponentMap components) {
    private static final List<DataComponentType<?>> SUPPORTED_COMPONENTS = List.of(DataComponents.CONSUMABLE, DataComponents.EQUIPPABLE, DataComponents.FOOD,
            DataComponents.MAX_DAMAGE, DataComponents.MAX_STACK_SIZE, DataComponents.USE_COOLDOWN, DataComponents.ENCHANTABLE, DataComponents.ENCHANTMENT_GLINT_OVERRIDE);

    private static final Codec<DataComponentMap> FILTERED_COMPONENT_MAP_CODEC = DataComponentMap.CODEC.xmap(Function.identity(), map -> {
        DataComponentMap.Builder filtered = DataComponentMap.builder();
        map.stream().filter(component -> SUPPORTED_COMPONENTS.contains(component.type()))
                .forEach(component -> setTypedComponent(filtered, component));
        return filtered.build();
    });

    public static final Codec<GeyserMapping> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("type").forGetter(mapping -> "definition"),
                    ResourceLocation.CODEC.fieldOf("model").forGetter(GeyserMapping::model),
                    ResourceLocation.CODEC.fieldOf("bedrock_identifier").forGetter(GeyserMapping::bedrockIdentifier),
                    BedrockOptions.CODEC.fieldOf("bedrock_options").forGetter(GeyserMapping::bedrockOptions),
                    FILTERED_COMPONENT_MAP_CODEC.fieldOf("components").forGetter(GeyserMapping::components)
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

    private static <T> void setTypedComponent(DataComponentMap.Builder builder, TypedDataComponent<T> component) {
        builder.set(component.type(), component.value());
    }
}
