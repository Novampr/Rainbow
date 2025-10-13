package org.geysermc.rainbow.creative;

import net.kyori.adventure.key.Key;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.geysermc.rainbow.KeyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.metadata.Metadata;
import team.unnamed.creative.overlay.Overlay;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MinecraftCreativeResourcePack extends CachingStreamResourceContainer implements ImmutableResourcePack {
    // Matches a path in 3 groups: the first directory, the rest of the path, and the file extension (e.g. .json)
    private static final Pattern PATH_SANITIZE_REGEX = Pattern.compile("(^\\w+/)(.*)(\\.\\w+$)");

    private final ResourceManager resourceManager;

    public MinecraftCreativeResourcePack(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    public @Nullable InputStream open(Key key) throws IOException {
        Optional<Resource> resource = resourceManager.getResource(KeyUtil.keyToResourceLocation(key));
        if (resource.isPresent()) {
            return resource.get().open();
        }
        return null;
    }

    @Override
    public List<Key> assets(String category) {
        return resourceManager.listResources(category, resource -> true).keySet().stream()
                .map(location -> location.withPath(path -> PATH_SANITIZE_REGEX.matcher(path).replaceAll("$2")))
                .map(KeyUtil::resourceLocationToKey)
                .toList();
    }

    @Override
    public Collection<String> namespaces() {
        return resourceManager.getNamespaces();
    }

    @Override
    public @Nullable Writable icon() {
        return null;
    }

    @Override
    public @NotNull Metadata metadata() {
        return Metadata.empty();
    }

    @Override
    public @Nullable Overlay overlay(@NotNull String directory) {
        return null;
    }

    @Override
    public @NotNull Collection<Overlay> overlays() {
        return List.of();
    }
}
