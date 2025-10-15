package org.geysermc.rainbow.mapping.geometry;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.animation.AnimationMapper;
import org.geysermc.rainbow.mapping.animation.BedrockAnimationContext;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record BedrockGeometryContext(Optional<BedrockGeometry> geometry, Optional<BedrockAnimationContext> animation, TextureHolder texture,
                                     boolean handheld) {
    private static final List<ResourceLocation> HANDHELD_MODELS = Stream.of("item/handheld", "item/handheld_rod", "item/handheld_mace")
            .map(ResourceLocation::withDefaultNamespace)
            .toList();

    public static BedrockGeometryContext create(ResolvedModel model) {
        ResolvedModel parentModel = model.parent();
        // debugName() returns the resource location of the model as a string
        boolean handheld = parentModel != null && HANDHELD_MODELS.contains(ResourceLocation.parse(parentModel.debugName()));

        TextureSlots textures = model.getTopTextureSlots();
        Material layer0Texture = textures.getMaterial("layer0");
        Optional<BedrockGeometry> geometry;
        Optional<BedrockAnimationContext> animation;
        TextureHolder texture;

        if (layer0Texture != null) {
            geometry = Optional.empty();
            animation = Optional.empty();
            texture = new TextureHolder(layer0Texture.texture());
        } else {
            // Unknown model (doesn't use layer0), so we immediately assume the geometry is custom
            // This check should probably be done differently (actually check if the model is 2D or 3D)

            ResourceLocation modelLocation = ResourceLocation.parse(model.debugName());
            String safeIdentifier = Rainbow.fileSafeResourceLocation(modelLocation);
            
            StitchedTextures stitchedTextures = StitchedTextures.stitchModelTextures(textures);
            geometry = GeometryMapper.mapGeometry(safeIdentifier, "bone", model, stitchedTextures);
            animation = Optional.of(AnimationMapper.mapAnimation(safeIdentifier, "bone", model.getTopTransforms()));
            texture = new TextureHolder(modelLocation.withSuffix("_stitched"), Optional.of(stitchedTextures.stitched()));
            // TODO geometry rendering
        }

        return new BedrockGeometryContext(geometry, animation, texture, handheld);
    }
}
