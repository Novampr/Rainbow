package xyz.eclipseisoffline.geyser;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class GeyserMappingsGenerator implements ClientModInitializer {

    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommandManager.literal("geyser")
                    .then(ClientCommandManager.literal("create")
                            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                    .executes(context -> {
                                        String name = StringArgumentType.getString(context, "name");
                                        PackManager.getInstance().startPack(name);
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
                    .then(ClientCommandManager.literal("finish")
                            .executes(context -> {
                                PackManager.getInstance().finish();
                                context.getSource().sendFeedback(Component.literal("Wrote pack to disk"));
                                return 0;
                            })
                    )
            );
        });
    }
}
