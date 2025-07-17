package org.geysermc.rainbow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class CodecUtil {
    // It's fine to cast to mutable here since codecs won't change the data
    public static final Codec<Vector2fc> VECTOR2F_CODEC = ExtraCodecs.VECTOR2F.xmap(vector -> vector, vector -> (Vector2f) vector);
    public static final Codec<Vector3fc> VECTOR3F_CODEC = ExtraCodecs.VECTOR3F.xmap(vector -> vector, vector -> (Vector3f) vector);

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
            Rainbow.LOGGER.warn("Failed to read JSON file {}!", path, exception);
            throw exception;
        }
    }

    public static <T> void trySaveJson(Codec<T> codec, T object, Path path) throws IOException {
        JsonElement json = codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow();

        try {
            ensureDirectoryExists(path.getParent());
            Files.writeString(path, GSON.toJson(json));
        } catch (IOException exception) {
            Rainbow.LOGGER.warn("Failed to write file {}!", path, exception);
            throw exception;
        }
    }

    public static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException exception) {
                Rainbow.LOGGER.warn("Failed to create directory!", exception);
                throw exception;
            }
        }
    }
}
