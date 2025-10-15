package org.geysermc.rainbow.client;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.mapping.PackSerializer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MinecraftPackSerializer implements PackSerializer {
    private final HolderLookup.Provider registries;

    public MinecraftPackSerializer(Minecraft minecraft) {
        registries = Objects.requireNonNull(minecraft.level).registryAccess();
    }

    @Override
    public <T> CompletableFuture<?> saveJson(Codec<T> codec, T object, Path path) {
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        return CompletableFuture.runAsync(() -> {
            try {
                CodecUtil.trySaveJson(codec, object, path.resolveSibling(path.getFileName() + ".json"), ops);
            } catch (IOException exception) {
                // TODO log
            }
        }, Util.backgroundExecutor().forName("PackSerializer-saveJson"));
    }

    @Override
    public CompletableFuture<?> saveTexture(byte[] texture, Path path) {
        return CompletableFuture.runAsync(() -> {
            try {
                CodecUtil.ensureDirectoryExists(path.getParent());
                try (OutputStream outputTexture = new FileOutputStream(path.toFile())) {
                    outputTexture.write(texture);
                }
            } catch (IOException exception) {
                // TODO log
            }
        }, Util.backgroundExecutor().forName("PackSerializer-saveTexture"));
    }
}
