package org.geysermc.packgenerator.pack;

import com.mojang.serialization.Codec;

import java.util.List;

public record BedrockVersion(int major, int minor, int patch) {
    public static final Codec<BedrockVersion> CODEC = Codec.INT.listOf(3, 3)
            .xmap(list -> BedrockVersion.of(list.getFirst(), list.get(1), list.getLast()),
                    version -> List.of(version.major, version.minor, version.patch));

    public static BedrockVersion of(int patch) {
        return of(0, 0, patch);
    }

    public static BedrockVersion of(int minor, int patch) {
        return of(0, minor, patch);
    }

    public static BedrockVersion of(int major, int minor, int patch) {
        return new BedrockVersion(major, minor, patch);
    }

    public BedrockVersion increment() {
        return new BedrockVersion(major, minor, patch + 1);
    }
}
