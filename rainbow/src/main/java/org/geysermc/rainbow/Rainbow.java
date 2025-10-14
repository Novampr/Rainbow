package org.geysermc.rainbow;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.pack.bedrock.resource.BedrockResourcePack;
import org.geysermc.pack.converter.PackConversionContext;
import org.geysermc.pack.converter.PackConverter;
import org.geysermc.pack.converter.converter.ActionListener;
import org.geysermc.pack.converter.converter.Converter;
import org.geysermc.pack.converter.converter.base.PackManifestConverter;
import org.geysermc.pack.converter.converter.lang.LangConverter;
import org.geysermc.pack.converter.converter.misc.SplashTextConverter;
import org.geysermc.pack.converter.converter.model.ModelConverter;
import org.geysermc.pack.converter.data.ConversionData;
import org.geysermc.pack.converter.util.DefaultLogListener;
import org.geysermc.pack.converter.util.LogListener;
import org.geysermc.rainbow.command.CommandSuggestionsArgumentType;
import org.geysermc.rainbow.command.PackGeneratorCommand;
import org.geysermc.rainbow.creative.MinecraftCreativeResourcePack;
import org.geysermc.rainbow.mapper.PackMapper;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Rainbow implements ClientModInitializer {

    public static final String MOD_ID = "rainbow";
    public static final String MOD_NAME = "Rainbow";
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

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommandManager.literal("debugtest")
                    .executes(context -> {

                        PackConverter packConverter = new PackConverter()
                                .packName("RAINBOW-TEST")
                                .output(FabricLoader.getInstance().getGameDir().resolve("pack-convert-out"));
                        Path tmpDir = FabricLoader.getInstance().getGameDir().resolve("pack-convert-temp");

                        ImageIO.scanForPlugins();
                        MinecraftCreativeResourcePack resourcePack = new MinecraftCreativeResourcePack(Minecraft.getInstance().getResourceManager());
                        BedrockResourcePack bedrockResourcePack = new BedrockResourcePack(tmpDir);

                        LogListener logListener = new DefaultLogListener();
                        final Converter.ConversionDataCreationContext conversionDataCreationContext = new Converter.ConversionDataCreationContext(
                                packConverter, logListener, null, tmpDir, resourcePack, resourcePack
                        );


                        List<Converter<?>> converters = new ArrayList<>();
                        converters.add(new PackManifestConverter());
                        converters.add(new LangConverter());
                        converters.add(new ModelConverter());

                        int errors = 0;
                        for (Converter converter : converters) {
                            ConversionData data = converter.createConversionData(conversionDataCreationContext);
                            PackConversionContext<?> conversionContext = new PackConversionContext<>(data, packConverter, resourcePack, bedrockResourcePack, logListener);

                            List<ActionListener<?>> actionListeners = List.of();
                            try {
                                actionListeners.forEach(actionListener -> actionListener.preConvert((PackConversionContext) conversionContext));
                                converter.convert(conversionContext);
                                actionListeners.forEach(actionListener -> actionListener.postConvert((PackConversionContext) conversionContext));
                            } catch (Throwable t) {
                                logListener.error("Error converting pack!", t);
                                errors++;
                            }
                        }

                        try {
                            bedrockResourcePack.export();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        context.getSource().sendFeedback(Component.literal("exporting, " + errors + " errors"));

                        return 0;
                    })
            );
        });
    }

    public static ResourceLocation getModdedLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String fileSafeResourceLocation(ResourceLocation location) {
        return location.toString().replace(':', '.').replace('/', '_');
    }
}
