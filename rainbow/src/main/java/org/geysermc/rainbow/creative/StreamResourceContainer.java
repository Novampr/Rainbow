package org.geysermc.rainbow.creative;

import com.google.gson.JsonElement;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.blockstate.BlockState;
import team.unnamed.creative.equipment.Equipment;
import team.unnamed.creative.font.Font;
import team.unnamed.creative.item.Item;
import team.unnamed.creative.lang.Language;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.model.Model;
import team.unnamed.creative.part.ResourcePackPart;
import team.unnamed.creative.serialize.minecraft.GsonUtil;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackStructure;
import team.unnamed.creative.serialize.minecraft.ResourceCategory;
import team.unnamed.creative.serialize.minecraft.atlas.AtlasSerializer;
import team.unnamed.creative.serialize.minecraft.blockstate.BlockStateSerializer;
import team.unnamed.creative.serialize.minecraft.equipment.EquipmentCategory;
import team.unnamed.creative.serialize.minecraft.font.FontSerializer;
import team.unnamed.creative.serialize.minecraft.item.ItemSerializer;
import team.unnamed.creative.serialize.minecraft.language.LanguageSerializer;
import team.unnamed.creative.serialize.minecraft.metadata.MetadataSerializer;
import team.unnamed.creative.serialize.minecraft.model.ModelSerializer;
import team.unnamed.creative.serialize.minecraft.sound.SoundRegistrySerializer;
import team.unnamed.creative.serialize.minecraft.sound.SoundSerializer;
import team.unnamed.creative.sound.Sound;
import team.unnamed.creative.sound.SoundRegistry;
import team.unnamed.creative.texture.Texture;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings({"UnstableApiUsage", "PatternValidation"})
public interface StreamResourceContainer extends ImmutableResourceContainer {

    @Nullable
    InputStream open(Key key) throws IOException;
    
    List<Key> assets(String category);

    Collection<String> namespaces();

    String METADATA_EXTENSION = MinecraftResourcePackStructure.METADATA_EXTENSION;
    String TEXTURE_EXTENSION = MinecraftResourcePackStructure.TEXTURE_EXTENSION;
    String TEXTURE_CATEGORY = MinecraftResourcePackStructure.TEXTURES_FOLDER;
    String SOUNDS_FILE = MinecraftResourcePackStructure.SOUNDS_FILE;

    private static Key addExtensionAndCategory(Key key, String category, String extension) {
        return Key.key(key.namespace(), category + "/" + key.value() + extension);
    }
    
    private static JsonElement parseJson(InputStream input) throws IOException {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return GsonUtil.parseReader(reader);
        }
    }

    @Nullable
    private static Writable writable(Key key, String category, String extension, ResourceOpener opener) throws IOException {
        Key withExtension = addExtensionAndCategory(key, category, extension);
        try (InputStream input = opener.open(withExtension)) {
            if (input != null) {
                return Writable.inputStream(() -> opener.open(withExtension));
            }
        }
        return null;
    }

    private static <T extends Keyed & ResourcePackPart> List<T> collect(ResourceCategory<T> category, Function<Key, T> deserializer,
                                                                        Function<String, List<Key>> assetLookup) {
        return assetLookup.apply(category.folder(-1)).stream()
                .map(deserializer)
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    default <T extends Keyed & ResourcePackPart> T deserialize(ResourceCategory<T> category, Key key) {
        Key withExtensionAndCategory = addExtensionAndCategory(key, category.folder(-1), category.extension(-1));
        try (InputStream input = open(withExtensionAndCategory)) {
            if (input != null) {
                return category.deserializer().deserialize(input, key);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return null;
    }

    @Override
    default @Nullable Atlas atlas(@NotNull Key key) {
        return deserialize(AtlasSerializer.CATEGORY, key);
    }

    @Override
    default @NotNull Collection<Atlas> atlases() {
        return collect(AtlasSerializer.CATEGORY, this::atlas, this::assets);
    }

    @Override
    default @Nullable BlockState blockState(@NotNull Key key) {
        return deserialize(BlockStateSerializer.CATEGORY, key);
    }

    @Override
    @NotNull
    default Collection<BlockState> blockStates() {
        return collect(BlockStateSerializer.CATEGORY, this::blockState, this::assets);
    }

    @Override
    default @Nullable Equipment equipment(@NotNull Key key) {
        return deserialize(EquipmentCategory.INSTANCE, key);
    }

    @Override
    @NotNull
    default Collection<Equipment> equipment() {
        return collect(EquipmentCategory.INSTANCE, this::equipment, this::assets);
    }

    @Override
    default @Nullable Font font(@NotNull Key key) {
        return deserialize(FontSerializer.CATEGORY, key);
    }

    @Override
    @NotNull
    default Collection<Font> fonts() {
        return collect(FontSerializer.CATEGORY, this::font, this::assets);
    }

    @Override
    default @Nullable Item item(@NotNull Key key) {
        return deserialize(ItemSerializer.CATEGORY, key);
    }

    @Override
    @NotNull
    default Collection<Item> items() {
        return collect(ItemSerializer.CATEGORY, this::item, this::assets);
    }

    @Override
    default @Nullable Language language(@NotNull Key key) {
        return deserialize(LanguageSerializer.CATEGORY, key);
    }

    @Override
    @NotNull
    default Collection<Language> languages() {
        return collect(LanguageSerializer.CATEGORY, this::language, this::assets);
    }

    @Override
    default @Nullable Model model(@NotNull Key key) {
        return deserialize(ModelSerializer.CATEGORY, key);
    }

    @Override
    @NotNull
    default Collection<Model> models() {
        return collect(ModelSerializer.CATEGORY, this::model, this::assets);
    }

    @Override
    default @Nullable SoundRegistry soundRegistry(@NotNull String namespace) {
        try (InputStream input = open(Key.key(namespace, SOUNDS_FILE))) {
            if (input != null) {
                return SoundRegistrySerializer.INSTANCE.readFromTree(parseJson(input), namespace);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return null;
    }

    @Override
    @NotNull
    default Collection<SoundRegistry> soundRegistries() {
        return namespaces().stream()
                .map(this::soundRegistry)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    default @Nullable Sound sound(@NotNull Key key) {
        return deserialize(SoundSerializer.CATEGORY, key);
    }

    @Override
    @NotNull
    default Collection<Sound> sounds() {
        return collect(SoundSerializer.CATEGORY, this::sound, this::assets);
    }

    @Override
    default @Nullable Texture texture(@NotNull Key key) {
        try {
            Writable texture = writable(key, TEXTURE_CATEGORY, TEXTURE_EXTENSION, this::open);
            if (texture != null) {
                Metadata metadata = Metadata.empty();
                try (InputStream metadataStream = open(addExtensionAndCategory(key, TEXTURE_CATEGORY, METADATA_EXTENSION))) {
                    if (metadataStream != null) {
                        metadata = MetadataSerializer.INSTANCE.readFromTree(parseJson(metadataStream));
                    }
                }
                return Texture.texture(key, texture, metadata);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return null;
    }

    @Override
    @NotNull
    default Collection<Texture> textures() {
        return assets(TEXTURE_CATEGORY).stream()
                .map(this::texture)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    default @Nullable Writable unknownFile(@NotNull String path) {
        throw new UnsupportedOperationException("Unsupported by StreamResourceContainer");
    }

    @Override
    default @NotNull Map<String, Writable> unknownFiles() {
        return Map.of();
    }

    @FunctionalInterface
    interface ResourceOpener {

        InputStream open(Key key) throws IOException;
    }
}
