package org.geysermc.rainbow.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.PackManager;
import org.geysermc.rainbow.mapper.InventoryMapper;
import org.geysermc.rainbow.mapper.PackMapper;
import org.geysermc.rainbow.pack.BedrockPack;

import java.util.function.BiConsumer;

public class PackGeneratorCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, PackManager packManager, PackMapper packMapper) {
        dispatcher.register(ClientCommandManager.literal("rainbow")
                .then(ClientCommandManager.literal("create")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    try {
                                        packManager.startPack(name);
                                    } catch (Exception exception) {
                                        context.getSource().sendError(Component.literal("Failed to create new pack!"));
                                        throw new RuntimeException(exception);
                                    }
                                    context.getSource().sendFeedback(Component.literal("Created pack with name " + name));
                                    return 0;
                                })
                        )
                )
                .then(ClientCommandManager.literal("map")
                        .executes(runWithPack(packManager, (source, pack) -> {
                            ItemStack heldItem = source.getPlayer().getMainHandItem();
                            switch (pack.map(heldItem)) {
                                case NONE_MAPPED -> source.sendError(Component.literal("No item was mapped. Either no custom item was found, or it was already included in the pack"));
                                case PROBLEMS_OCCURRED -> source.sendFeedback(Component.literal("The held item was mapped, however problems occurred whilst doing so. Read the pack report after finishing the pack for more information"));
                                case MAPPED_SUCCESSFULLY -> source.sendFeedback(Component.literal("The held item was mapped"));
                            }
                        }))
                )
                .then(ClientCommandManager.literal("mapinventory")
                        .executes(runWithPack(packManager, (source, pack) -> {
                            int mapped = 0;
                            boolean errors = false;
                            Inventory inventory = source.getPlayer().getInventory();

                            for (ItemStack stack : inventory) {
                                BedrockPack.MappingResult result = pack.map(stack);
                                if (result != BedrockPack.MappingResult.NONE_MAPPED) {
                                    mapped++;
                                    if (result == BedrockPack.MappingResult.PROBLEMS_OCCURRED) {
                                        errors = true;
                                    }
                                }
                            }

                            if (mapped > 0) {
                                source.sendFeedback(Component.literal("Mapped " + mapped + " items from your inventory"));
                                if (errors) {
                                    source.sendFeedback(Component.literal("Problems occurred whilst mapping items. Read the pack report after finishing the pack for more information"));
                                }
                            } else {
                                source.sendError(Component.literal("No items were mapped. Either no custom items were found, or they were already included in the pack"));
                            }
                        }))
                )
                .then(ClientCommandManager.literal("auto")
                        /* This is disabled for now.
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
                         */
                        .then(ClientCommandManager.literal("inventory")
                                .executes(runWithPack(packManager, (source, pack) -> {
                                    packMapper.setItemProvider(InventoryMapper.INSTANCE);
                                    source.sendFeedback(Component.literal("Now watching inventories for custom items to map"));
                                }))
                        )
                        .then(ClientCommandManager.literal("stop")
                                .executes(runWithPack(packManager, (source, pack) -> {
                                    packMapper.setItemProvider(null);
                                    source.sendFeedback(Component.literal("Stopped automatic mapping of custom items"));
                                }))
                        )
                )
                .then(ClientCommandManager.literal("finish")
                        .executes(context -> {
                            packManager.finish().ifPresentOrElse(success -> {
                                if (!success) {
                                    context.getSource().sendError(Component.literal("Errors occurred whilst writing the pack to disk!"));
                                } else {
                                    context.getSource().sendFeedback(Component.literal("Wrote pack to disk"));
                                }
                            }, () -> context.getSource().sendError(Component.literal("Create a pack first!")));
                            return 0;
                        })
                )
        );
    }

    private static Command<FabricClientCommandSource> runWithPack(PackManager manager, BiConsumer<FabricClientCommandSource, BedrockPack> executor) {
        return context -> {
            manager.runOrElse(pack -> executor.accept(context.getSource(), pack),
                    () -> context.getSource().sendError(Component.literal("Create a pack first!")));
            return 0;
        };
    }
}
