package org.geysermc.packgenerator.mapping.geyser;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.geysermc.packgenerator.CodecUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

// TODO group definitions
public class GeyserMappings {
    private static final Codec<Map<Holder<Item>, Collection<GeyserMapping>>> MAPPINGS_CODEC = Codec.unboundedMap(Item.CODEC, GeyserMapping.CODEC.listOf().xmap(Function.identity(), ArrayList::new));

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

    public void map(Holder<Item> item, GeyserMapping mapping) {
        for (GeyserMapping existing : mappings.get(item)) {
            if (existing.conflictsWith(mapping)) {
                throw new IllegalArgumentException("Mapping conflicts with existing mapping");
            }
        }
        mappings.put(item, mapping);
    }

    public void map(ItemStack stack, ProblemReporter reporter, Consumer<GeyserMapping> mappingConsumer) {
        Optional<? extends ResourceLocation> patchedModel = stack.getComponentsPatch().get(DataComponents.ITEM_MODEL);
        //noinspection OptionalAssignedToNull - annoying Mojang
        if (patchedModel == null || patchedModel.isEmpty()) {
            return;
        }

        ResourceLocation model = patchedModel.get();
        String displayName = stack.getHoverName().getString();
        int protectionValue = 0; // TODO check the attributes

        GeyserItemMapper.mapItem(model, displayName, protectionValue, stack.getComponentsPatch(), reporter)
                .forEach(mapping -> {
                    try {
                        map(stack.getItemHolder(), mapping);
                    } catch (IllegalArgumentException exception) {
                        reporter.forChild(() -> "mapping with bedrock identifier " + mapping.bedrockIdentifier() + " ").report(() -> "failed to add mapping to mappings file: " + exception.getMessage());
                        return;
                    }
                    mappingConsumer.accept(mapping);
                });
    }

    public Map<Holder<Item>, Collection<GeyserMapping>> mappings() {
        return mappings.asMap();
    }
}
