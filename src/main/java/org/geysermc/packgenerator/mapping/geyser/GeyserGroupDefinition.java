package org.geysermc.packgenerator.mapping.geyser;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record GeyserGroupDefinition(Optional<ResourceLocation> model, List<GeyserMapping> definitions) implements GeyserMapping {

    public static final MapCodec<GeyserGroupDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("model").forGetter(GeyserGroupDefinition::model),
                    GeyserMapping.CODEC.listOf().fieldOf("definitions").forGetter(GeyserGroupDefinition::definitions)
            ).apply(instance, GeyserGroupDefinition::new)
    );

    public GeyserGroupDefinition with(GeyserMapping mapping) {
        return new GeyserGroupDefinition(model, Stream.concat(definitions.stream(), Stream.of(mapping)).toList());
    }

    public boolean isFor(ResourceLocation model) {
        return this.model.isPresent() && this.model.get().equals(model);
    }

    public boolean conflictsWith(Optional<ResourceLocation> parentModel, GeyserSingleDefinition other) {
        Optional<ResourceLocation> thisModel = model.or(() -> parentModel);
        for (GeyserMapping definition : definitions) {
            if (definition instanceof GeyserGroupDefinition group && group.conflictsWith(thisModel, other)) {
                return true;
            } else if (definition instanceof GeyserSingleDefinition single && single.conflictsWith(thisModel, other)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        int totalSize = 0;
        for (GeyserMapping definition : definitions) {
            if (definition instanceof GeyserGroupDefinition group) {
                totalSize += group.size();
            } else {
                totalSize++;
            }
        }
        return totalSize;
    }

    @Override
    public Type type() {
        return Type.GROUP;
    }
}
