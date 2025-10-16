package org.geysermc.rainbow.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.OversizedItemRenderer;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.client.mixin.PictureInPictureRendererAccessor;
import org.joml.Matrix3x2fStack;

import java.util.Objects;

// TODO maybe just use this even for normal 2D items, not sure, could be useful for composite models and stuff
// TODO output in a size bedrock likes
public class MinecraftGeometryRenderer implements GeometryRenderer {
    public static final MinecraftGeometryRenderer INSTANCE = new MinecraftGeometryRenderer();

    @Override
    public NativeImage render(ItemStack stack) {
        TrackingItemStackRenderState itemRenderState = new TrackingItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(itemRenderState, stack, ItemDisplayContext.GUI, null, null, 0);
        itemRenderState.setOversizedInGui(true);

        GuiItemRenderState guiItemRenderState = new GuiItemRenderState("geometry_render", new Matrix3x2fStack(16), itemRenderState, 0, 0, null);
        ScreenRectangle sizeBounds = guiItemRenderState.oversizedItemBounds();
        Objects.requireNonNull(sizeBounds);
        OversizedItemRenderState oversizedRenderState = new OversizedItemRenderState(guiItemRenderState, sizeBounds.left(), sizeBounds.top(), sizeBounds.right() + 4, sizeBounds.bottom() + 4);

        try (OversizedItemRenderer itemRenderer = new OversizedItemRenderer(Minecraft.getInstance().renderBuffers().bufferSource())) {
            //noinspection DataFlowIssue
            ((PictureInPictureCopyRenderer) itemRenderer).rainbow$allowTextureCopy();
            itemRenderer.prepare(oversizedRenderState, new GuiRenderState(), 4);
            return writeToImage(((PictureInPictureRendererAccessor) itemRenderer).getTexture());
        }
    }

    // Simplified TextureUtil#writeAsPNG with some modifications to just write to a NativeImage, flip the image and just generate it at full size
    private static NativeImage writeToImage(GpuTexture texture) {
        RenderSystem.assertOnRenderThread();
        int width = texture.getWidth(0);
        int height = texture.getHeight(0);
        int bufferSize = texture.getFormat().pixelSize() * width * height;

        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        NativeImage image = new NativeImage(width, height, false);
        Runnable writer = () -> {
            try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(buffer, true, false)) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int colour = mappedView.data().getInt((x + y * width) * texture.getFormat().pixelSize());
                        image.setPixelABGR(x, height - y - 1, colour);
                    }
                }
            }

            buffer.close();
        };
        commandEncoder.copyTextureToBuffer(texture, buffer, 0, writer, 0);

        return image;
    }
}
