package org.geysermc.rainbow.creative;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.blockstate.BlockState;
import team.unnamed.creative.equipment.Equipment;
import team.unnamed.creative.font.Font;
import team.unnamed.creative.item.Item;
import team.unnamed.creative.lang.Language;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.overlay.ResourceContainer;
import team.unnamed.creative.resources.MergeStrategy;
import team.unnamed.creative.sound.Sound;
import team.unnamed.creative.sound.SoundRegistry;
import team.unnamed.creative.texture.Texture;

@SuppressWarnings("NonExtendableApiUsage")
public interface ImmutableResourceContainer extends ResourceContainer {

    private static <T> T thr() {
        throw new UnsupportedOperationException("ResourceContainer is immutable");
    }

    @Override
    default void atlas(@NotNull Atlas atlas) {
        thr();
    }

    @Override
    default boolean removeAtlas(@NotNull Key key) {
        return thr();
    }

    @Override
    default void blockState(@NotNull BlockState state) {
        thr();
    }

    @Override
    default boolean removeBlockState(@NotNull Key key) {
        return thr();
    }

    @Override
    default void equipment(@NotNull Equipment equipment) {
        thr();
    }

    @Override
    default boolean removeEquipment(@NotNull Key key) {
        return thr();
    }

    @Override
    default void font(@NotNull Font font) {
        thr();
    }

    @Override
    default boolean removeFont(@NotNull Key key) {
        return thr();
    }

    @Override
    default void item(@NotNull Item item) {
        thr();
    }

    @Override
    default boolean removeItem(@NotNull Key key) {
        return thr();
    }

    @Override
    default void language(@NotNull Language language) {
        thr();
    }

    @Override
    default boolean removeLanguage(@NotNull Key key) {
        return thr();
    }

    @Override
    default void model(@NotNull Model model) {
        thr();
    }

    @Override
    default boolean removeModel(@NotNull Key key) {
        return thr();
    }

    @Override
    default void soundRegistry(@NotNull SoundRegistry soundRegistry) {
        thr();
    }

    @Override
    default boolean removeSoundRegistry(@NotNull String namespace) {
        return thr();
    }

    @Override
    default void sound(@NotNull Sound sound) {
        thr();
    }

    @Override
    default boolean removeSound(@NotNull Key key) {
        return thr();
    }

    @Override
    default void texture(@NotNull Texture texture) {
        thr();
    }

    @Override
    default boolean removeTexture(@NotNull Key key) {
        return thr();
    }

    @Override
    default void unknownFile(@NotNull String path, @NotNull Writable data) {
        thr();
    }

    @Override
    default boolean removeUnknownFile(@NotNull String path) {
        return thr();
    }

    @Override
    default void merge(@NotNull ResourceContainer other, @NotNull MergeStrategy strategy) {
        thr();
    }
}
