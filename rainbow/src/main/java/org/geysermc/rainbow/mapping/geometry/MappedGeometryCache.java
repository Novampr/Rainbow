package org.geysermc.rainbow.mapping.geometry;

import com.google.common.base.Suppliers;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.texture.StitchedTextures;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.util.HashMap;
import java.util.Map;

public class MappedGeometryCache {
    private final Map<GeometryCacheKey, MappedGeometryInstance> cachedGeometry = new HashMap<>();

    public MappedGeometry mapGeometry(ResourceLocation bedrockIdentifier, ResolvedModel model, ItemStack stackToRender, PackContext context) {
        GeometryCacheKey cacheKey = new GeometryCacheKey(model);
        MappedGeometry cached = cachedGeometry.get(cacheKey);
        if (cached != null) {
            return cached.cachedCopy();
        }

        ResourceLocation modelLocation = ResourceLocation.parse(model.debugName());
        ResourceLocation stitchedTexturesLocation = modelLocation.withSuffix("_stitched");
        String safeIdentifier = Rainbow.safeResourceLocation(bedrockIdentifier);

        StitchedTextures stitchedTextures = StitchedTextures.stitchModelTextures(model.getTopTextureSlots(), context);
        BedrockGeometry geometry = GeometryMapper.mapGeometry(safeIdentifier, "bone", model, stitchedTextures);
        TextureHolder icon = context.geometryRenderer().isPresent() ? context.geometryRenderer().orElseThrow().render(modelLocation, stackToRender)
                                                                    : TextureHolder.createNonExistent(modelLocation);
        MappedGeometryInstance instance = new MappedGeometryInstance(geometry, TextureHolder.createCustom(stitchedTexturesLocation, stitchedTextures.stitched()), icon);
        cachedGeometry.put(cacheKey, instance);
        return instance;
    }

    private record GeometryCacheKey(UnbakedGeometry geometry, Map<String, Material> textures) {

        private GeometryCacheKey(ResolvedModel model) {
            this(model.getTopGeometry(), ((TextureSlotsAccessor) model.getTopTextureSlots()).getResolvedValues());
        }
    }
}
