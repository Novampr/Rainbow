package org.geysermc.rainbow.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.client.mixin.SplashRendererAccessor;
import org.geysermc.rainbow.client.render.MinecraftGeometryRenderer;
import org.geysermc.rainbow.pack.BedrockItem;
import org.geysermc.rainbow.pack.BedrockPack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class PackManager {
    private static final List<String> PACK_SUMMARY_COMMENTS = List.of("Use the custom item API v2 build!", "bugrock moment", "RORY",
            "use !!plshelp", "rm -rf --no-preserve-root /*", "welcome to the internet!", "beep beep. boop boop?", "FROG", "it is frog day", "it is cat day!",
            "eclipse will hear about this.", "you must now say the word 'frog' in the #general channel", "You Just Lost The Game", "you are now breathing manually",
            "you are now blinking manually", "you're eligible for a free hug token! <3", "don't mind me!", "hissss", "Gayser and Floodgayte, my favourite plugins.",
            "meow", "we'll be done here soon™", "got anything else to say?", "we're done now!", "this will be fixed by v6053", "expect it to be done within 180 business days!",
            "any colour you like", "someone tell Mojang about this", "you can't unbake baked models, so we'll store the unbaked models", "soon fully datagen ready",
            "packconverter when", "codecs ftw");
    private static final RandomSource RANDOM = RandomSource.create();

    private static final Path EXPORT_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve(Rainbow.MOD_ID);
    private static final Path PACK_DIRECTORY = Path.of("pack");
    private static final Path MAPPINGS_FILE = Path.of("geyser_mappings");
    private static final Path PACK_ZIP_FILE = Path.of("pack.zip");
    private static final Path REPORT_FILE = Path.of("report.txt");

    private Optional<BedrockPack> currentPack = Optional.empty();

    public void startPack(String name) throws IOException {
        if (currentPack.isPresent()) {
            throw new IllegalStateException("Already started a pack (" + currentPack.get().name() + ")");
        }

        Path packDirectory = createPackDirectory(name);
        BedrockPack pack = BedrockPack.builder(name, packDirectory.resolve(MAPPINGS_FILE), packDirectory.resolve(PACK_DIRECTORY),
                        new MinecraftPackSerializer(Minecraft.getInstance()), new MinecraftAssetResolver(Minecraft.getInstance()))
                .withPackZipFile(packDirectory.resolve(PACK_ZIP_FILE))
                .withGeometryRenderer(MinecraftGeometryRenderer.INSTANCE)
                .reportSuccesses()
                .build();
        currentPack = Optional.of(pack);
    }

    public void run(Consumer<BedrockPack> consumer) {
        currentPack.ifPresent(consumer);
    }

    public void runOrElse(Consumer<BedrockPack> consumer, Runnable runnable) {
        currentPack.ifPresentOrElse(consumer, runnable);
    }

    public Optional<Path> getExportPath() {
        return currentPack.map(pack -> EXPORT_DIRECTORY.resolve(pack.name()));
    }

    public boolean finish() {
        currentPack.map(pack -> {
            try {
                Files.writeString(getExportPath().orElseThrow().resolve(REPORT_FILE), createPackSummary(pack));
            } catch (IOException exception) {
                // TODO log
            }
            return pack.save();
        }).ifPresent(CompletableFuture::join);
        boolean wasPresent = currentPack.isPresent();
        currentPack = Optional.empty();
        return wasPresent;
    }

    private static String createPackSummary(BedrockPack pack) {
        String problems = pack.getReporter().getTreeReport();
        if (StringUtil.isBlank(problems)) {
            problems = "Well that's odd... there's nothing here!";
        }

        Set<BedrockItem> bedrockItems = pack.getBedrockItems();
        long attachables = bedrockItems.stream().filter(item -> item.attachable().isPresent()).count();
        long geometries = bedrockItems.stream().filter(item -> item.geometry().geometry().isPresent()).count();
        long animations = bedrockItems.stream().filter(item -> item.geometry().animation().isPresent()).count();

        return """
-- PACK GENERATION REPORT --
// %s

Generated pack: %s
Mappings written: %d
Item texture atlas size: %d
Attachables tried to export: %d
Geometry files tried to export: %d
Animations tried to export: %d
Textures tried to export: %d

-- MAPPING TREE REPORT --
%s
""".formatted(randomSummaryComment(), pack.name(), pack.getMappings(), pack.getItemTextureAtlasSize(),
                attachables, geometries, animations, pack.getAdditionalExportedTextures(), problems);
    }

    private static String randomSummaryComment() {
        if (RANDOM.nextDouble() < 0.5) {
            SplashRenderer splash = Minecraft.getInstance().getSplashManager().getSplash();
            if (splash == null) {
                return "Undefined Undefined :(";
            }
            return ((SplashRendererAccessor) splash).getSplash();
        }
        return randomBuiltinSummaryComment();
    }

    private static String randomBuiltinSummaryComment() {
        return PACK_SUMMARY_COMMENTS.get(RANDOM.nextInt(PACK_SUMMARY_COMMENTS.size()));
    }

    private static Path createPackDirectory(String name) throws IOException {
        Path path = EXPORT_DIRECTORY.resolve(name);
        CodecUtil.ensureDirectoryExists(path);
        return path;
    }
}
