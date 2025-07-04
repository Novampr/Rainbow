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
import org.geysermc.packgenerator.mapper.InventoryMapper;
import org.geysermc.packgenerator.mapper.ItemSuggestionProvider;
import org.geysermc.packgenerator.mapper.PackMapper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PackGeneratorCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, PackManager packManager, PackMapper packMapper) {
        dispatcher.register(ClientCommandManager.literal("packgenerator")
                .then(ClientCommandManager.literal("create")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    try {
                                        packManager.startPack(name);
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
                            packManager.run(pack -> {
                                ItemStack heldItem = context.getSource().getPlayer().getMainHandItem();
                                Optional<Boolean> problems = pack.map(heldItem);
                                if (problems.isEmpty()) {
                                    context.getSource().sendError(Component.literal("No item found to map!"));
                                } else if (problems.get()) {
                                    context.getSource().sendError(Component.literal("Problems occurred whilst mapping the item!"));
                                }
                                context.getSource().sendFeedback(Component.literal("Added held item to Geyser mappings"));
                            });
                            return 0;
                        })
                )
                .then(ClientCommandManager.literal("mapinventory")
                        .executes(context -> mapInventory(packManager, context.getSource().getPlayer().getInventory(), context.getSource()::sendFeedback, true))
                )
                .then(ClientCommandManager.literal("finish")
                        .executes(context -> {
                            packManager.finish().ifPresent(success -> {
                                if (!success) {
                                    context.getSource().sendError(Component.literal("Errors occurred whilst trying to write the pack to disk!"));
                                } else {
                                    context.getSource().sendFeedback(Component.literal("Wrote pack to disk"));
                                }
                            });
                            return 0;
                        })
                )
                .then(ClientCommandManager.literal("auto")
                        .then(ClientCommandManager.literal("command")
                                .then(ClientCommandManager.argument("suggestions", CommandSuggestionsArgumentType.TYPE)
                                        .executes(context -> {
                                            Pair<String, CompletableFuture<Suggestions>> suggestions = CommandSuggestionsArgumentType.getSuggestions(context, "suggestions");
                                            String baseCommand = suggestions.getFirst();
                                            suggestions.getSecond().thenAccept(completed -> {
                                                ItemSuggestionProvider provider = new ItemSuggestionProvider(completed.getList().stream()
                                                        .map(suggestion -> baseCommand.substring(0, suggestion.getRange().getStart()) + suggestion.getText())
                                                        .toList());
                                                packMapper.setItemProvider(provider);
                                                context.getSource().sendFeedback(Component.literal("Running " + provider.queueSize() + " commands to obtain custom items to map"));
                                            });
                                            return 0;
                                        })
                                )
                        )
                        .then(ClientCommandManager.literal("inventory")
                                .executes(context -> {
                                    packMapper.setItemProvider(InventoryMapper.INSTANCE);
                                    context.getSource().sendFeedback(Component.literal("Now watching inventories for custom items to map"));
                                    return 0;
                                })
                        )
                        .then(ClientCommandManager.literal("stop")
                                .executes(context -> {
                                    packMapper.setItemProvider(null);
                                    context.getSource().sendFeedback(Component.literal("Stopped automatic mapping of custom items"));
                                    return 0;
                                })
                        )
                )
        );
    }

    public static int mapInventory(PackManager manager, Inventory inventory, Consumer<Component> feedback, boolean feedbackOnEmpty) {
        return manager.run(pack -> {
            int mapped = 0;
            boolean errors = false;
            for (ItemStack stack : inventory) {
                Optional<Boolean> problems = pack.map(stack);
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
        }).orElse(0);
    }
}
