package org.geysermc.packgenerator;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.IOException;
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
        // TODO do the exceptions properly
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommandManager.literal("packgenerator")
                    .then(ClientCommandManager.literal("create")
                            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                    .executes(context -> {
                                        String name = StringArgumentType.getString(context, "name");
                                        try {
                                            PackManager.getInstance().startPack(name);
                                        } catch (IOException exception) {
                                            throw new SimpleCommandExceptionType(Component.literal(exception.getMessage())).create();
                                        }
                                        context.getSource().sendFeedback(Component.literal("Created pack with name " + name));
                                        return 0;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("map")
                            .executes(context -> {
                                ItemStack heldItem = context.getSource().getPlayer().getMainHandItem();
                                PackManager.getInstance().map(heldItem);
                                context.getSource().sendFeedback(Component.literal("Added held item to Geyser mappings"));
                                return 0;
                            })
                    )
                    .then(ClientCommandManager.literal("mapinventory")
                            .executes(context -> {
                                int mapped = 0;
                                for (ItemStack stack : context.getSource().getPlayer().getInventory()) {
                                    if (PackManager.getInstance().map(stack)) {
                                        mapped++;
                                    }
                                }
                                context.getSource().sendFeedback(Component.literal("Mapped " + mapped + " items from your inventory"));
                                return 0;
                            })
                    )
                    .then(ClientCommandManager.literal("finish")
                            .executes(context -> {
                                if (!PackManager.getInstance().finish()) {
                                    throw new SimpleCommandExceptionType(Component.literal("Errors occurred whilst trying to write the pack to disk!")).create();
                                }
                                context.getSource().sendFeedback(Component.literal("Wrote pack to disk"));
                                return 0;
                            })
                    )
                    .then(ClientCommandManager.literal("auto")
                            .then(ClientCommandManager.argument("namespace", StringArgumentType.word())
                                    .then(ClientCommandManager.argument("path", StringArgumentType.string())
                                            .executes(context -> {
                                                String namespace = StringArgumentType.getString(context, "namespace");
                                                String path = StringArgumentType.getString(context, "path");

                                                // Duplicated code, this is just to try this out
                                                try {
                                                    PackManager.getInstance().startPack(namespace);
                                                } catch (IOException exception) {
                                                    throw new SimpleCommandExceptionType(Component.literal(exception.getMessage())).create();
                                                }
                                                context.getSource().sendFeedback(Component.literal("Created pack with name " + namespace));

                                                // hack
                                                CommandContext<?> suggestionsContext = new CommandContext<>(null,
                                                        "loot give @s loot " + namespace + ":" + path,
                                                        null, null, null, null, null, null, null, false);
                                                context.getSource().getClient().getConnection().getSuggestionsProvider()
                                                        .customSuggestion(suggestionsContext)
                                                        .whenComplete((suggestions, throwable) -> pendingPackCommands.addAll(suggestions.getList().stream().map(Suggestion::getText).toList()));
                                                return 0;
                                            })
                                    )
                            )
                    )
            );
        });

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
