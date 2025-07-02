package org.geysermc.packgenerator;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.packgenerator.command.CommandSuggestionsArgumentType;
import org.geysermc.packgenerator.command.ItemSuggestionMapper;
import org.geysermc.packgenerator.command.PackGeneratorCommand;
import org.slf4j.Logger;

public class GeyserMappingsGenerator implements ClientModInitializer {

    public static final String MOD_ID = "geyser-mappings-generator";
    public static final String MOD_NAME = "Geyser Mappings Generator";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> PackGeneratorCommand.register(dispatcher));
        ClientTickEvents.START_CLIENT_TICK.register(minecraft -> ItemSuggestionMapper.getInstance().tick(minecraft));

        ArgumentTypeRegistry.registerArgumentType(getModdedLocation("command_suggestions"),
                CommandSuggestionsArgumentType.class, SingletonArgumentInfo.contextFree(CommandSuggestionsArgumentType::new));
    }

    public ResourceLocation getModdedLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
