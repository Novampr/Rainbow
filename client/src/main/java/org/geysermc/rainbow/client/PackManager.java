package org.geysermc.rainbow.client;

import net.minecraft.client.Minecraft;
import org.geysermc.rainbow.pack.BedrockPack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public final class PackManager {

    private Optional<BedrockPack> currentPack = Optional.empty();

    public void startPack(String name) throws IOException {
        if (currentPack.isPresent()) {
            throw new IllegalStateException("Already started a pack (" + currentPack.get().name() + ")");
        }

        currentPack = Optional.of(new BedrockPack(name, new MinecraftAssetResolver(Minecraft.getInstance())));
    }

    public void run(Consumer<BedrockPack> consumer) {
        currentPack.ifPresent(consumer);
    }

    public void runOrElse(Consumer<BedrockPack> consumer, Runnable runnable) {
        currentPack.ifPresentOrElse(consumer, runnable);
    }

    public Optional<Path> getExportPath() {
        return currentPack.map(BedrockPack::getExportPath);
    }

    public Optional<Boolean> finish() {
        Optional<Boolean> success = currentPack.map(BedrockPack::save);
        currentPack = Optional.empty();
        return success;
    }
}
