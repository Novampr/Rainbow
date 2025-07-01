package org.geysermc.packgenerator.pack.attachable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import org.geysermc.packgenerator.CodecUtil;
import org.geysermc.packgenerator.PackConstants;
import org.geysermc.packgenerator.mapping.geyser.GeyserMapping;
import org.geysermc.packgenerator.pack.BedrockTextures;
import org.geysermc.packgenerator.pack.BedrockVersion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record BedrockAttachable(BedrockVersion formatVersion, AttachableInfo info) {
    public static final Codec<BedrockAttachable> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BedrockVersion.STRING_CODEC.fieldOf("format_version").forGetter(BedrockAttachable::formatVersion),
                    AttachableInfo.CODEC.fieldOf("description").fieldOf("minecraft:attachable").forGetter(BedrockAttachable::info)
            ).apply(instance, BedrockAttachable::new)
    );

    public static Builder builder(ResourceLocation identifier) {
        return new Builder(identifier);
    }

    public static BedrockAttachable equipment(ResourceLocation identifier, EquipmentSlot slot, String texture) {
        String script = switch (slot) {
            case HEAD -> "v.helmet_layer_visible = 0.0;";
            case CHEST -> "v.chest_layer_visible = 0.0;";
            case LEGS -> "v.leg_layer_visible = 0.0;";
            case FEET -> "v.boot_layer_visible = 0.0;";
            default -> "";
        };
        return builder(identifier)
                .withMaterial(DisplaySlot.DEFAULT, "armor")
                .withMaterial(DisplaySlot.ENCHANTED, "armor_enchanted")
                .withTexture(DisplaySlot.DEFAULT, texture)
                .withTexture(DisplaySlot.ENCHANTED, VanillaTextures.ENCHANTED_ACTOR_GLINT)
                .withGeometry(DisplaySlot.DEFAULT, VanillaGeometries.fromEquipmentSlot(slot))
                .withScript("parent_setup", script)
                .withRenderController(VanillaRenderControllers.ARMOR)
                .build();
    }

    public void save(Path attachablesPath) throws IOException {
        // Get a save attachable path by using Geyser's way of getting icons
        CodecUtil.trySaveJson(CODEC, this, attachablesPath.resolve(GeyserMapping.iconFromResourceLocation(info.identifier) + ".json"));
    }

    public static class Builder {
        private final ResourceLocation identifier;
        private final EnumMap<DisplaySlot, String> materials = new EnumMap<>(DisplaySlot.class);
        private final EnumMap<DisplaySlot, String> textures = new EnumMap<>(DisplaySlot.class);
        private final EnumMap<DisplaySlot, String> geometries = new EnumMap<>(DisplaySlot.class);
        private final Map<String, String> animations = new HashMap<>();
        private final Map<String, String> scripts = new HashMap<>();
        private final List<String> renderControllers = new ArrayList<>();

        public Builder(ResourceLocation identifier) {
            this.identifier = identifier;
        }

        public Builder withMaterial(DisplaySlot slot, String material) {
            materials.put(slot, material);
            return this;
        }

        public Builder withTexture(DisplaySlot slot, String texture) {
            textures.put(slot, BedrockTextures.TEXTURES_FOLDER + texture);
            return this;
        }

        public Builder withGeometry(DisplaySlot slot, String geometry) {
            geometries.put(slot, geometry);
            return this;
        }

        public Builder withAnimation(String key, String animation) {
            animations.put(key, animation);
            return this;
        }

        public Builder withScript(String key, String script) {
            scripts.put(key, script);
            return this;
        }

        public Builder withRenderController(String controller) {
            renderControllers.add(controller);
            return this;
        }

        public BedrockAttachable build() {
            return new BedrockAttachable(PackConstants.ENGINE_VERSION,
                    new AttachableInfo(identifier, verifyDefault(materials), verifyDefault(textures), verifyDefault(geometries), Map.copyOf(animations), Map.copyOf(scripts), List.copyOf(renderControllers)));
        }

        private static DisplayMap verifyDefault(EnumMap<DisplaySlot, String> map) {
            if (!map.containsKey(DisplaySlot.DEFAULT)) {
                throw new IllegalStateException("DisplayMap must have a default key");
            }
            return new DisplayMap(map);
        }
    }

    public record AttachableInfo(ResourceLocation identifier, DisplayMap materials, DisplayMap textures,
                                 DisplayMap geometry, Map<String, String> animations, Map<String, String> scripts,
                                 List<String> renderControllers) {
        private static final Codec<Map<String, String>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);
        public static final Codec<AttachableInfo> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ResourceLocation.CODEC.fieldOf("identifier").forGetter(AttachableInfo::identifier),
                        DisplayMap.CODEC.fieldOf("materials").forGetter(AttachableInfo::materials),
                        DisplayMap.CODEC.fieldOf("textures").forGetter(AttachableInfo::textures),
                        DisplayMap.CODEC.fieldOf("geometry").forGetter(AttachableInfo::geometry),
                        STRING_MAP_CODEC.optionalFieldOf("animations", Map.of()).forGetter(AttachableInfo::animations),
                        STRING_MAP_CODEC.optionalFieldOf("scripts", Map.of()).forGetter(AttachableInfo::scripts),
                        Codec.STRING.listOf().optionalFieldOf("render_controllers", List.of()).forGetter(AttachableInfo::renderControllers)
                ).apply(instance, AttachableInfo::new)
        );
    }

    public record DisplayMap(EnumMap<DisplaySlot, String> map) {
        public static final Codec<DisplayMap> CODEC = Codec.unboundedMap(DisplaySlot.CODEC, Codec.STRING)
                .xmap(map -> map.isEmpty() ? new DisplayMap(new EnumMap<>(DisplaySlot.class)) : new DisplayMap(new EnumMap<>(map)), DisplayMap::map);
    }

    public enum DisplaySlot implements StringRepresentable {
        DEFAULT("default"),
        ENCHANTED("enchanted");

        public static final Codec<DisplaySlot> CODEC = StringRepresentable.fromEnum(DisplaySlot::values);

        private final String name;

        DisplaySlot(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
