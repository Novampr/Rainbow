package org.geysermc.packgenerator.accessor;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.renderer.item.ItemModel;

// Implemented on BlockModelWrapper, since this class doesn't store the cases it has in a nice format, we have to store it manually
public interface SelectItemModelCasesAccessor<T> {

    Object2ObjectMap<T, ItemModel> geyser_mappings_generator$getCases();

    void geyser_mappings_generator$setCases(Object2ObjectMap<T, ItemModel> cases);
}
