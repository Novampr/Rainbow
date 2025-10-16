package org.geysermc.rainbow.mapping.geometry;

import com.google.common.base.Suppliers;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.animation.AnimationMapper;
import org.geysermc.rainbow.mapping.animation.BedrockAnimationContext;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record BedrockGeometryContext(Optional<Supplier<StitchedGeometry>> geometry,
                                     Optional<BedrockAnimationContext> animation, TextureHolder icon,
                                     boolean handheld) {
    private static final List<ResourceLocation> HANDHELD_MODELS = Stream.of("item/handheld", "item/handheld_rod", "item/handheld_mace")
            .map(ResourceLocation::withDefaultNamespace)
            .toList();

    public static BedrockGeometryContext create(ResourceLocation bedrockIdentifier, ItemStack stackToRender, ResolvedModel model, PackContext context) {
        ResolvedModel parentModel = model.parent();
        // debugName() returns the resource location of the model as a string
        boolean handheld = parentModel != null && HANDHELD_MODELS.contains(ResourceLocation.parse(parentModel.debugName()));

        TextureSlots textures = model.getTopTextureSlots();
        Material layer0Texture = textures.getMaterial("layer0");
        Optional<Supplier<StitchedGeometry>> geometry;
        Optional<BedrockAnimationContext> animation;
        TextureHolder icon;

        if (layer0Texture != null) {
            geometry = Optional.empty();
            animation = Optional.empty();
            icon = new TextureHolder(layer0Texture.texture());
        } else {
            // Unknown model (doesn't use layer0), so we immediately assume the geometry is custom
            // This check should probably be done differently (actually check if the model is 2D or 3D)

            ResourceLocation modelLocation = ResourceLocation.parse(model.debugName());
            String safeIdentifier = Rainbow.fileSafeResourceLocation(bedrockIdentifier);

            geometry = Optional.of(Suppliers.memoize(() -> {
                StitchedTextures stitchedTextures = StitchedTextures.stitchModelTextures(textures, context);
                BedrockGeometry mappedGeometry = GeometryMapper.mapGeometry(safeIdentifier, "bone", model, stitchedTextures);
                return new StitchedGeometry(mappedGeometry, new TextureHolder(modelLocation.withSuffix("_stitched"), stitchedTextures.stitched()));
            }));

            animation = Optional.of(AnimationMapper.mapAnimation(safeIdentifier, "bone", model.getTopTransforms()));
            icon = new TextureHolder(modelLocation, context.geometryRenderer().map(renderer -> () -> renderer.render(stackToRender)));
        }

        return new BedrockGeometryContext(geometry, animation, icon, handheld);
    }

    public record StitchedGeometry(BedrockGeometry geometry, TextureHolder stitchedTextures) {}
}
