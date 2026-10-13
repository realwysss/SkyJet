package dev.skyjet.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.skyjet.SkyJet;
import dev.skyjet.config.SkyConfig;
import dev.skyjet.config.SkyboxLayer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

//? if >=26.2 {
import org.joml.Vector4fc;
//?}

/**
 * Draws the procedural SkyJet sky as a single full screen triangle.
 */
public final class SkyJetRenderer {
    private static final int FLOATS_PER_VERTEX = 3;
    private static final int VERTEX_COUNT = 3;

    private static GpuBuffer vertexBuffer;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer previewProjection;
    private static GpuBuffer previewFog;

    private static final SkyUniforms UNIFORMS = new SkyUniforms();
    private static final SkyState WORLD_STATE = new SkyState();
    private static final SkyState PREVIEW_STATE = new SkyState();
    private static final ByteBuffer SCRATCH =
            ByteBuffer.allocateDirect(SkyUniforms.SIZE_BYTES).order(ByteOrder.nativeOrder());
    // Reused every frame. Direct buffers are expensive to allocate and only the garbage collector
    // frees them, so churning one per frame is a slow leak dressed up as cleanliness.
    private static final ByteBuffer PROJECTION_SCRATCH =
            ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());

    // Packed uniform slot N is sampled by Skybox<N>Sampler, so the resolved layers are compacted
    // into consecutive slots. Indexing by config position instead would point a slot at the
    // sampler of a layer that failed to load.
    private static final SkyTextures.Binding[] SKYBOX_BINDINGS =
            new SkyTextures.Binding[SkyConfig.MAX_SKYBOXES];
    private static final SkyboxLayer[] SKYBOX_LAYERS = new SkyboxLayer[SkyConfig.MAX_SKYBOXES];
    private static int skyboxCount;
    private static SkyTextures.Binding watermarkBinding;

    private static final Matrix4f PREVIEW_PROJECTION = new Matrix4f();
    private static final Matrix4f PREVIEW_VIEW = new Matrix4f();
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static final Vector4f WHITE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector3f ZERO = new Vector3f();

    private static boolean failed;

    private SkyJetRenderer() {
    }

    public static boolean isFailed() {
        return failed;
    }

    public static void reset() {
        close();
        SkyPipelines.invalidate();
        failed = false;
    }

    public static void close() {
        SkyNoise.close();
        SkyTextures.invalidate();
        vertexBuffer = closeBuffer(vertexBuffer);
        uniformBuffer = closeBuffer(uniformBuffer);
        previewProjection = closeBuffer(previewProjection);
        previewFog = closeBuffer(previewFog);
    }

    private static GpuBuffer closeBuffer(GpuBuffer buffer) {
        if (buffer != null) {
            try {
                buffer.close();
            } catch (Exception ignored) {
                // Buffer already released with the device.
            }
        }
        return null;
    }

    private static void ensureResources(GpuDevice device) {
        SkyNoise.ensure();
        if (vertexBuffer == null) {
            ByteBuffer verts = ByteBuffer.allocateDirect(VERTEX_COUNT * FLOATS_PER_VERTEX * Float.BYTES)
                    .order(ByteOrder.nativeOrder());
            // Oversized triangle covering the whole normalised device space.
            putVertex(verts, -1.0f, -1.0f);
            putVertex(verts, 3.0f, -1.0f);
            putVertex(verts, -1.0f, 3.0f);
            verts.flip();
            vertexBuffer = device.createBuffer(() -> "SkyJet fullscreen triangle", GpuBuffer.USAGE_VERTEX, verts);
        }
        if (uniformBuffer == null) {
            uniformBuffer = device.createBuffer(() -> "SkyJet uniforms",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, SkyUniforms.SIZE_BYTES);
        }
    }

    private static void putVertex(ByteBuffer buffer, float x, float y) {
        buffer.putFloat(x).putFloat(y).putFloat(0.0f);
    }

    /**
     * Resolves this frame's textures, dropping skybox layers that are off or failed to load.
     * Runs before the render pass opens: the texture manager uploads on first use, and an upload
     * issued while a pass is recording is rejected outright on some versions.
     */
    private static void resolveTextures(SkyConfig cfg) {
        watermarkBinding = SkyTextures.watermark();
        skyboxCount = 0;
        for (SkyboxLayer layer : cfg.skyboxes) {
            if (skyboxCount >= SKYBOX_BINDINGS.length) break;
            if (!layer.enabled) continue;
            SkyTextures.Binding binding = SkyTextures.resolve(layer);
            if (binding == null) continue;
            SKYBOX_LAYERS[skyboxCount] = layer;
            SKYBOX_BINDINGS[skyboxCount] = binding;
            skyboxCount++;
        }
        for (int i = skyboxCount; i < SKYBOX_BINDINGS.length; i++) {
            SKYBOX_LAYERS[i] = null;
            SKYBOX_BINDINGS[i] = null;
        }
    }

    /**
     * Every declared sampler has to be bound whether or not the sky uses it, so unused skybox
     * slots fall back to the noise texture rather than leaving a dangling binding.
     */
    private static void bindTextures(RenderPass pass) {
        pass.bindTexture(SkyNoise.SAMPLER, SkyNoise.view(), SkyNoise.sampler());
        SkyTextures.Binding mark = watermarkBinding;
        if (mark != null) {
            pass.bindTexture(SkyPipelines.WATERMARK_SAMPLER, mark.view(), mark.sampler());
        } else {
            pass.bindTexture(SkyPipelines.WATERMARK_SAMPLER, SkyNoise.view(), SkyNoise.sampler());
        }
        for (int i = 0; i < SkyPipelines.SKYBOX_SAMPLERS.length; i++) {
            SkyTextures.Binding binding = i < SKYBOX_BINDINGS.length ? SKYBOX_BINDINGS[i] : null;
            if (binding != null) {
                pass.bindTexture(SkyPipelines.SKYBOX_SAMPLERS[i], binding.view(), binding.sampler());
            } else {
                pass.bindTexture(SkyPipelines.SKYBOX_SAMPLERS[i], SkyNoise.view(), SkyNoise.sampler());
            }
        }
    }

    // ------------------------------------------------------------ world pass

    public static void renderWorldSky(GpuTextureView color, GpuTextureView depth) {
        if (failed) return;
        SkyConfig cfg = SkyJetRuntime.config();
        Minecraft mc = Minecraft.getInstance();
        double camX = 0.0, camY = 0.0, camZ = 0.0;
        if (mc.gameRenderer != null) {
            var camera = SkyCompat.mainCamera(mc);
            if (camera != null && camera.position() != null) {
                camX = camera.position().x;
                camY = camera.position().y;
                camZ = camera.position().z;
            }
        }
        boolean paused = SkyCompat.currentScreen(mc) != null;
        WORLD_STATE.update(cfg, camX, camY, camZ, paused);

        try {
            GpuDevice device = RenderSystem.getDevice();
            ensureResources(device);
            resolveTextures(cfg);
            UNIFORMS.pack(cfg, WORLD_STATE, SKYBOX_LAYERS, SKYBOX_BINDINGS, skyboxCount);
            UNIFORMS.writeTo(SCRATCH);
            CommandEncoder encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(), SCRATCH);

            Matrix4f modelView = SkyCompat.modelViewMatrix();
            var transforms = RenderSystem.getDynamicUniforms()
                    .writeTransform(modelView, WHITE, ZERO, IDENTITY);

            //? if >=26.2 {
            try (RenderPass pass = encoder.createRenderPass(() -> "SkyJet sky", color,
                    Optional.<Vector4fc>empty(), depth, OptionalDouble.empty())) {
            //?} else {
            /*try (RenderPass pass = encoder.createRenderPass(() -> "SkyJet sky", color,
                    OptionalInt.empty(), depth, OptionalDouble.empty())) {
            *///?}
                pass.setPipeline(SkyPipelines.sky());
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setUniform(SkyPipelines.UNIFORM_BLOCK, uniformBuffer.slice());
                bindTextures(pass);
                SkyCompat.bindVertices(pass, vertexBuffer);
                SkyCompat.drawTriangle(pass);
            }
        } catch (Exception e) {
            failed = true;
            SkyJet.LOG.error("SkyJet sky rendering failed and has been disabled for this session", e);
        }
    }

    // ---------------------------------------------------------- preview pass

    public static void renderPreview(SkyConfig cfg, GpuTextureView color, GpuTextureView depth,
                                     int width, int height, float yaw, float pitch, float fovDegrees,
                                     float time, float dayFraction) {
        if (failed) return;
        try {
            GpuDevice device = RenderSystem.getDevice();
            ensureResources(device);

            PREVIEW_STATE.updateForPreview(cfg, time, dayFraction);
            resolveTextures(cfg);
            UNIFORMS.pack(cfg, PREVIEW_STATE, SKYBOX_LAYERS, SKYBOX_BINDINGS, skyboxCount);
            UNIFORMS.writeTo(SCRATCH);

            CommandEncoder encoder = device.createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(), SCRATCH);

            float aspect = height <= 0 ? 1.0f : (float) width / (float) height;
            // fovDegrees is the horizontal field of view; deriving the vertical one keeps the
            // wide preview strip from stretching into a fisheye.
            float horizontalFov = (float) Math.toRadians(fovDegrees);
            float verticalFov = (float) (2.0 * Math.atan(Math.tan(horizontalFov * 0.5) / Math.max(0.2f, aspect)));
            // The preview is sampled by the GUI blitter, which expects a top-left texel origin,
            // so the projection is flipped vertically up front.
            PREVIEW_PROJECTION.identity()
                    .perspective(verticalFov, aspect, 0.05f, 1000.0f)
                    .scaleLocal(1.0f, -1.0f, 1.0f);
            if (previewProjection == null) {
                previewProjection = device.createBuffer(() -> "SkyJet preview projection",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 64);
            }
            PROJECTION_SCRATCH.clear();
            PREVIEW_PROJECTION.get(PROJECTION_SCRATCH);
            PROJECTION_SCRATCH.position(0).limit(64);
            encoder.writeToBuffer(previewProjection.slice(), PROJECTION_SCRATCH);

            if (previewFog == null) {
                ByteBuffer fogBytes = ByteBuffer.allocateDirect(48).order(ByteOrder.nativeOrder());
                while (fogBytes.hasRemaining()) fogBytes.put((byte) 0);
                fogBytes.flip();
                previewFog = device.createBuffer(() -> "SkyJet preview fog", GpuBuffer.USAGE_UNIFORM, fogBytes);
            }

            PREVIEW_VIEW.identity()
                    .rotateX((float) Math.toRadians(pitch))
                    .rotateY((float) Math.toRadians(yaw + 180.0f));
            var transforms = RenderSystem.getDynamicUniforms()
                    .writeTransform(PREVIEW_VIEW, WHITE, ZERO, IDENTITY);

            //? if >=26.2 {
            Optional<Vector4fc> clear = Optional.of(new org.joml.Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
            try (RenderPass pass = encoder.createRenderPass(() -> "SkyJet preview", color,
                    clear, depth, OptionalDouble.of(1.0))) {
            //?} else {
            /*try (RenderPass pass = encoder.createRenderPass(() -> "SkyJet preview", color,
                    OptionalInt.of(0xFF000000), depth, OptionalDouble.of(1.0))) {
            *///?}
                pass.setPipeline(SkyPipelines.sky());
                GpuBuffer globals = RenderSystem.getGlobalSettingsUniform();
                if (globals != null) {
                    pass.setUniform("Globals", globals);
                }
                pass.setUniform("Projection", previewProjection.slice());
                pass.setUniform("Fog", previewFog.slice());
                pass.setUniform("DynamicTransforms", transforms);
                pass.setUniform(SkyPipelines.UNIFORM_BLOCK, uniformBuffer.slice());
                bindTextures(pass);
                SkyCompat.bindVertices(pass, vertexBuffer);
                SkyCompat.drawTriangle(pass);
            }
        } catch (Exception e) {
            failed = true;
            SkyJet.LOG.error("SkyJet preview rendering failed", e);
        }
    }
}
