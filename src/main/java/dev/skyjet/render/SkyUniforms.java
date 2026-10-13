package dev.skyjet.render;

import dev.skyjet.config.AuroraLayer;
import dev.skyjet.config.BodyLayer;
import dev.skyjet.config.CloudLayer;
import dev.skyjet.config.GradientLayer;
import dev.skyjet.config.LayerStack;
import dev.skyjet.config.NebulaLayer;
import dev.skyjet.config.SkyConfig;
import dev.skyjet.config.SkyboxLayer;
import dev.skyjet.config.StarLayer;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Packs a {@link SkyConfig} plus the live frame state into the std140 layout consumed by
 * {@code assets/skyjet/shaders/core/skyjet_sky.fsh}.
 *
 * <p>The vec4 indices below are the contract with that shader. They are contiguous and ordered
 * exactly as the block declares its members, because std140 lays a block of {@code vec4} and
 * {@code vec4[]} members out with no padding beyond the sixteen byte stride.
 */
public final class SkyUniforms {
    private static final int SKY_C = 17;
    private static final int SKY_P = 23;
    private static final int SKY_Q = 29;
    private static final int SKY_R = 35;
    private static final int SKY_A = 41;
    private static final int SKY_F = 47;
    private static final int SKY_W = 53;

    private static final int GRAD_A = 59;
    private static final int GRAD_B = 67;
    private static final int GRAD_P = 75;
    private static final int GRAD_Q = 83;

    private static final int STAR_C = 91;
    private static final int STAR_C2 = 97;
    private static final int STAR_P = 103;
    private static final int STAR_Q = 109;
    private static final int STAR_R = 115;
    private static final int STAR_S = 121;
    private static final int STAR_T = 127;

    private static final int NEB_C = 133;
    private static final int NEB_C2 = 137;
    private static final int NEB_P = 141;
    private static final int NEB_Q = 145;

    private static final int AUR_C = 149;
    private static final int AUR_C2 = 152;
    private static final int AUR_P = 155;
    private static final int AUR_Q = 158;
    private static final int AUR_R = 161;

    private static final int BODY_C = 164;
    private static final int BODY_C2 = 170;
    private static final int BODY_P = 176;
    private static final int BODY_Q = 182;
    private static final int BODY_R = 188;
    private static final int BODY_S = 194;

    private static final int CLOUD_C = 200;
    private static final int CLOUD_C2 = 203;
    private static final int CLOUD_P = 206;
    private static final int CLOUD_Q = 209;
    private static final int CLOUD_R = 212;

    private static final int STACK_A = 215;
    private static final int STACK_T = 224;

    // Appended after the original block rather than inserted next to AUR_R, so adding the
    // borealis parameters did not renumber every array that came after the aurora.
    private static final int AUR_S = 233;
    private static final int AUR_T = 236;
    private static final int AUR_U = 239;
    private static final int AUR_V = 242;
    private static final int AUR_W = 245;

    public static final int VEC4_COUNT = AUR_W + SkyConfig.MAX_AURORAE;
    public static final int SIZE_BYTES = VEC4_COUNT * 16;

    /** Width over height of assets/skyjet/textures/watermark.png. */
    private static final float WATERMARK_ASPECT = 2048.0f / 512.0f;
    /**
     * Angular size of the mark. Smaller values spread the same texture over less sky, which is
     * both less obtrusive and sharper, because the texels land closer together on screen.
     */
    private static final float WATERMARK_SCALE = 0.34f;
    private static final float WATERMARK_OPACITY = 0.8f;

    private final float[] data = new float[VEC4_COUNT * 4];

    public void clear() {
        java.util.Arrays.fill(data, 0.0f);
    }

    private void set(int vec4Index, float x, float y, float z, float w) {
        int i = vec4Index * 4;
        data[i] = x;
        data[i + 1] = y;
        data[i + 2] = z;
        data[i + 3] = w;
    }

    private void setColor(int vec4Index, int argb, float w) {
        int i = vec4Index * 4;
        data[i] = ((argb >> 16) & 0xFF) / 255.0f;
        data[i + 1] = ((argb >> 8) & 0xFF) / 255.0f;
        data[i + 2] = (argb & 0xFF) / 255.0f;
        data[i + 3] = w;
    }

    private void setColorWithAlpha(int vec4Index, int argb) {
        setColor(vec4Index, argb, ((argb >>> 24) & 0xFF) / 255.0f);
    }

    public void pack(SkyConfig cfg, SkyState state, SkyboxLayer[] skyboxLayers,
                     SkyTextures.Binding[] skyboxBindings, int skyboxCount) {
        clear();

        set(0, state.camX, state.camY, state.camZ, state.time);
        set(1, state.sunX, state.sunY, state.sunZ, 1.0f);
        set(2, -state.sunX, -state.sunY, -state.sunZ, 1.0f);
        set(3, state.dayFraction, state.rain, state.thunder, state.starVisibility);
        set(4, cfg.opacity, cfg.exposure, cfg.saturation, cfg.gamma);

        int gradCount = countEnabled(cfg.gradients, SkyConfig.MAX_GRADIENTS);
        int starCount = countEnabled(cfg.stars, SkyConfig.MAX_STARS);
        int nebCount = countEnabled(cfg.nebulae, SkyConfig.MAX_NEBULAE);
        int aurCount = countEnabled(cfg.aurorae, SkyConfig.MAX_AURORAE);
        int bodyCount = countEnabled(cfg.bodies, SkyConfig.MAX_BODIES);
        int cloudCount = countEnabled(cfg.clouds, SkyConfig.MAX_CLOUDS);

        set(5, gradCount, starCount, nebCount, aurCount);
        set(6, bodyCount, cloudCount, cfg.atmosphereEnabled ? 1.0f : 0.0f, state.effectiveQuality);
        set(7, skyboxCount, cfg.antiAlias ? 1.0f : 0.0f, cfg.dither, state.worldTicks);
        set(8, cfg.rayleigh, cfg.mie, cfg.mieG, cfg.atmosphereDensity);
        setColor(9, cfg.atmosphereTint, cfg.sunsetGlow);
        setColor(10, cfg.sunColor, cfg.sunSize);
        setColor(11, cfg.moonColor, cfg.moonSize);
        setColor(12, cfg.groundColor, cfg.belowHorizon);
        set(13, cfg.horizonOffset, cfg.horizonSoftness, cfg.horizonCurve, cfg.zoomDetail);
        set(14, cfg.fogBlend, cfg.fogHeight, cfg.drawSun ? 1.0f : 0.0f, cfg.drawMoon ? 1.0f : 0.0f);
        set(15, WATERMARK_OPACITY, WATERMARK_SCALE, WATERMARK_ASPECT, 1.0f);
        setColor(16, cfg.layers.masterTint, 1.0f);

        for (int slot = 0; slot < skyboxCount && slot < SkyConfig.MAX_SKYBOXES; slot++) {
            SkyboxLayer s = skyboxLayers[slot];
            SkyTextures.Binding binding = skyboxBindings[slot];
            if (s == null || binding == null) continue;
            setColor(SKY_C + slot, s.tint, s.opacity);
            set(SKY_P + slot, s.mode, s.yaw, s.pitch, s.roll);
            set(SKY_Q + slot, s.blendMode, s.brightness, s.nightOnly, s.blackIsTransparent ? 1.0f : 0.0f);
            set(SKY_R + slot, binding.texelU(), binding.texelV(), s.rotate ? 1.0f : 0.0f, s.rotationSpeed);
            set(SKY_A + slot, s.axisX, s.axisY, s.axisZ, s.fadeEnabled ? 1.0f : 0.0f);
            set(SKY_F + slot, s.startFadeIn, s.endFadeIn, s.startFadeOut, s.endFadeOut);
            set(SKY_W + slot, s.inClear ? 1.0f : 0.0f, s.inRain ? 1.0f : 0.0f, s.inThunder ? 1.0f : 0.0f, 0.0f);
        }

        int slot = 0;
        for (GradientLayer g : cfg.gradients) {
            if (!g.enabled || slot >= SkyConfig.MAX_GRADIENTS) continue;
            setColorWithAlpha(GRAD_A + slot, g.colorA);
            setColorWithAlpha(GRAD_B + slot, g.colorB);
            set(GRAD_P + slot, g.position, g.softness, g.opacity, g.blendMode);
            set(GRAD_Q + slot, g.angle, g.noiseAmount, g.noiseScale, g.axis);
            slot++;
        }

        slot = 0;
        for (StarLayer s : cfg.stars) {
            if (!s.enabled || slot >= SkyConfig.MAX_STARS) continue;
            setColor(STAR_C + slot, s.color, s.colorVariance);
            setColor(STAR_C2 + slot, s.colorB, 1.0f);
            set(STAR_P + slot, s.density, s.size, s.brightness * state.starBoost, s.depth);
            set(STAR_Q + slot, s.twinkleSpeed, s.twinkleAmount, s.rotationSpeed, s.shape);
            set(STAR_R + slot, s.glow, s.minPixelSize, s.orbitTilt, s.bandBias);
            // The seed is scaled into a range that actually moves the hash lattice.
            set(STAR_S + slot, s.bandTightness, s.sparkleAmount, s.sizeVariance, s.seed * 13.37f);
            set(STAR_T + slot, s.jitter, s.frozen ? 1.0f : 0.0f, s.driftSpeed, 0.0f);
            slot++;
        }

        slot = 0;
        for (NebulaLayer n : cfg.nebulae) {
            if (!n.enabled || slot >= SkyConfig.MAX_NEBULAE) continue;
            setColor(NEB_C + slot, n.colorA, n.intensity);
            setColor(NEB_C2 + slot, n.colorB, 1.0f);
            set(NEB_P + slot, n.scale, n.contrast, n.depth, n.octaves);
            set(NEB_Q + slot, n.driftSpeed, n.warp, n.elevationBias, n.shape);
            slot++;
        }

        slot = 0;
        for (AuroraLayer a : cfg.aurorae) {
            if (!a.enabled || slot >= SkyConfig.MAX_AURORAE) continue;
            setColor(AUR_C + slot, a.colorBase, a.intensity);
            setColor(AUR_C2 + slot, a.colorTip, a.style);
            set(AUR_P + slot, a.baseHeight, Math.max(a.topHeight, a.baseHeight + 16.0f),
                    a.speed, a.steps);
            set(AUR_Q + slot, a.waviness, a.sharpness, a.coverage, a.groundGlow);
            set(AUR_R + slot, a.thickness, a.depthFade, a.bands, a.bandSpacing);
            set(AUR_S + slot, (float) Math.toRadians(a.arcAzimuth), a.arcDistance,
                    a.arcWidth, a.curvature);
            set(AUR_T + slot, a.foldAmount, a.foldScale, a.foldSpeed, a.rayAmount);
            set(AUR_U + slot, a.rayScale, a.raySharpness, a.bottomSharpness, a.topFalloff);
            setColor(AUR_V + slot, a.colorLow, a.drift);
            set(AUR_W + slot, a.pulse, a.pulseSpeed, 0.0f, 0.0f);
            slot++;
        }

        slot = 0;
        for (BodyLayer b : cfg.bodies) {
            if (!b.enabled || slot >= SkyConfig.MAX_BODIES) continue;
            setColor(BODY_C + slot, b.color, b.intensity);
            setColor(BODY_C2 + slot, b.shadowColor, b.ringTilt);
            set(BODY_P + slot, b.azimuth, b.elevation, b.size, b.type);
            set(BODY_Q + slot, b.orbitSpeed, b.glow, b.ringWidth, b.shading);
            set(BODY_R + slot, b.orbitTilt, b.orbitPhase, b.motion, b.spinSpeed);
            set(BODY_S + slot, b.detail, 0.0f, 0.0f, 0.0f);
            slot++;
        }

        slot = 0;
        for (CloudLayer c : cfg.clouds) {
            if (!c.enabled || slot >= SkyConfig.MAX_CLOUDS) continue;
            setColor(CLOUD_C + slot, c.color, c.opacity);
            setColor(CLOUD_C2 + slot, c.shadowColor, c.evolveSpeed);
            set(CLOUD_P + slot, c.coverage, c.density, c.speed, c.altitude);
            set(CLOUD_Q + slot, c.octaves, c.sharpness, c.selfShadow, c.scale);
            set(CLOUD_R + slot, (float) Math.cos(c.windAngle), (float) Math.sin(c.windAngle),
                    c.quantity, c.type);
            slot++;
        }

        // The stack is packed in list order, which is the order the shader composites in.
        for (int i = 0; i < LayerStack.COUNT; i++) {
            LayerStack.Entry e = i < cfg.layers.entries.size() ? cfg.layers.entries.get(i) : null;
            if (e == null) {
                set(STACK_A + i, 0.0f, 0.0f, 0.0f, 0.0f);
                setColor(STACK_T + i, 0xFFFFFFFF, 1.0f);
                continue;
            }
            set(STACK_A + i, e.category, e.opacity, blendFor(e), e.enabled ? 1.0f : 0.0f);
            setColor(STACK_T + i, e.tint, 1.0f);
        }
    }

    /**
     * A category with no explicit blend uses the one that suits it: the base layers composite
     * with alpha, everything emissive adds.
     */
    private static int blendFor(LayerStack.Entry e) {
        if (e.blendMode >= 0) return e.blendMode;
        return switch (e.category) {
            case LayerStack.GRADIENTS, LayerStack.SKYBOX, LayerStack.CLOUDS -> SkyboxLayer.BLEND_NORMAL;
            default -> SkyboxLayer.BLEND_ADD;
        };
    }

    public void writeTo(ByteBuffer buffer) {
        buffer.clear();
        for (float f : data) {
            buffer.putFloat(f);
        }
        buffer.flip();
    }

    private static int countEnabled(List<?> list, int max) {
        int n = 0;
        for (Object o : list) {
            if (isEnabled(o)) n++;
            if (n >= max) break;
        }
        return n;
    }

    private static boolean isEnabled(Object o) {
        if (o instanceof GradientLayer g) return g.enabled;
        if (o instanceof StarLayer s) return s.enabled;
        if (o instanceof NebulaLayer n) return n.enabled;
        if (o instanceof AuroraLayer a) return a.enabled;
        if (o instanceof BodyLayer b) return b.enabled;
        if (o instanceof CloudLayer c) return c.enabled;
        return false;
    }
}
