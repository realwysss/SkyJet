package dev.skyjet.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.skyjet.SkyJet;
import dev.skyjet.config.SkyConfig;
import dev.skyjet.render.SkyJetRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

//? if >=26.2 {
import com.mojang.blaze3d.GpuFormat;
//?} else {
/*import com.mojang.blaze3d.textures.TextureFormat;
*///?}

/**
 * Off screen render target used by the configuration screen so the sky can be previewed live
 * without touching the world framebuffer.
 */
public final class SkyPreview implements AutoCloseable {
    public static final Identifier TEXTURE_ID = SkyJet.id("preview");

    private RenderTarget target;
    private Binding binding;
    private int width;
    private int height;
    private boolean broken;

    private static final class Binding extends AbstractTexture {
        Binding(RenderTarget target) {
            this.texture = target.getColorTexture();
            this.textureView = target.getColorTextureView();
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }

        @Override
        public void close() {
            // The backing textures belong to the render target.
        }
    }

    public boolean ensure(int requestedWidth, int requestedHeight) {
        if (broken) return false;
        int w = Math.max(16, requestedWidth);
        int h = Math.max(16, requestedHeight);
        if (target != null && w == width && h == height) return true;
        try {
            close();
            //? if >=26.2 {
            target = new TextureTarget("SkyJet preview", w, h, true, GpuFormat.RGBA8_UNORM);
            //?} else {
            /*target = new TextureTarget("SkyJet preview", w, h, true);
            *///?}
            width = w;
            height = h;
            binding = new Binding(target);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, binding);
            return true;
        } catch (Exception e) {
            broken = true;
            SkyJet.LOG.error("Unable to create the SkyJet preview target", e);
            return false;
        }
    }

    public void render(SkyConfig config, float yaw, float pitch, float fov, float time, float dayFraction) {
        if (target == null || broken) return;
        SkyJetRenderer.renderPreview(config, target.getColorTextureView(), target.getDepthTextureView(),
                width, height, yaw, pitch, fov, time, dayFraction);
    }

    public void draw(Gfx g, int x, int y, int w, int h) {
        if (target == null || broken) return;
        g.blit(TEXTURE_ID, x, y, w, h, width, height);
    }

    public boolean isReady() {
        return target != null && !broken;
    }

    @Override
    public void close() {
        if (binding != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            binding = null;
        }
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        width = 0;
        height = 0;
    }
}
