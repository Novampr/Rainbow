package org.geysermc.packgenerator;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
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

    public Optional<Boolean> map(ItemStack stack) throws CommandSyntaxException {
        ensurePackIsCreated();

        return currentPack.map(stack);
    }

    public boolean finish() throws CommandSyntaxException {
        ensurePackIsCreated();
        boolean success = currentPack.save();
        currentPack = null;
        return success;
    }

    private void ensurePackIsCreated() throws CommandSyntaxException {
        if (currentPack == null) {
            throw new SimpleCommandExceptionType(Component.literal("Create a new pack first!")).create();
        }
    }

    public static PackManager getInstance() {
        return INSTANCE;
    }
}
