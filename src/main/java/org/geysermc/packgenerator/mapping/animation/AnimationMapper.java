package org.geysermc.packgenerator.mapping.animation;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import org.geysermc.packgenerator.pack.animation.BedrockAnimation;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class AnimationMapper {
    private static final Vector3fc POSITION_OFFSET = new Vector3f(0.0F, 13.0F, -3.0F);
    private static final Vector3fc ROTATION_OFFSET = new Vector3f(90.0F, -90.0F, 0.0F);

    public static BedrockAnimationContext mapAnimation(String identifier, String bone, ItemTransforms transforms) {
        ItemTransform thirdPerson = transforms.thirdPersonLeftHand();
        Vector3f thirdPersonPosition = POSITION_OFFSET.add(thirdPerson.translation(), new Vector3f());
        Vector3f thirdPersonRotation = ROTATION_OFFSET.add(-thirdPerson.rotation().x(), thirdPerson.rotation().y(), thirdPerson.rotation().z(), new Vector3f());
        Vector3f thirdPersonScale = new Vector3f(thirdPerson.scale());

        return new BedrockAnimationContext(BedrockAnimation.builder()
                .withAnimation(identifier + ".hold_first_person", BedrockAnimation.animation())
                .withAnimation(identifier + ".hold_third_person", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, thirdPersonPosition, thirdPersonRotation, thirdPersonScale))
                .build(), "animation." + identifier + ".hold_first_person", "animation." + identifier + ".hold_third_person");
    }
}
