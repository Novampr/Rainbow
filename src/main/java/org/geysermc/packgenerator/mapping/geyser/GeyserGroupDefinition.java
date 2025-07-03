package org.geysermc.packgenerator.mapping.geyser;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record GeyserGroupDefinition(Optional<ResourceLocation> model, List<GeyserMapping> definitions) implements GeyserMapping {

    public static final MapCodec<GeyserGroupDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("model").forGetter(GeyserGroupDefinition::model),
                    GeyserMapping.CODEC.listOf().fieldOf("definitions").forGetter(GeyserGroupDefinition::definitions)
            ).apply(instance, GeyserGroupDefinition::new)
    );

    @Override
    public Type type() {
        return Type.GROUP;
    }
}
