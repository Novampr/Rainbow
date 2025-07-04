package org.geysermc.packgenerator.mapping.geometry;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.core.Direction;
import org.geysermc.packgenerator.pack.geometry.BedrockGeometry;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Map;

public class GeometryMapper {

    public static BedrockGeometry mapGeometry(String identifier, SimpleUnbakedGeometry geometry) {
        BedrockGeometry.Builder builder = BedrockGeometry.builder(identifier);
        BedrockGeometry.Bone.Builder bone = BedrockGeometry.bone(identifier);

        for (BlockElement element : geometry.elements()) {
            bone.withCube(mapBlockElement(element));
        }

        // TODO texture size, etc.
        return builder.withBone(bone).build();
    }

    private static BedrockGeometry.Cube.Builder mapBlockElement(BlockElement element) {
        BedrockGeometry.Cube.Builder builder = BedrockGeometry.cube(new Vector3f(element.from()), element.to().sub(element.from(), new Vector3f()));

        for (Map.Entry<Direction, BlockElementFace> faceEntry : element.faces().entrySet()) {
            // TODO texture key
            Direction direction = faceEntry.getKey();
            BlockElementFace face = faceEntry.getValue();

            Vector2f uvOrigin;
            Vector2f uvSize;
            BlockElementFace.UVs uvs = face.uvs();
            if (uvs != null) {
                uvOrigin = new Vector2f(uvs.minU(), uvs.minV());
                uvSize = new Vector2f(uvs.maxU() - uvs.minU(), uvs.maxV() - uvs.minV());
            } else {
                uvOrigin = new Vector2f();
                uvSize = new Vector2f();
            }

            builder.withFace(direction, uvOrigin, uvSize, face.rotation());
        }

        BlockElementRotation rotation = element.rotation();
        if (rotation != null) {
            builder.withPivot(rotation.origin());

            Vector3f bedrockRotation = switch (rotation.axis()) {
                case X -> new Vector3f(rotation.angle(), 0.0F, 0.0F);
                case Y -> new Vector3f(0.0F, rotation.angle(), 0.0F);
                case Z -> new Vector3f(0.0F, 0.0F, rotation.angle());
            };
            builder.withRotation(bedrockRotation);
        }

        return builder;
    }
}
