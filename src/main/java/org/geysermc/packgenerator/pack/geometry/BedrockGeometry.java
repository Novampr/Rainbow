package org.geysermc.packgenerator.pack.geometry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import org.geysermc.packgenerator.pack.BedrockVersion;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BedrockGeometry(BedrockVersion formatVersion, List<GeometryDefinition> definitions) {
    public static final Vector3f VECTOR3F_ZERO = new Vector3f();

    public static final Codec<BedrockGeometry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BedrockVersion.STRING_CODEC.fieldOf("format_version").forGetter(BedrockGeometry::formatVersion),
                    GeometryDefinition.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("minecraft:geometry").forGetter(BedrockGeometry::definitions)
            ).apply(instance, BedrockGeometry::new)
    );

    public record GeometryDefinition(GeometryInfo info, List<Bone> bones) {
        public static final Codec<GeometryDefinition> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        GeometryInfo.CODEC.fieldOf("description").forGetter(GeometryDefinition::info),
                        Bone.CODEC.listOf().optionalFieldOf("bones", List.of()).forGetter(GeometryDefinition::bones)
                ).apply(instance, GeometryDefinition::new)
        );
    }

    public record GeometryInfo(String identifier, Optional<Float> visibleBoundsWidth, Optional<Float> visibleBoundsHeight, Optional<Vector3f> visibleBoundsOffset,
                               Optional<Integer> textureWidth, Optional<Integer> textureHeight) {
        public static final Codec<GeometryInfo> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("identifier").forGetter(GeometryInfo::identifier),
                        Codec.FLOAT.optionalFieldOf("visible_bounds_width").forGetter(GeometryInfo::visibleBoundsWidth),
                        Codec.FLOAT.optionalFieldOf("visible_bounds_height").forGetter(GeometryInfo::visibleBoundsHeight),
                        ExtraCodecs.VECTOR3F.optionalFieldOf("visible_bounds_offset").forGetter(GeometryInfo::visibleBoundsOffset),
                        Codec.INT.optionalFieldOf("texture_width").forGetter(GeometryInfo::textureWidth),
                        Codec.INT.optionalFieldOf("texture_height").forGetter(GeometryInfo::textureHeight)
                ).apply(instance, GeometryInfo::new)
        );
    }

    public record Bone(String name, Optional<String> parent, Vector3f pivot, Vector3f rotation, boolean mirror,
                       float inflate, List<Cube> cubes) {
        public static final Codec<Bone> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("name").forGetter(Bone::name),
                        Codec.STRING.optionalFieldOf("parent").forGetter(Bone::parent),
                        ExtraCodecs.VECTOR3F.optionalFieldOf("pivot", VECTOR3F_ZERO).forGetter(Bone::pivot),
                        ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", VECTOR3F_ZERO).forGetter(Bone::rotation),
                        Codec.BOOL.optionalFieldOf("mirror", false).forGetter(Bone::mirror),
                        Codec.FLOAT.optionalFieldOf("inflate", 0.0F).forGetter(Bone::inflate),
                        Cube.CODEC.listOf().optionalFieldOf("cubes", List.of()).forGetter(Bone::cubes)
                ).apply(instance, Bone::new)
        );
    }

    public record Cube(Vector3f origin, Vector3f size, Vector3f rotation, Vector3f pivot, float inflate, boolean mirror,
                       Map<Direction, Face> faces) {
        private static final Codec<Map<Direction, Face>> FACE_MAP_CODEC = Codec.unboundedMap(Direction.CODEC, Face.CODEC);
        public static final Codec<Cube> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ExtraCodecs.VECTOR3F.fieldOf("origin").forGetter(Cube::origin),
                        ExtraCodecs.VECTOR3F.fieldOf("size").forGetter(Cube::size),
                        ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", VECTOR3F_ZERO).forGetter(Cube::size),
                        ExtraCodecs.VECTOR3F.optionalFieldOf("pivot", VECTOR3F_ZERO).forGetter(Cube::pivot),
                        Codec.FLOAT.optionalFieldOf("inflate", 0.0F).forGetter(Cube::inflate),
                        Codec.BOOL.optionalFieldOf("mirror", false).forGetter(Cube::mirror),
                        FACE_MAP_CODEC.optionalFieldOf("uv", Map.of()).forGetter(Cube::faces)
                ).apply(instance, Cube::new)
        );
    }

    public record Face(Vector2f origin, Vector2f size) {
        public static final Codec<Face> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ExtraCodecs.VECTOR2F.fieldOf("uv").forGetter(Face::origin),
                        ExtraCodecs.VECTOR2F.fieldOf("uv_size").forGetter(Face::size)
                ).apply(instance, Face::new)
        );
    }
}
