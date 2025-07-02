package org.geysermc.packgenerator;

import net.minecraft.world.item.ItemStack;
import org.geysermc.packgenerator.pack.BedrockPack;

import java.io.IOException;
import java.util.Optional;

public final class PackManager {

    private static final PackManager INSTANCE = new PackManager();

    private BedrockPack currentPack;

    private PackManager() {}

    public void startPack(String name) throws IOException {
        if (currentPack != null) {
            throw new IllegalStateException("Already started a pack (" + currentPack.name() + ")");
        }

        currentPack = new BedrockPack(name);
    }

    public Optional<Boolean> map(ItemStack stack) {
        ensurePackIsCreated();

        return currentPack.map(stack);
    }

    public boolean finish() {
        ensurePackIsCreated();
        boolean success = currentPack.save();
        currentPack = null;
        return success;
    }

    public void ensurePackIsCreated() {
        if (currentPack == null) {
            throw new IllegalStateException("Create a new pack first!");
        }
    }

    public static PackManager getInstance() {
        return INSTANCE;
    }
}
