package org.geysermc.packgenerator;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.Optional;

public class PackGeneratorCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("packgenerator")
                .then(ClientCommandManager.literal("create")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    try {
                                        PackManager.getInstance().startPack(name);
                                    } catch (Exception exception) {
                                        context.getSource().sendError(Component.literal("Failed to create new pack: " + exception.getMessage()));
                                        return -1;
                                    }
                                    context.getSource().sendFeedback(Component.literal("Created pack with name " + name));
                                    return 0;
                                })
                        )
                )
                .then(ClientCommandManager.literal("map")
                        .executes(context -> {
                            ItemStack heldItem = context.getSource().getPlayer().getMainHandItem();
                            Optional<Boolean> problems = PackManager.getInstance().map(heldItem);
                            if (problems.isEmpty()) {
                                context.getSource().sendError(Component.literal("No item found to map!"));
                            } else if (problems.get()) {
                                context.getSource().sendError(Component.literal("Problems occurred whilst mapping the item!"));
                            }
                            context.getSource().sendFeedback(Component.literal("Added held item to Geyser mappings"));
                            return 0;
                        })
                )
                .then(ClientCommandManager.literal("mapinventory")
                        .executes(context -> {
                            int mapped = 0;
                            boolean errors = false;
                            for (ItemStack stack : context.getSource().getPlayer().getInventory()) {
                                Optional<Boolean> problems = PackManager.getInstance().map(stack);
                                if (problems.isPresent()) {
                                    mapped++;
                                    errors |= problems.get();
                                }
                            }
                            if (errors) {
                                context.getSource().sendError(Component.literal("Problems occurred whilst mapping items!"));
                            }
                            context.getSource().sendFeedback(Component.literal("Mapped " + mapped + " items from your inventory"));
                            return 0;
                        })
                )
                .then(ClientCommandManager.literal("finish")
                        .executes(context -> {
                            if (!PackManager.getInstance().finish()) {
                                context.getSource().sendError(Component.literal("Errors occurred whilst trying to write the pack to disk!"));
                                return -1;
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
    }
}
