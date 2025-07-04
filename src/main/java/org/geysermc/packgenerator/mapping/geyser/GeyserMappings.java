package org.geysermc.packgenerator.mapping.geyser;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.geysermc.packgenerator.CodecUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class GeyserMappings {
    private static final Codec<Map<Holder<Item>, Collection<GeyserMapping>>> MAPPINGS_CODEC = Codec.unboundedMap(Item.CODEC, GeyserMapping.MODEL_SAFE_CODEC.listOf().xmap(Function.identity(), ArrayList::new));

    public static final Codec<GeyserMappings> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(Codec.INT, "format_version", 2),
                    MAPPINGS_CODEC.fieldOf("items").forGetter(GeyserMappings::mappings)
            ).apply(instance, (format, mappings) -> new GeyserMappings(mappings))
    );

    private final Multimap<Holder<Item>, GeyserMapping> mappings = MultimapBuilder.hashKeys().hashSetValues().build();

    public GeyserMappings() {}

    private GeyserMappings(Map<Holder<Item>, Collection<GeyserMapping>> mappings) {
        for (Holder<Item> item : mappings.keySet()) {
            this.mappings.putAll(item, mappings.get(item));
        }
    }

    public void map(Holder<Item> item, GeyserSingleDefinition mapping) {
        ResourceLocation model = mapping.model().orElseThrow();
        Optional<GeyserGroupDefinition> modelGroup = Optional.empty();

        Collection<GeyserMapping> existingMappings = new ArrayList<>(mappings.get(item));
        for (GeyserMapping existing : existingMappings) {
            if (existing instanceof GeyserGroupDefinition existingGroup && existingGroup.isFor(model)) {
                if (existingGroup.conflictsWith(Optional.empty(), mapping)) {
                    throw new IllegalArgumentException("Mapping conflicts with existing group mapping");
                }
                modelGroup = Optional.of(existingGroup);
                break;
            } else if (existing instanceof GeyserSingleDefinition single) {
                if (single.conflictsWith(Optional.empty(), mapping)) {
                    throw new IllegalArgumentException("Mapping conflicts with existing single mapping");
                } else if (model.equals(single.model().orElseThrow())) {
                    mappings.remove(item, single);
                    modelGroup = Optional.of(new GeyserGroupDefinition(Optional.of(model), List.of(single.withoutModel())));
                }
            }
        }

        if (modelGroup.isPresent()) {
            mappings.remove(item, modelGroup.get());
            mappings.put(item, modelGroup.get().with(mapping.withoutModel()));
        } else {
            mappings.put(item, mapping);
        }
    }

    public int size() {
        int totalSize = 0;
        for (GeyserMapping mapping : mappings.values()) {
            if (mapping instanceof GeyserGroupDefinition group) {
                totalSize += group.size();
            } else {
                totalSize++;
            }
        }
        return totalSize;
    }

    public Map<Holder<Item>, Collection<GeyserMapping>> mappings() {
        return mappings.asMap();
    }
}
