package org.geysermc.rainbow.mapping.geyser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.geyser.predicate.GeyserPredicate;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// TODO other keys, etc.
// TODO sometimes still includes components key when patch before filtering is not empty but after is
// TODO display name can be a component
public record GeyserBaseDefinition(ResourceLocation bedrockIdentifier, Optional<String> displayName,
                                   List<GeyserPredicate> predicates, BedrockOptions bedrockOptions, DataComponentPatch components) {
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

    public static final MapCodec<GeyserBaseDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("bedrock_identifier").forGetter(GeyserBaseDefinition::bedrockIdentifier),
                    Codec.STRING.optionalFieldOf("display_name").forGetter(GeyserBaseDefinition::displayName),
                    GeyserPredicate.LIST_CODEC.optionalFieldOf("predicate", List.of()).forGetter(GeyserBaseDefinition::predicates),
                    BedrockOptions.CODEC.optionalFieldOf("bedrock_options", BedrockOptions.DEFAULT).forGetter(GeyserBaseDefinition::bedrockOptions),
                    FILTERED_COMPONENT_MAP_CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(GeyserBaseDefinition::components)
            ).apply(instance, GeyserBaseDefinition::new)
    );

    public boolean conflictsWith(GeyserBaseDefinition other) {
        if (predicates.size() == other.predicates.size()) {
            boolean predicatesAreEqual = true;
            for (GeyserPredicate predicate : predicates) {
                if (!other.predicates.contains(predicate)) {
                    predicatesAreEqual = false;
                    break;
                }
            }
            return predicatesAreEqual;
        }
        return false;
    }

    public String textureName() {
        return bedrockOptions.icon.orElse(Rainbow.fileSafeResourceLocation(bedrockIdentifier));
    }

    public record BedrockOptions(Optional<String> icon, boolean allowOffhand, boolean displayHandheld, int protectionValue) {
        public static final Codec<BedrockOptions> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("icon").forGetter(BedrockOptions::icon),
                        Codec.BOOL.optionalFieldOf("allow_offhand", true).forGetter(BedrockOptions::allowOffhand),
                        Codec.BOOL.optionalFieldOf("display_handheld", false).forGetter(BedrockOptions::displayHandheld),
                        Codec.INT.optionalFieldOf("protection_value", 0).forGetter(BedrockOptions::protectionValue)
                ).apply(instance, BedrockOptions::new)
        );
        public static final BedrockOptions DEFAULT = new BedrockOptions(Optional.empty(), true, false, 0);
    }
}
