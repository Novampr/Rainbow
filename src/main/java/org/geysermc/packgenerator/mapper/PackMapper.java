package org.geysermc.packgenerator.mapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.geysermc.packgenerator.PackManager;

import java.util.Objects;
import java.util.Optional;

public class PackMapper {
    private final PackManager packManager;
    private CustomItemProvider itemProvider;

    public PackMapper(PackManager manager) {
        this.manager = manager;
    }

    public void setItemProvider(CustomItemProvider itemProvider) {
        this.itemProvider = itemProvider;
    }

    public void tick(Minecraft minecraft) {
        if (itemProvider != null) {
            LocalPlayer player = Objects.requireNonNull(minecraft.player);
            ClientPacketListener connection = Objects.requireNonNull(minecraft.getConnection());

            packManager.runOrElse(pack -> {
                // TODO maybe report problems here... probably better to do so in pack class though
                long mapped = itemProvider.nextItems(player, connection)
                        .map(pack::map)
                        .filter(Optional::isPresent)
                        .count();
                if (mapped != 0) {
                    player.displayClientMessage(Component.literal("Mapped " + mapped + " items"), false);
                }
                if (itemProvider.isDone()) {
                    player.displayClientMessage(Component.literal("Finished mapping items from provider"), false);
                    itemProvider = null;
                }
            }, () -> itemProvider = null);
        }
    }
}
