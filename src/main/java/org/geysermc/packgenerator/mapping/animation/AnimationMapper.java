package org.geysermc.packgenerator.mapping.animation;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import org.geysermc.packgenerator.pack.animation.BedrockAnimation;
import org.joml.Vector3f;
import org.joml.Vector3fc;

// TODO these offset values are completely wrong, I think
public class AnimationMapper {
    // These aren't perfect... but I spent over 1.5 hours trying to get these. It's good enough for me.
    private static final Vector3fc FIRST_PERSON_POSITION_OFFSET = new Vector3f(-7.0F, 22.5F, -7.0F);
    private static final Vector3fc FIRST_PERSON_ROTATION_OFFSET = new Vector3f(-22.5F, 50.0F, -32.5F);

    private static final Vector3fc THIRD_PERSON_POSITION_OFFSET = new Vector3f(0.0F, 13.0F, -3.0F);
    private static final Vector3fc THIRD_PERSON_ROTATION_OFFSET = new Vector3f(90.0F, -90.0F, 0.0F);

    public static BedrockAnimationContext mapAnimation(String identifier, String bone, ItemTransforms transforms) {
        // I don't think it's possible to display separate animations for left- and right hands
        ItemTransform firstPerson = transforms.firstPersonRightHand();
        Vector3f firstPersonPosition = FIRST_PERSON_POSITION_OFFSET.add(firstPerson.translation(), new Vector3f());
        Vector3f firstPersonRotation = FIRST_PERSON_ROTATION_OFFSET.add(firstPerson.rotation(), new Vector3f());
        Vector3f firstPersonScale = new Vector3f(firstPerson.scale());

        ItemTransform thirdPerson = transforms.thirdPersonRightHand();
        Vector3f thirdPersonPosition = THIRD_PERSON_POSITION_OFFSET.add(thirdPerson.translation(), new Vector3f());
        Vector3f thirdPersonRotation = THIRD_PERSON_ROTATION_OFFSET.add(-thirdPerson.rotation().x(), thirdPerson.rotation().y(), thirdPerson.rotation().z(), new Vector3f());
        Vector3f thirdPersonScale = new Vector3f(thirdPerson.scale());

        return new BedrockAnimationContext(BedrockAnimation.builder()
                .withAnimation(identifier + ".hold_first_person", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, firstPersonPosition, firstPersonRotation, firstPersonScale))
                .withAnimation(identifier + ".hold_third_person", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, thirdPersonPosition, thirdPersonRotation, thirdPersonScale))
                .build(), "animation." + identifier + ".hold_first_person", "animation." + identifier + ".hold_third_person");
    }
}
