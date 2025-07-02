package org.geysermc.packgenerator.command;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;

import java.util.ArrayList;
import java.util.List;

// TODO safety
public final class ItemSuggestionMapper {
    private static final ItemSuggestionMapper INSTANCE = new ItemSuggestionMapper();

    private List<String> remainingCommands = new ArrayList<>();
    private boolean waitingOnItem = false;
    private boolean waitingOnClear = false;
    private int mapped = 0;

    private ItemSuggestionMapper() {}

    public boolean start(List<String> commands) {
        if (remainingCommands.isEmpty()) {
            remainingCommands.addAll(commands);
            return true;
        }
        return false;
    }

    public void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null) {
            stop();
            return;
        }

        if (!remainingCommands.isEmpty() || waitingOnItem) {
            if (waitingOnClear && minecraft.player.getInventory().isEmpty()) {
                waitingOnClear = false;
            } else if (!waitingOnItem) {
                minecraft.getConnection().send(new ServerboundChatCommandPacket(remainingCommands.removeFirst()));
                waitingOnItem = true;
            } else {
                if (!minecraft.player.getInventory().isEmpty()) {
                    mapped += PackGeneratorCommand.mapInventory(minecraft.player.getInventory(),
                            component -> minecraft.player.displayClientMessage(component, false), false);

                    minecraft.getConnection().send(new ServerboundChatCommandPacket("clear"));

                    waitingOnItem = false;
                    if (remainingCommands.isEmpty()) {
                        minecraft.player.displayClientMessage(Component.literal("Done, " + mapped + " items have been mapped"), false);
                    } else {
                        waitingOnClear = true;
                    }
                }
            }
        }
    }

    public int queueSize() {
        return remainingCommands.size();
    }

    public void stop() {
        remainingCommands.clear();
        waitingOnItem = false;
        waitingOnClear = false;
        mapped = 0;
    }

    public static ItemSuggestionMapper getInstance() {
        return INSTANCE;
    }
}
