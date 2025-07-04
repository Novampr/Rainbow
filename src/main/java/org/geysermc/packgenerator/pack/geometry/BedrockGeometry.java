package org.geysermc.packgenerator.pack.geometry;

import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import org.geysermc.packgenerator.CodecUtil;
import org.geysermc.packgenerator.pack.BedrockVersion;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BedrockGeometry(BedrockVersion formatVersion, List<GeometryDefinition> definitions) {
    public static final BedrockVersion FORMAT_VERSION = BedrockVersion.of(1, 21, 0);
    public static final Vector3f VECTOR3F_ZERO = new Vector3f();

    public static final Codec<BedrockGeometry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BedrockVersion.STRING_CODEC.fieldOf("format_version").forGetter(BedrockGeometry::formatVersion),
                    GeometryDefinition.CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("minecraft:geometry").forGetter(BedrockGeometry::definitions)
            ).apply(instance, BedrockGeometry::new)
    );

    public void save(Path geometryDirectory) throws IOException {
        CodecUtil.trySaveJson(CODEC, this, geometryDirectory.resolve(definitions.getFirst().info.identifier + ".geo.json"));
    }

    public static BedrockGeometry of(GeometryDefinition... definitions) {
        return new BedrockGeometry(FORMAT_VERSION, Arrays.asList(definitions));
    }

    public static Builder builder(String identifier) {
        return new Builder(identifier);
    }

    public static Bone.Builder bone(String name) {
        return new Bone.Builder(name);
    }

    public static Cube.Builder cube(Vector3f origin, Vector3f size) {
        return new Cube.Builder(origin, size);
    }

    public static class Builder {
        private final String identifier;
        private final List<Bone> bones = new ArrayList<>();

        private Optional<Float> visibleBoundsWidth = Optional.empty();
        private Optional<Float> visibleBoundsHeight = Optional.empty();
        private Optional<Vector3f> visibleBoundsOffset = Optional.empty();
        private Optional<Integer> textureWidth = Optional.empty();
        private Optional<Integer> textureHeight = Optional.empty();

        public Builder(String identifier) {
            this.identifier = "geometry." + identifier;
        }

        public Builder withVisibleBoundsWidth(float visibleBoundsWidth) {
            this.visibleBoundsWidth = Optional.of(visibleBoundsWidth);
            return this;
        }

        public Builder withVisibleBoundsHeight(float visibleBoundsHeight) {
            this.visibleBoundsHeight = Optional.of(visibleBoundsHeight);
            return this;
        }

        public Builder withVisibleBoundsOffset(Vector3f visibleBoundsOffset) {
            this.visibleBoundsOffset = Optional.of(visibleBoundsOffset);
            return this;
        }

        public Builder withTextureWidth(int textureWidth) {
            this.textureWidth = Optional.of(textureWidth);
            return this;
        }

        public Builder withTextureHeight(int textureHeight) {
            this.textureHeight = Optional.of(textureHeight);
            return this;
        }

        public Builder withBone(Bone bone) {
            if (bones.stream().anyMatch(existing -> existing.name.equals(bone.name))) {
                throw new IllegalArgumentException("Duplicate bone with name " + bone.name);
            }
            bones.add(bone);
            return this;
        }

        public Builder withBone(Bone.Builder builder) {
            return withBone(builder.build());
        }

        public BedrockGeometry build() {
            return BedrockGeometry.of(new GeometryDefinition(
                    new GeometryInfo(identifier, visibleBoundsWidth, visibleBoundsHeight, visibleBoundsOffset, textureWidth, textureHeight), List.copyOf(bones)));
        }
    }

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

        public static class Builder {
            private final String name;
            private final List<Cube> cubes = new ArrayList<>();

            private Optional<String> parent = Optional.empty();
            private Vector3f pivot = VECTOR3F_ZERO;
            private Vector3f rotation = VECTOR3F_ZERO;
            private boolean mirror = false;
            private float inflate = 0.0F;

            public Builder(String name) {
                this.name = name;
            }

            public Builder withParent(String parent) {
                this.parent = Optional.of(parent);
                return this;
            }

            public Builder withPivot(Vector3f pivot) {
                this.pivot = pivot;
                return this;
            }

            public Builder withRotation(Vector3f rotation) {
                this.rotation = rotation;
                return this;
            }

            public Builder mirror() {
                this.mirror = true;
                return this;
            }

            public Builder withInflate(float inflate) {
                this.inflate = inflate;
                return this;
            }

            public Builder withCube(Cube cube) {
                this.cubes.add(cube);
                return this;
            }

            public Builder withCube(Cube.Builder builder) {
                return withCube(builder.build());
            }

            public Bone build() {
                return new Bone(name, parent, pivot, rotation, mirror, inflate, List.copyOf(cubes));
            }
        }
    }

    public record Cube(Vector3f origin, Vector3f size, Vector3f rotation, Vector3f pivot, float inflate, boolean mirror,
                       Map<Direction, Face> faces) {
        private static final Codec<Map<Direction, Face>> FACE_MAP_CODEC = Codec.unboundedMap(Direction.CODEC, Face.CODEC);
        public static final Codec<Cube> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ExtraCodecs.VECTOR3F.fieldOf("origin").forGetter(Cube::origin),
                        ExtraCodecs.VECTOR3F.fieldOf("size").forGetter(Cube::size),
                        ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", VECTOR3F_ZERO).forGetter(Cube::rotation),
                        ExtraCodecs.VECTOR3F.optionalFieldOf("pivot", VECTOR3F_ZERO).forGetter(Cube::pivot),
                        Codec.FLOAT.optionalFieldOf("inflate", 0.0F).forGetter(Cube::inflate),
                        Codec.BOOL.optionalFieldOf("mirror", false).forGetter(Cube::mirror),
                        FACE_MAP_CODEC.optionalFieldOf("uv", Map.of()).forGetter(Cube::faces)
                ).apply(instance, Cube::new)
        );

        public static class Builder {
            private final Vector3f origin;
            private final Vector3f size;
            private final Map<Direction, Face> faces = new HashMap<>();

            private Vector3f rotation = VECTOR3F_ZERO;
            private Vector3f pivot = VECTOR3F_ZERO;
            private float inflate = 0.0F;
            private boolean mirror = false;

            public Builder(Vector3f origin, Vector3f size) {
                this.origin = origin;
                this.size = size;
            }

            public Builder withRotation(Vector3f rotation) {
                this.rotation = rotation;
                return this;
            }

            public Builder withPivot(Vector3f pivot) {
                this.pivot = pivot;
                return this;
            }

            public Builder withInflate(float inflate) {
                this.inflate = inflate;
                return this;
            }

            public Builder mirror() {
                this.mirror = true;
                return this;
            }

            public Builder withFace(Direction direction, Vector2f uvOrigin, Vector2f uvSize, Quadrant uvRotation) {
                if (faces.containsKey(direction)) {
                    throw new IllegalArgumentException("Already added a face for direction " + direction);
                }
                faces.put(direction, new Face(uvOrigin, uvSize, uvRotation));
                return this;
            }

            public Builder withFace(Direction direction, Vector2f uvOrigin, Vector2f uvSize) {
                return withFace(direction, uvOrigin, uvSize, Quadrant.R0);
            }

            public Cube build() {
                return new Cube(origin, size, rotation, pivot, inflate, mirror, Map.copyOf(faces));
            }
        }
    }

    public record Face(Vector2f uvOrigin, Vector2f uvSize, Quadrant uvRotation) {
        public static final Codec<Face> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ExtraCodecs.VECTOR2F.fieldOf("uv").forGetter(Face::uvOrigin),
                        ExtraCodecs.VECTOR2F.fieldOf("uv_size").forGetter(Face::uvSize),
                        Quadrant.CODEC.optionalFieldOf("uv_rotation", Quadrant.R0).forGetter(Face::uvRotation)
                ).apply(instance, Face::new)
        );
    }
}
