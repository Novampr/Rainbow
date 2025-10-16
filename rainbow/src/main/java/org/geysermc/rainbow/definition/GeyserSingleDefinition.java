package org.geysermc.rainbow.definition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record GeyserSingleDefinition(GeyserBaseDefinition base, Optional<ResourceLocation> model) implements GeyserItemDefinition {
    public static final MapCodec<GeyserSingleDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    GeyserBaseDefinition.MAP_CODEC.forGetter(GeyserSingleDefinition::base),
                    ResourceLocation.CODEC.optionalFieldOf("model").forGetter(GeyserSingleDefinition::model)
            ).apply(instance, GeyserSingleDefinition::new)
    );

    @Override
    public boolean conflictsWith(Optional<ResourceLocation> parentModel, GeyserItemDefinition other) {
        if (other instanceof GeyserSingleDefinition otherSingle) {
            ResourceLocation thisModel = model.or(() -> parentModel).orElseThrow();
            ResourceLocation otherModel = otherSingle.model.or(() -> parentModel).orElseThrow();
            return thisModel.equals(otherModel) && base.conflictsWith(other.base());
        }
        return false;
    }

    public GeyserSingleDefinition withoutModel() {
        return new GeyserSingleDefinition(base, Optional.empty());
    }

    @Override
    public Type type() {
        return Type.SINGLE;
    }
}
