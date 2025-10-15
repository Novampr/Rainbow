package org.geysermc.rainbow.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.client.command.CommandSuggestionsArgumentType;
import org.geysermc.rainbow.client.command.PackGeneratorCommand;
import org.geysermc.rainbow.client.mapper.PackMapper;
import org.geysermc.rainbow.mixin.SpriteContentsAccessor;
import org.geysermc.rainbow.mixin.SpriteLoaderAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class RainbowClient implements ClientModInitializer {

    private final PackManager packManager = new PackManager();
    private final PackMapper packMapper = new PackMapper(packManager);

    // TODO export language overrides
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            PackGeneratorCommand.register(dispatcher, packManager, packMapper);

            dispatcher.register(
                    ClientCommandManager.literal("DEBUGTEST")
                            .then(ClientCommandManager.argument("textures", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        SpriteLoader spriteLoader = new SpriteLoader(AtlasIds.BLOCKS, 1024, 16, 16);
                                        List<SpriteContents> sprites = Arrays.stream(StringArgumentType.getString(context, "textures").split(" "))
                                                .map(ResourceLocation::tryParse)
                                                .map(RainbowClient::readSpriteContents)
                                                .toList();
                                        SpriteLoader.Preparations preparations = ((SpriteLoaderAccessor) spriteLoader).invokeStitch(sprites, 0, Util.backgroundExecutor());

                                        try (NativeImage stitched = stitchTextureAtlas(preparations)) {
                                            stitched.writeToFile(FabricLoader.getInstance().getGameDir().resolve("test.png"));
                                        } catch (IOException exception) {
                                            throw new RuntimeException(exception);
                                        }
                                        return 0;
                                    })
                            )
            );
        });
        ClientTickEvents.START_CLIENT_TICK.register(packMapper::tick);

        ArgumentTypeRegistry.registerArgumentType(Rainbow.getModdedLocation("command_suggestions"),
                CommandSuggestionsArgumentType.class, SingletonArgumentInfo.contextFree(CommandSuggestionsArgumentType::new));
    }

    private static SpriteContents readSpriteContents(ResourceLocation location) {
        try (InputStream textureStream = Minecraft.getInstance().getResourceManager().open(location.withPath(path -> "textures/" + path + ".png"))) {
            NativeImage texture = NativeImage.read(textureStream);
            return new SpriteContents(location, new FrameSize(texture.getWidth(), texture.getHeight()), texture);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static NativeImage stitchTextureAtlas(SpriteLoader.Preparations preparations) {
        NativeImage stitched = new NativeImage(preparations.width(), preparations.height(), false);
        for (TextureAtlasSprite sprite : preparations.regions().values()) {
            try (SpriteContents contents = sprite.contents()) {
                ((SpriteContentsAccessor) contents).getOriginalImage().copyRect(stitched, 0, 0,
                        sprite.getX(), sprite.getY(), contents.width(), contents.height(), false, false);
            }
        }
        return stitched;
    }
}
