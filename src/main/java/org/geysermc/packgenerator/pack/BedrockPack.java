package org.geysermc.packgenerator.pack;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.io.IOUtils;
import org.geysermc.packgenerator.CodecUtil;
import org.geysermc.packgenerator.PackConstants;
import org.geysermc.packgenerator.mapping.attachable.AttachableMapper;
import org.geysermc.packgenerator.mapping.geyser.GeyserMappings;
import org.geysermc.packgenerator.pack.attachable.BedrockAttachable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private final List<ResourceLocation> texturesToExport = new ArrayList<>();

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
            // TODO a proper way to get texture from item model
            itemTextures.withItemTexture(mapping, mapping.bedrockIdentifier().getPath());
            ResourceLocation texture = mapping.bedrockIdentifier();
            if (texture.getNamespace().equals("geyser_mc")) {
                texture = ResourceLocation.withDefaultNamespace(texture.getPath());
            }
            texturesToExport.add(texture);
            AttachableMapper.mapItem(stack, mapping.bedrockIdentifier(), texturesToExport::add).ifPresent(attachables::add);
        });
    }

    public void save() throws IOException {
        CodecUtil.trySaveJson(GeyserMappings.CODEC, mappings, exportPath.resolve(MAPPINGS_FILE));
        CodecUtil.trySaveJson(PackManifest.CODEC, manifest, packPath.resolve(MANIFEST_FILE));
        CodecUtil.trySaveJson(BedrockTextureAtlas.CODEC, BedrockTextureAtlas.itemAtlas(name, itemTextures), packPath.resolve(ITEM_ATLAS_FILE));
        for (BedrockAttachable attachable : attachables) {
            attachable.save(packPath.resolve(ATTACHABLES_DIRECTORY));
        }

        for (ResourceLocation texture : texturesToExport) {
            texture = texture.withPath(path -> "textures/" + path + ".png");
            try (InputStream inputTexture = Minecraft.getInstance().getResourceManager().open(texture)) {
                Path texturePath = packPath.resolve(texture.getPath());
                CodecUtil.ensureDirectoryExists(texturePath.getParent());
                try (OutputStream outputTexture = new FileOutputStream(texturePath.toFile())) {
                    IOUtils.copy(inputTexture, outputTexture);
                }
            } catch (FileNotFoundException exception) {
                // TODO
            }
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
