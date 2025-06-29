package org.geysermc.packgenerator;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.geysermc.packgenerator.mappings.GeyserMappings;
import org.geysermc.packgenerator.pack.PackManifest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class PackManager {
    private static final Path EXPORT_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("geyser");
    private static final Function<Path, Path> PACK_DIRECTORY = path -> path.resolve("pack");

    private static final PackManager INSTANCE = new PackManager();

    private String currentPackName;
    private Path exportPath;
    private Path packPath;
    private GeyserMappings mappings;
    private PackManifest manifest;

    private PackManager() {}

    public void startPack(String name) throws CommandSyntaxException, IOException {
        if (currentPackName != null) {
            throw new SimpleCommandExceptionType(Component.literal("Already started a pack with name " + currentPackName)).create();
        }
        currentPackName = name;
        exportPath = createPackDirectory(currentPackName);
        packPath = PACK_DIRECTORY.apply(exportPath);
        mappings = new GeyserMappings();
        manifest = new PackManifest(new PackManifest.Header(name, "PLACEHOLDER", UUID.randomUUID(), "PLACEHOLDER"),
                List.of(new PackManifest.Module(new PackManifest.Header(name, "PLACEHOLDER", UUID.randomUUID(), "PLACEHOLDER"))));
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

        CodecUtil.trySaveJson(GeyserMappings.CODEC, mappings, exportPath.resolve("geyser_mappings.json"));
        CodecUtil.trySaveJson(PackManifest.CODEC, manifest, packPath.resolve("manifest.json"));

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

    public static PackManager getInstance() {
        return INSTANCE;
    }
}
