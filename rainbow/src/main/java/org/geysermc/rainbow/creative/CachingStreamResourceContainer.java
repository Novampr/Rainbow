package org.geysermc.rainbow.creative;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.part.ResourcePackPart;
import team.unnamed.creative.serialize.minecraft.ResourceCategory;
import team.unnamed.creative.sound.SoundRegistry;
import team.unnamed.creative.texture.Texture;

@SuppressWarnings("UnstableApiUsage")
public abstract class CachingStreamResourceContainer implements StreamResourceContainer {
    private final Reference2ObjectMap<ResourceCategory<?>, Object2ObjectMap<Key, Object>> cache = new Reference2ObjectOpenHashMap<>();
    private final Object2ObjectMap<String, SoundRegistry> soundRegistryCache = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<Key, Texture> textureCache = new Object2ObjectOpenHashMap<>();

    @SuppressWarnings("unchecked")
    private <T extends Keyed & ResourcePackPart> T cacheOrDeserialize(ResourceCategory<T> deserializer, Key key) {
        Object2ObjectMap<Key, Object> deserializerCache = cache.computeIfAbsent(deserializer, cacheKey -> new Object2ObjectOpenHashMap<>());
        return (T) deserializerCache.computeIfAbsent(key, cacheKey -> StreamResourceContainer.super.deserialize(deserializer, key));
    }

    @Override
    public <T extends Keyed & ResourcePackPart> @Nullable T deserialize(ResourceCategory<T> deserializer, Key key) {
        return cacheOrDeserialize(deserializer, key);
    }

    @Override
    public @Nullable SoundRegistry soundRegistry(@NotNull String namespace) {
        return soundRegistryCache.computeIfAbsent(namespace, cacheNamespace -> StreamResourceContainer.super.soundRegistry(namespace));
    }

    @Override
    public @Nullable Texture texture(@NotNull Key key) {
        return textureCache.computeIfAbsent(key, cacheKey -> StreamResourceContainer.super.texture(key));
    }
}
