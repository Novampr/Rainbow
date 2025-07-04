package org.geysermc.packgenerator;

import org.geysermc.packgenerator.pack.BedrockPack;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PackManager {

    private Optional<BedrockPack> currentPack = Optional.empty();

    public void startPack(String name) throws IOException {
        if (currentPack.isPresent()) {
            throw new IllegalStateException("Already started a pack (" + currentPack.get().name() + ")");
        }

        currentPack = Optional.of(new BedrockPack(name));
    }

    public void run(Consumer<BedrockPack> consumer) {
        currentPack.ifPresent(consumer);
    }

    public void runOrElse(Consumer<BedrockPack> consumer, Runnable runnable) {
        currentPack.ifPresentOrElse(consumer, runnable);
    }

    public <T> Optional<T> run(Function<BedrockPack, T> function) {
        return currentPack.map(function);
    }

    public Optional<Boolean> finish() {
        Optional<Boolean> success = currentPack.map(BedrockPack::save);
        currentPack = Optional.empty();
        return success;
    }
}
