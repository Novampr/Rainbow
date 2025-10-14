package org.geysermc.rainbow.creative;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.metadata.sodium.SodiumMeta;
import team.unnamed.creative.overlay.Overlay;

@SuppressWarnings("NonExtendableApiUsage")
public interface ImmutableResourcePack extends ResourcePack, ImmutableResourceContainer {

    private static void thr() {
        throw new UnsupportedOperationException("ResourcePack is immutable");
    }

    @Override
    default void icon(@Nullable Writable icon) {
        thr();
    }

    @Override
    default void metadata(@NotNull Metadata metadata) {
        thr();
    }

    @Override
    default void overlay(@NotNull Overlay overlay) {
        thr();
    }

    @Override
    default void sodiumMeta(@NotNull SodiumMeta sodiumMeta) {
        thr();
    }
}
