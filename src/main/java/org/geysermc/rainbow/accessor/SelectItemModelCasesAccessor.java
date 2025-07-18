package org.geysermc.rainbow.accessor;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.renderer.item.ItemModel;

// Implemented on BlockModelWrapper, since this class doesn't store the cases it has in a nice format after baking, we have to store it manually
public interface SelectItemModelCasesAccessor<T> {

    Object2ObjectMap<T, ItemModel> rainbow$getCases();

    void rainbow$setCases(Object2ObjectMap<T, ItemModel> cases);
}
