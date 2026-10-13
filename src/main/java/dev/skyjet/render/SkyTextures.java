package dev.skyjet.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.skyjet.SkyJet;
import dev.skyjet.config.SkyboxLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

//? if >=26.2 {
import com.mojang.blaze3d.GpuFormat;
//?} else {
/*import com.mojang.blaze3d.textures.TextureFormat;
*///?}

/**
 * Resolves the textures behind {@link SkyboxLayer}, so SkyJet can wear the art from an existing
 * sky resource pack instead of only drawing its own procedural layers.
 *
 * <p>A cube sheet is a 3:2 image holding all six faces, which is the layout both OptiFine custom
 * skies and FabricSkyBoxes single-texture skyboxes use. Those bind straight through with no work
 * at all. Six separate face images are stitched once into that same sheet, so the shader only
 * ever has one mapping to undo.
 *
 * <p>Face suffixes accepted when a layer names a prefix rather than six explicit files:
 * {@code _top/_bottom/_north/_south/_east/_west}, then {@code _0}..{@code _5}, then
 * {@code _up}/{@code _down}.
 */
public final class SkyTextures {
    /** A resolved layer: what to bind, and the texel size the shader needs for seam insetting. */
    public record Binding(GpuTextureView view, GpuSampler sampler, float texelU, float texelV) {
    }

    /**
     * Atlas cell order, matching {@code cubeSheetUv} in the fragment shader:
     * bottom, top, south on the top row; west, north, east on the bottom row.
     */
    private static final String[][] FACE_SUFFIXES = {
            {"_bottom", "_5", "_down"},
            {"_top", "_4", "_up"},
            {"_south", "_2", "_back"},
            {"_west", "_3", "_left"},
            {"_north", "_0", "_front"},
            {"_east", "_1", "_right"}
    };

    /** Same order as {@link #FACE_SUFFIXES}, for layers that name all six files explicitly. */
    public static final String[] FACE_KEYS = {"bottom", "top", "south", "west", "north", "east"};

    /** Only stitched sheets live here. Views owned by the texture manager are looked up fresh. */
    private static final Map<String, Binding> CACHE = new HashMap<>();
    private static final Map<String, GpuTexture> OWNED = new HashMap<>();
    /** Parsed identifiers, so resolving a binding every frame stays a map lookup. */
    private static final Map<String, Identifier> IDS = new HashMap<>();

    private SkyTextures() {
    }

    /** Drops every stitched atlas. Safe to call at any time; the next frame rebuilds what it needs. */
    public static void invalidate() {
        for (GpuTexture texture : OWNED.values()) {
            try {
                texture.close();
            } catch (Exception ignored) {
                // Already released with the device.
            }
        }
        OWNED.clear();
        CACHE.clear();
    }

    public static Binding resolve(SkyboxLayer layer) {
        if (layer == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;

        if (layer.mode != SkyboxLayer.MODE_CUBE_FACES) {
            return single(mc, layer.texture);
        }

        String key = layer.textureKey();
        Binding cached = CACHE.get(key);
        if (cached != null || CACHE.containsKey(key)) return cached;
        // Null is cached too: a missing pack should not retry six resource lookups every frame.
        Binding built = buildFaceSheet(mc, layer);
        CACHE.put(key, built);
        return built;
    }

    /** The mod's own mark, painted onto the bottom face of every SkyJet sky. */
    public static Binding watermark() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        return single(mc, SkyJet.MOD_ID + ":textures/watermark.png");
    }

    private static Identifier parse(String path) {
        if (path == null || path.isBlank()) return null;
        if (IDS.containsKey(path)) return IDS.get(path);
        Identifier id = parseUncached(path);
        IDS.put(path, id);
        return id;
    }

    private static Identifier parseUncached(String path) {
        try {
            String value = path.trim();
            if (!value.endsWith(".png")) {
                value = value + ".png";
            }
            return Identifier.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static Binding single(Minecraft mc, String path) {
        Identifier id = parse(path);
        if (id == null) return null;
        AbstractTexture texture = mc.getTextureManager().getTexture(id);
        if (texture == null || texture.getTextureView() == null) return null;
        // The texture manager hands back the missing-texture placeholder rather than failing, so
        // a typo shows as a checkerboard sky instead of a crash.
        float texelU = 0.0f;
        float texelV = 0.0f;
        GpuTexture gpu = texture.getTexture();
        if (gpu != null) {
            int w = gpu.getWidth(0);
            int h = gpu.getHeight(0);
            if (w > 0 && h > 0) {
                texelU = 0.5f / w;
                texelV = 0.5f / h;
            }
        }
        return new Binding(texture.getTextureView(), texture.getSampler(), texelU, texelV);
    }

    private static String faceSource(SkyboxLayer layer, int face) {
        if (layer.faceTextures != null && face < layer.faceTextures.length) {
            String explicit = layer.faceTextures[face];
            if (explicit != null && !explicit.isBlank()) return explicit;
        }
        return null;
    }

    private static Binding buildFaceSheet(Minecraft mc, SkyboxLayer layer) {
        String base = layer.texture == null ? "" : layer.texture.trim();
        if (base.endsWith(".png")) {
            base = base.substring(0, base.length() - 4);
        }

        NativeImage[] faces = new NativeImage[6];
        try {
            int cell = 0;
            for (int i = 0; i < 6; i++) {
                faces[i] = loadFace(mc, layer, base, i);
                if (faces[i] == null) {
                    SkyJet.LOG.warn("SkyJet skybox '{}' is missing the {} face", layer.name, FACE_KEYS[i]);
                    return null;
                }
                cell = Math.max(cell, Math.max(faces[i].getWidth(), faces[i].getHeight()));
            }
            cell = Math.max(16, Math.min(2048, cell));

            int width = cell * 3;
            int height = cell * 2;
            try (NativeImage atlas = new NativeImage(width, height, false)) {
                for (int i = 0; i < 6; i++) {
                    copyScaled(faces[i], atlas, (i % 3) * cell, (i / 3) * cell, cell);
                }
                GpuTexture texture = createTexture(layer.name, width, height);
                if (texture == null) return null;
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, atlas);
                GpuTextureView view = RenderSystem.getDevice().createTextureView(texture);
                GpuSampler sampler = RenderSystem.getSamplerCache().getSampler(
                        AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                        FilterMode.LINEAR, FilterMode.LINEAR, false);
                OWNED.put(layer.textureKey(), texture);
                return new Binding(view, sampler, 0.5f / width, 0.5f / height);
            }
        } catch (Exception e) {
            SkyJet.LOG.error("Failed to build the SkyJet skybox '{}'", layer.name, e);
            return null;
        } finally {
            for (NativeImage face : faces) {
                if (face != null) face.close();
            }
        }
    }

    private static GpuTexture createTexture(String label, int width, int height) {
        try {
            //? if >=26.2 {
            return RenderSystem.getDevice().createTexture("SkyJet skybox " + label,
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.RGBA8_UNORM, width, height, 1, 1);
            //?} else {
            /*return RenderSystem.getDevice().createTexture("SkyJet skybox " + label,
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    TextureFormat.RGBA8, width, height, 1, 1);
            *///?}
        } catch (Exception e) {
            SkyJet.LOG.error("Failed to allocate a SkyJet skybox texture", e);
            return null;
        }
    }

    private static NativeImage loadFace(Minecraft mc, SkyboxLayer layer, String base, int face) {
        String explicit = faceSource(layer, face);
        if (explicit != null) {
            NativeImage image = read(mc, parse(explicit));
            if (image != null) return image;
        }
        for (String suffix : FACE_SUFFIXES[face]) {
            NativeImage image = read(mc, parse(base + suffix));
            if (image != null) return image;
        }
        return null;
    }

    private static NativeImage read(Minecraft mc, Identifier id) {
        if (id == null) return null;
        Optional<net.minecraft.server.packs.resources.Resource> resource = mc.getResourceManager().getResource(id);
        if (resource.isEmpty()) return null;
        try (InputStream in = resource.get().open()) {
            return NativeImage.read(in);
        } catch (Exception e) {
            SkyJet.LOG.warn("Could not read SkyJet skybox face {}", id, e);
            return null;
        }
    }

    /** Blits one face into its atlas cell, rescaling first when the pack ships mismatched faces. */
    private static void copyScaled(NativeImage source, NativeImage target, int ox, int oy, int cell) {
        if (source.getWidth() == cell && source.getHeight() == cell) {
            source.copyRect(target, 0, 0, ox, oy, cell, cell, false, false);
            return;
        }
        try (NativeImage scaled = new NativeImage(cell, cell, false)) {
            source.resizeSubRectTo(0, 0, source.getWidth(), source.getHeight(), scaled);
            scaled.copyRect(target, 0, 0, ox, oy, cell, cell, false, false);
        }
    }
}
