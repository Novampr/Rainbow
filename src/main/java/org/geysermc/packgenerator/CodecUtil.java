package org.geysermc.packgenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class CodecUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static <O, T> RecordCodecBuilder<O, T> unitVerifyCodec(Codec<T> codec, String field, T value) {
        return codec.validate(read -> {
            if (!read.equals(value)) {
                return DataResult.error(() -> field + " must equal " + value + ", was " + read);
            }
            return DataResult.success(read);
        }).fieldOf(field).forGetter(object -> value);
    }

    public static <T> T readOrCompute(Codec<T> codec, Path path, Supplier<T> supplier) throws IOException {
        if (Files.exists(path)) {
            return tryReadJson(codec, path);
        }
        return supplier.get();
    }

    public static <T> T tryReadJson(Codec<T> codec, Path path) throws IOException {
        try {
            String raw = Files.readString(path);
            JsonElement json = GSON.fromJson(raw, JsonElement.class);
            return codec.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (IOException exception) {
            GeyserMappingsGenerator.LOGGER.warn("Failed to read JSON file {}!", path, exception);
            throw exception;
        }
    }

    public static <T> void trySaveJson(Codec<T> codec, T object, Path path) throws IOException {
        JsonElement json = codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow();

        try {
            ensureDirectoryExists(path.getParent());
            Files.writeString(path, GSON.toJson(json));
        } catch (IOException exception) {
            GeyserMappingsGenerator.LOGGER.warn("Failed to write file {}!", path, exception);
            throw exception;
        }
    }

    public static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException exception) {
                GeyserMappingsGenerator.LOGGER.warn("Failed to create directory!", exception);
                throw exception;
            }
        }
    }
}
