package org.geysermc.packgenerator;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.packgenerator.command.CommandSuggestionsArgumentType;
import org.geysermc.packgenerator.command.PackGeneratorCommand;
import org.geysermc.packgenerator.mapper.PackMapper;
import org.slf4j.Logger;

public class GeyserMappingsGenerator implements ClientModInitializer {

    public static final String MOD_ID = "geyser-mappings-generator";
    public static final String MOD_NAME = "Geyser Mappings Generator";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final PackManager packManager = new PackManager();
    private final PackMapper packMapper = new PackMapper(packManager);

    // TODO export language overrides
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> PackGeneratorCommand.register(dispatcher, packManager, packMapper));
        ClientTickEvents.START_CLIENT_TICK.register(packMapper::tick);

        ArgumentTypeRegistry.registerArgumentType(getModdedLocation("command_suggestions"),
                CommandSuggestionsArgumentType.class, SingletonArgumentInfo.contextFree(CommandSuggestionsArgumentType::new));
    }

    public static ResourceLocation getModdedLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
