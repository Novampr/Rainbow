package org.geysermc.packgenerator;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.geysermc.packgenerator.pack.BedrockPack;

import java.io.IOException;

public final class PackManager {

    private static final PackManager INSTANCE = new PackManager();

    private BedrockPack currentPack;

    private PackManager() {}

    public void startPack(String name) throws CommandSyntaxException, IOException {
        if (currentPack != null) {
            throw new SimpleCommandExceptionType(Component.literal("Already started a pack (" + currentPack.name() + ")")).create();
        }

        currentPack = new BedrockPack(name);
    }

    public boolean map(ItemStack stack, boolean throwOnModelMissing) throws CommandSyntaxException {
        ensurePackIsCreated();

        try {
            currentPack.map(stack);
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
        currentPack.save();
        currentPack = null;
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
