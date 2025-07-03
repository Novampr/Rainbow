package org.geysermc.packgenerator.mapping.geyser;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record GroupDefinition(Optional<ResourceLocation> model, List<GeyserMappingNew> definitions) implements GeyserMappingNew {

    public static final MapCodec<GroupDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("model").forGetter(GroupDefinition::model),
                    GeyserMappingNew.CODEC.listOf().fieldOf("definitions").forGetter(GroupDefinition::definitions)
            ).apply(instance, GroupDefinition::new)
    );

    @Override
    public Type type() {
        return Type.GROUP;
    }
}
