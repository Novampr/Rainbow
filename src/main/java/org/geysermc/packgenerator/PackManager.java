package org.geysermc.packgenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.geysermc.packgenerator.mappings.GeyserMappings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PackManager {
    public static final Path EXPORT_DIRECTORY = FabricLoader.getInstance().getGameDir()
            .resolve("geyser");

    private static final PackManager INSTANCE = new PackManager();

    private String currentPackName;
    private Path exportPath;
    private GeyserMappings mappings;

    private PackManager() {}

    public void startPack(String name) throws CommandSyntaxException {
        if (currentPackName != null) {
            throw new SimpleCommandExceptionType(Component.literal("Already started a pack with name " + currentPackName)).create();
        }
        currentPackName = name;
        exportPath = createPackDirectory(currentPackName);
        mappings = new GeyserMappings();
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

    public void finish() throws CommandSyntaxException {
        ensurePackIsCreated();

        JsonElement savedMappings = GeyserMappings.CODEC.encodeStart(JsonOps.INSTANCE, mappings)
                .getOrThrow(error -> new SimpleCommandExceptionType(Component.literal("Failed to encode Geyser mappings! " + error)).create());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.writeString(exportPath.resolve("geyser_mappings.json"), gson.toJson(savedMappings));
        } catch (IOException exception) {
            GeyserMappingsGenerator.LOGGER.warn("Failed to write Geyser mappings to pack!", exception);
            throw new SimpleCommandExceptionType(Component.literal("Failed to write Geyser mappings to pack!")).create();
        }

        currentPackName = null;
    }

    private void ensurePackIsCreated() throws CommandSyntaxException {
        if (currentPackName == null) {
            throw new SimpleCommandExceptionType(Component.literal("Create a new pack first!")).create();
        }
    }

    private static Path createPackDirectory(String name) throws CommandSyntaxException {
        Path path = EXPORT_DIRECTORY.resolve(name);
        if (!Files.isDirectory(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException exception) {
                GeyserMappingsGenerator.LOGGER.warn("Failed to create pack export directory!", exception);
                throw new SimpleCommandExceptionType(Component.literal("Failed to create pack export directory for pack " + name)).create();
            }
        }
        return path;
    }

    public static PackManager getInstance() {
        return INSTANCE;
    }
}
