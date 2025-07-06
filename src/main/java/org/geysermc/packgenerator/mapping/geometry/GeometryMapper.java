package org.geysermc.packgenerator.mapping.geometry;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.packgenerator.pack.geometry.BedrockGeometry;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Map;

public class GeometryMapper {
    private static final Vector3fc CENTRE_OFFSET = new Vector3f(8.0F, 0.0F, 8.0F);

    public static BedrockGeometryContext mapGeometry(String identifier, String boneName, ResolvedModel model, ResourceLocation texture) {
        BedrockGeometry.Builder builder = BedrockGeometry.builder(identifier);
        // Blockbench seems to always use these values
        builder.withVisibleBoundsWidth(2.0F);
        builder.withVisibleBoundsHeight(2.5F);
        builder.withVisibleBoundsOffset(new Vector3f(0.0F, 0.75F, 0.0F));

        // TODO proper texture size
        builder.withTextureWidth(16);
        builder.withTextureHeight(16);

        BedrockGeometry.Bone.Builder bone = BedrockGeometry.bone(boneName);

        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(Float.MIN_VALUE);

        SimpleUnbakedGeometry geometry = (SimpleUnbakedGeometry) model.getTopGeometry();
        for (BlockElement element : geometry.elements()) {
            BedrockGeometry.Cube cube = mapBlockElement(element).build();
            bone.withCube(cube);
            min.min(cube.origin());
            max.max(cube.origin().add(cube.size(), new Vector3f()));
        }

        // Calculate the pivot to be at the centre of the bone
        // This is important for animations later, display animations rotate around the centre on Java
        bone.withPivot(min.add(max.sub(min).div(2.0F)));

        // Bind to the bone of the current item slot
        bone.withBinding("q.item_slot_to_bone_name(context.item_slot)");
        return new BedrockGeometryContext(builder.withBone(bone).build(), texture);
    }

    private static BedrockGeometry.Cube.Builder mapBlockElement(BlockElement element) {
        // The centre of the model is back by 8 in the X and Z direction on Java, so move the origin of the cube and the pivot like that
        BedrockGeometry.Cube.Builder builder = BedrockGeometry.cube(element.from().sub(CENTRE_OFFSET, new Vector3f()), element.to().sub(element.from(), new Vector3f()));

        for (Map.Entry<Direction, BlockElementFace> faceEntry : element.faces().entrySet()) {
            // TODO texture key
            Direction direction = faceEntry.getKey();
            BlockElementFace face = faceEntry.getValue();

            Vector2f uvOrigin;
            Vector2f uvSize;
            BlockElementFace.UVs uvs = face.uvs();
            if (uvs != null) {
                // Up and down faces are special
                if (direction.getAxis() == Direction.Axis.Y) {
                    uvOrigin = new Vector2f(uvs.maxU(), uvs.maxV());
                    uvSize = new Vector2f(uvs.minU() - uvs.maxU(), uvs.minV() - uvs.maxV());
                } else {
                    uvOrigin = new Vector2f(uvs.minU(), uvs.minV());
                    uvSize = new Vector2f(uvs.maxU() - uvs.minU(), uvs.maxV() - uvs.minV());
                }
            } else {
                uvOrigin = new Vector2f();
                uvSize = new Vector2f();
            }

            builder.withFace(direction, uvOrigin, uvSize, face.rotation());
        }

        BlockElementRotation rotation = element.rotation();
        if (rotation != null) {
            builder.withPivot(rotation.origin().sub(CENTRE_OFFSET, new Vector3f()));

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
