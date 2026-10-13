package dev.skyjet.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.skyjet.SkyJet;

//? if >=26.2 {
import com.mojang.blaze3d.GpuFormat;
//?} else {
/*import com.mojang.blaze3d.textures.TextureFormat;
*///?}

/**
 * Owns the tiling noise lookup the sky shader samples. Created once and reused for every frame,
 * including the configuration screen preview.
 */
public final class SkyNoise {
    public static final String SAMPLER = "NoiseSampler";

    private static GpuTexture texture;
    private static GpuTextureView view;
    private static GpuSampler sampler;

    private SkyNoise() {
    }

    public static boolean ensure() {
        if (view != null) return true;
        try {
            var device = RenderSystem.getDevice();
            int size = NoiseTextureData.SIZE;
            //? if >=26.2 {
            texture = device.createTexture("SkyJet noise", GpuTexture.USAGE_TEXTURE_BINDING
                    | GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM, size, size, 1, 1);
            //?} else {
            /*texture = device.createTexture("SkyJet noise", GpuTexture.USAGE_TEXTURE_BINDING
                    | GpuTexture.USAGE_COPY_DST, TextureFormat.RGBA8, size, size, 1, 1);
            *///?}
            view = device.createTextureView(texture);
            sampler = RenderSystem.getSamplerCache().getSampler(
                    AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, false);

            int[] pixels = NoiseTextureData.generate();
            try (NativeImage image = new NativeImage(size, size, false)) {
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        image.setPixelABGR(x, y, pixels[y * size + x]);
                    }
                }
                device.createCommandEncoder().writeToTexture(texture, image);
            }
            return true;
        } catch (Exception e) {
            SkyJet.LOG.error("Failed to create the SkyJet noise texture", e);
            close();
            return false;
        }
    }

    public static GpuTextureView view() {
        return view;
    }

    public static GpuSampler sampler() {
        return sampler;
    }

    public static void close() {
        if (texture != null) {
            try {
                texture.close();
            } catch (Exception ignored) {
                // Already released with the device.
            }
        }
        texture = null;
        view = null;
        sampler = null;
    }
}
