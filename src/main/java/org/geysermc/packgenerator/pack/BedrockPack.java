package org.geysermc.packgenerator.pack;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import org.geysermc.packgenerator.CodecUtil;
import org.geysermc.packgenerator.PackConstants;
import org.geysermc.packgenerator.mapping.attachable.AttachableMapper;
import org.geysermc.packgenerator.mapping.geyser.GeyserMappings;
import org.geysermc.packgenerator.pack.attachable.BedrockAttachable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BedrockPack {
    private static final Path EXPORT_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("geyser");
    private static final Path PACK_DIRECTORY = Path.of("pack");
    private static final Path ATTACHABLES_DIRECTORY = Path.of("attachables");

    private static final Path MAPPINGS_FILE = Path.of("geyser_mappings.json");
    private static final Path MANIFEST_FILE = Path.of("manifest.json");
    private static final Path ITEM_ATLAS_FILE = Path.of("textures/item_texture.json");

    private final String name;
    private final Path exportPath;
    private final Path packPath;
    private final PackManifest manifest;
    private final GeyserMappings mappings;
    private final BedrockTextures.Builder itemTextures;
    private final List<BedrockAttachable> attachables = new ArrayList<>();

    public BedrockPack(String name) throws IOException {
        this.name = name;
        exportPath = createPackDirectory(name);
        packPath = exportPath.resolve(PACK_DIRECTORY);
        mappings = CodecUtil.readOrCompute(GeyserMappings.CODEC, exportPath.resolve(MAPPINGS_FILE), GeyserMappings::new);
        manifest = CodecUtil.readOrCompute(PackManifest.CODEC, packPath.resolve(MANIFEST_FILE), () -> defaultManifest(name)).increment();
        itemTextures = CodecUtil.readOrCompute(BedrockTextureAtlas.ITEM_ATLAS_CODEC, packPath.resolve(ITEM_ATLAS_FILE),
                () -> BedrockTextureAtlas.itemAtlas(name, BedrockTextures.builder())).textures().toBuilder();
    }

    public String name() {
        return name;
    }

    public void map(ItemStack stack) {
        mappings.map(stack, mapping -> {
            itemTextures.withItemTexture(mapping, mapping.bedrockIdentifier().getPath());
            AttachableMapper.mapItem(stack, mapping.bedrockIdentifier()).ifPresent(attachables::add);
        });
    }

    public void save() throws IOException {
        CodecUtil.trySaveJson(GeyserMappings.CODEC, mappings, exportPath.resolve(MAPPINGS_FILE));
        CodecUtil.trySaveJson(PackManifest.CODEC, manifest, packPath.resolve(MANIFEST_FILE));
        CodecUtil.trySaveJson(BedrockTextureAtlas.CODEC, BedrockTextureAtlas.itemAtlas(name, itemTextures), packPath.resolve(ITEM_ATLAS_FILE));
        for (BedrockAttachable attachable : attachables) {
            attachable.save(packPath.resolve(ATTACHABLES_DIRECTORY));
        }
    }

    private static Path createPackDirectory(String name) throws IOException {
        Path path = EXPORT_DIRECTORY.resolve(name);
        CodecUtil.ensureDirectoryExists(path);
        return path;
    }

    private static PackManifest defaultManifest(String name) {
        return new PackManifest(new PackManifest.Header(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(0), PackConstants.ENGINE_VERSION),
                List.of(new PackManifest.Module(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(0))));
    }
}
