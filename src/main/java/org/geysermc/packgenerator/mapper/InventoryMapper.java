package org.geysermc.packgenerator.mapper;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

public class InventoryMapper implements CustomItemProvider {

    public InventoryMapper() {
    }

    @Override
    public Stream<ItemStack> nextItems(LocalPlayer player, ClientPacketListener connection) {
        return Stream.empty();
    }

    @Override
    public boolean isDone() {
        return false;
    }
}
