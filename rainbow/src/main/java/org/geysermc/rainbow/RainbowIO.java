package org.geysermc.rainbow;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Optional;

public final class RainbowIO {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RainbowIO() {}

    public static <T> T safeIO(IOSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (IOException exception) {
            LOGGER.error("Failed to perform IO operation!", exception);
            return defaultValue;
        }
    }

    public static <T> Optional<T> safeIO(IOSupplier<T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (IOException exception) {
            LOGGER.error("Failed to perform IO operation!", exception);
            return Optional.empty();
        }
    }

    public static void safeIO(IORunnable runnable) {
        try {
            runnable.run();
        } catch (IOException exception) {
            LOGGER.error("Failed to perform IO operation!", exception);
        }
    }

    @FunctionalInterface
    public interface IOSupplier<T> {

        T get() throws IOException;
    }

    @FunctionalInterface
    public interface IORunnable {

        void run() throws IOException;
    }
}
