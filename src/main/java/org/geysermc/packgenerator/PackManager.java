package org.geysermc.packgenerator;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.geysermc.packgenerator.mappings.GeyserMappings;
import org.geysermc.packgenerator.pack.BedrockVersion;
import org.geysermc.packgenerator.pack.PackManifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class PackManager {
    private static final Path EXPORT_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("geyser");
    private static final Path PACK_DIRECTORY = Path.of("pack");

    private static final Path MAPPINGS_FILE = Path.of("geyser_mappings.json");
    private static final Path MANIFEST_FILE = Path.of("manifest.json");

    private static final PackManager INSTANCE = new PackManager();

    private String currentPackName;
    private Path exportPath;
    private Path packPath;
    private GeyserMappings mappings;
    private PackManifest manifest;

    private PackManager() {}

    public void startPack(String name) throws CommandSyntaxException, IOException {
        if (currentPackName != null) {
            throw new SimpleCommandExceptionType(Component.literal("Already started a pack (" + currentPackName + ")")).create();
        }

        exportPath = createPackDirectory(name);
        packPath = exportPath.resolve(PACK_DIRECTORY);
        mappings = readOrCreateMappings(exportPath.resolve(MAPPINGS_FILE));
        manifest = readOrCreateManifest(name, packPath.resolve(MANIFEST_FILE));
        currentPackName = name;
    }

    public boolean map(ItemStack stack, boolean throwOnModelMissing) throws CommandSyntaxException {
        ensurePackIsCreated();

        try {
            mappings.map(stack);
            return true;
        } catch (IllegalArgumentException exception) {
            if (throwOnModelMissing) {
                throw new SimpleCommandExceptionType(Component.literal("Item stack does not have a custom model")).create();
            } else {
                return false;
            }
        }
    }

    public void finish() throws CommandSyntaxException, IOException {
        ensurePackIsCreated();

        CodecUtil.trySaveJson(GeyserMappings.CODEC, mappings, exportPath.resolve(MAPPINGS_FILE));
        CodecUtil.trySaveJson(PackManifest.CODEC, manifest, packPath.resolve(MANIFEST_FILE));

        currentPackName = null;
    }

    private void ensurePackIsCreated() throws CommandSyntaxException {
        if (currentPackName == null) {
            throw new SimpleCommandExceptionType(Component.literal("Create a new pack first!")).create();
        }
    }

    private static Path createPackDirectory(String name) throws IOException {
        Path path = EXPORT_DIRECTORY.resolve(name);
        CodecUtil.ensureDirectoryExists(path);
        return path;
    }

    private static GeyserMappings readOrCreateMappings(Path path) throws IOException {
        if (Files.exists(path)) {
            return CodecUtil.tryReadJson(GeyserMappings.CODEC, path);
        }
        return new GeyserMappings();
    }

    private static PackManifest readOrCreateManifest(String name, Path path) throws IOException {
        if (Files.exists(path)) {
            return CodecUtil.tryReadJson(PackManifest.CODEC, path).increment();
        }
        return new PackManifest(
                new PackManifest.Header(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(1), PackConstants.ENGINE_VERSION),
                List.of(new PackManifest.Module(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(1))));
    }

    public static PackManager getInstance() {
        return INSTANCE;
    }
}
