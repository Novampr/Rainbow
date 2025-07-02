package org.geysermc.packgenerator;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class GeyserMappingsGenerator implements ClientModInitializer {

    public static final String MOD_ID = "geyser-mappings-generator";
    public static final String MOD_NAME = "Geyser Mappings Generator";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final List<String> pendingPackCommands = new ArrayList<>();
    private boolean waitingOnItem = false;
    private boolean waitingOnClear = false;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> PackGeneratorCommand.register(dispatcher));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!pendingPackCommands.isEmpty() || waitingOnItem || waitingOnClear) {
                if (waitingOnClear && client.player.getInventory().isEmpty()) {
                    waitingOnClear = false;
                } else if (!waitingOnItem) {
                    String command = pendingPackCommands.removeFirst();
                    client.getConnection().send(new ServerboundChatCommandPacket("loot give @s loot " + command));
                    waitingOnItem = true;
                } else {
                    if (!client.player.getInventory().isEmpty()) {
                        int mapped = 0;
                        for (ItemStack stack : client.player.getInventory()) {
                            try {
                                if (PackManager.getInstance().map(stack)) {
                                    mapped++;
                                }
                            } catch (Exception exception) {
                                client.player.displayClientMessage(Component.literal("Failed to map item " + exception.getMessage()), false);
                            }
                        }
                        client.player.displayClientMessage(Component.literal("Mapped " + mapped + " items from your inventory"), false);

                        client.getConnection().send(new ServerboundChatCommandPacket("clear"));

                        waitingOnItem = false;
                        if (pendingPackCommands.isEmpty()) {
                            try {
                                if (!PackManager.getInstance().finish()) {
                                    client.player.displayClientMessage(Component.literal("Errors occurred whilst trying to write the pack to disk!"), false);
                                    return;
                                }
                            } catch (CommandSyntaxException e) {
                                throw new RuntimeException(e);
                            }
                            client.player.displayClientMessage(Component.literal("Wrote pack to disk"), false);
                        } else {
                            waitingOnClear = true;
                        }
                    }
                }
            }
        });
    }
}
