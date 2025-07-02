package org.geysermc.packgenerator.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.geysermc.packgenerator.PackManager;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
                        .executes(context -> mapInventory(context.getSource().getPlayer().getInventory(), context.getSource()::sendFeedback, true))
                )
                .then(ClientCommandManager.literal("finish")
                        .executes(context -> {
                            if (!PackManager.getInstance().finish()) {
                                context.getSource().sendError(Component.literal("Errors occurred whilst trying to write the pack to disk!"));
                            }
                            context.getSource().sendFeedback(Component.literal("Wrote pack to disk"));
                            return 0;
                        })
                )
                .then(ClientCommandManager.literal("auto")
                        .then(ClientCommandManager.argument("suggestions", CommandSuggestionsArgumentType.TYPE)
                                .executes(context -> {
                                    PackManager.getInstance().ensurePackIsCreated();
                                    Pair<String, CompletableFuture<Suggestions>> suggestions = CommandSuggestionsArgumentType.getSuggestions(context, "suggestions");
                                    String baseCommand = suggestions.getFirst();
                                    suggestions.getSecond().thenAccept(completed -> {
                                        ItemSuggestionMapper.getInstance().start(completed.getList().stream()
                                                .map(suggestion -> baseCommand.substring(0, suggestion.getRange().getStart()) + suggestion.getText())
                                                .toList());
                                        context.getSource().sendFeedback(Component.literal("Running " + ItemSuggestionMapper.getInstance().queueSize() + " commands to obtain custom items to map"));
                                    });
                                    return 0;
                                })
                        )
                )
        );
    }

    public static int mapInventory(Inventory inventory, Consumer<Component> feedback, boolean feedbackOnEmpty) {
        int mapped = 0;
        boolean errors = false;
        for (ItemStack stack : inventory) {
            Optional<Boolean> problems = PackManager.getInstance().map(stack);
            if (problems.isPresent()) {
                mapped++;
                errors |= problems.get();
            }
        }
        if (mapped > 0) {
            if (errors) {
                feedback.accept(Component.literal("Problems occurred whilst mapping items!").withStyle(ChatFormatting.RED));
            }
            feedback.accept(Component.literal("Mapped " + mapped + " items from your inventory"));
        } else if (feedbackOnEmpty) {
            feedback.accept(Component.literal("No items were mapped"));
        }
        return mapped;
    }
}
