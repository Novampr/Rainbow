package org.geysermc.packgenerator.mapper;

import net.minecraft.client.Minecraft;
import org.geysermc.packgenerator.PackManager;

public class PackMappers {
    private final ItemSuggestionMapper suggestionMapper;
    private final InventoryMapper inventoryMapper;

    public PackMappers(PackManager packManager) {
        this.suggestionMapper = new ItemSuggestionMapper(packManager);
        this.inventoryMapper = new InventoryMapper(packManager);
    }

    public ItemSuggestionMapper getSuggestionMapper() {
        return suggestionMapper;
    }

    public InventoryMapper getInventoryMapper() {
        return inventoryMapper;
    }

    public void tick(Minecraft minecraft) {
        suggestionMapper.tick(minecraft);
    }
}
