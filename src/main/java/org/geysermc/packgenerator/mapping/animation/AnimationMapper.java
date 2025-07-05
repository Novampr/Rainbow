package org.geysermc.packgenerator.mapping.animation;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import org.geysermc.packgenerator.pack.animation.BedrockAnimation;
import org.joml.Vector3f;

public class AnimationMapper {

    public static BedrockAnimationContext mapAnimation(String identifier, String bone, ItemTransforms transforms) {
        return new BedrockAnimationContext(BedrockAnimation.builder()
                .withAnimation(identifier + ".hold_first_person", BedrockAnimation.animation())
                .withAnimation(identifier + ".hold_third_person", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, new Vector3f(0.0F, 40.0F, 0.0F), new Vector3f(), new Vector3f()))
                .build(), "animation." + identifier + ".hold_first_person", "animation." + identifier + ".hold_third_person");
    }
}
