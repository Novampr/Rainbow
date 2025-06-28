package org.geysermc.packgenerator.mappings;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class GeyserMappings {
    private static final Codec<Map<Holder<Item>, Collection<GeyserMapping>>> MAPPINGS_CODEC = Codec.unboundedMap(Item.CODEC, GeyserMapping.CODEC.listOf().xmap(Function.identity(), ArrayList::new));

    public static final Codec<GeyserMappings> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("format_version").forGetter(mappings -> 2),
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

    public void map(Holder<Item> item, GeyserMapping mapping) {
        // TODO conflict detection
        mappings.put(item, mapping);
    }

    public Map<Holder<Item>, Collection<GeyserMapping>> mappings() {
        return mappings.asMap();
    }
}
