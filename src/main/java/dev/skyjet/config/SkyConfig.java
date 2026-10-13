package dev.skyjet.config;

import java.util.ArrayList;
import java.util.List;

public final class SkyConfig {
    public static final int MAX_GRADIENTS = 8;
    public static final int MAX_STARS = 6;
    public static final int MAX_NEBULAE = 4;
    public static final int MAX_AURORAE = 3;
    public static final int MAX_BODIES = 6;
    public static final int MAX_SKYBOXES = 6;
    public static final int MAX_CLOUDS = 3;

    /** What the sky does below the horizon. */
    public static final int BELOW_CONTINUE = 0;
    public static final int BELOW_MIRROR = 1;
    public static final int BELOW_GROUND = 2;
    public static final String[] BELOW_NAMES = {"Continue the sky", "Mirror the sky above", "Solid ground"};

    public String name = "Custom Sky";
    public String author = "";
    /** Preset this sky started from, so the editor's reset button has something to go back to. */
    public String basedOn = "";
    public boolean enabled = true;

    public float opacity = 1.0f;
    public float exposure = 1.0f;
    public float saturation = 1.0f;
    public float gamma = 1.0f;

    /**
     * Dither strength in output steps. Large smooth gradients quantise badly at 8 bits per
     * channel and read as blocky bands; a triangular dither of about one step removes them.
     */
    public float dither = 1.0f;
    /** Softens hard analytic edges using screen space derivatives, so nothing stair-steps. */
    public boolean antiAlias = true;

    public int quality = 3;
    /** Lets the shader drop detail on its own when the frame is expensive. */
    public boolean autoQuality = true;
    /**
     * How hard procedural detail sharpens as the camera zooms in. Without it a zoomed view just
     * magnifies the same octaves and the sky goes soft and blocky.
     */
    public float zoomDetail = 1.0f;

    public float horizonOffset = 0.0f;
    public float horizonSoftness = 0.05f;
    public float horizonCurve = 0.65f;

    /**
     * The lower hemisphere used to fall to the dark end of every gradient, so looking down showed
     * a black floor instead of sky.
     */
    public int belowHorizon = BELOW_CONTINUE;
    public int groundColor = 0xFF0A0D12;

    public boolean atmosphereEnabled = true;
    public float rayleigh = 1.0f;
    public float mie = 1.0f;
    public float mieG = 0.76f;
    public float atmosphereDensity = 1.0f;
    public int atmosphereTint = 0xFFFFFFFF;
    public float sunsetGlow = 1.0f;

    public boolean drawSun = false;
    public boolean drawMoon = false;
    public int sunColor = 0xFFFFE6BC;
    public float sunSize = 0.035f;
    public int moonColor = 0xFFD6E1FF;
    public float moonSize = 0.030f;

    public float fogBlend = 0.25f;
    public float fogHeight = 0.07f;

    public AnimationSettings animation = new AnimationSettings();
    public WorldSettings world = new WorldSettings();
    public LayerStack layers = LayerStack.defaults();

    public List<SkyboxLayer> skyboxes = new ArrayList<>();
    public List<GradientLayer> gradients = new ArrayList<>();
    public List<StarLayer> stars = new ArrayList<>();
    public List<NebulaLayer> nebulae = new ArrayList<>();
    public List<AuroraLayer> aurorae = new ArrayList<>();
    public List<BodyLayer> bodies = new ArrayList<>();
    public List<CloudLayer> clouds = new ArrayList<>();

    public void sanitize() {
        if (animation == null) animation = new AnimationSettings();
        if (world == null) world = new WorldSettings();
        if (layers == null) layers = LayerStack.defaults();
        layers.sanitize();
        if (skyboxes == null) skyboxes = new ArrayList<>();
        if (gradients == null) gradients = new ArrayList<>();
        if (stars == null) stars = new ArrayList<>();
        if (nebulae == null) nebulae = new ArrayList<>();
        if (aurorae == null) aurorae = new ArrayList<>();
        if (bodies == null) bodies = new ArrayList<>();
        if (clouds == null) clouds = new ArrayList<>();
        trim(skyboxes, MAX_SKYBOXES);
        trim(gradients, MAX_GRADIENTS);
        trim(stars, MAX_STARS);
        trim(nebulae, MAX_NEBULAE);
        trim(aurorae, MAX_AURORAE);
        trim(bodies, MAX_BODIES);
        trim(clouds, MAX_CLOUDS);
        quality = clamp(quality, 1, 5);
        opacity = clamp(opacity, 0.0f, 1.0f);
        exposure = clamp(exposure, 0.0f, 6.0f);
        saturation = clamp(saturation, 0.0f, 3.0f);
        gamma = clamp(gamma, 0.2f, 4.0f);
        dither = clamp(dither, 0.0f, 4.0f);
        zoomDetail = clamp(zoomDetail, 0.0f, 3.0f);
        belowHorizon = clamp(belowHorizon, 0, BELOW_NAMES.length - 1);
        if (basedOn == null) basedOn = "";
        for (SkyboxLayer s : skyboxes) {
            if (s.texture == null) s.texture = "";
        }
    }

    /** Wipes every layer and returns the sky to a blank canvas, keeping only the profile name. */
    public static SkyConfig blank(String name) {
        SkyConfig c = new SkyConfig();
        c.name = name;
        c.author = "";
        c.basedOn = "";
        c.atmosphereEnabled = false;
        c.gradients.add(new GradientLayer("Base", 0xFF05070C, 0xFF05070C, 0.5f, 0.5f));
        return c;
    }

    private static void trim(List<?> list, int max) {
        while (list.size() > max) {
            list.remove(list.size() - 1);
        }
    }

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public SkyConfig copy() {
        SkyConfig c = new SkyConfig();
        c.name = name;
        c.author = author;
        c.basedOn = basedOn;
        c.enabled = enabled;
        c.opacity = opacity;
        c.exposure = exposure;
        c.saturation = saturation;
        c.gamma = gamma;
        c.dither = dither;
        c.antiAlias = antiAlias;
        c.quality = quality;
        c.autoQuality = autoQuality;
        c.zoomDetail = zoomDetail;
        c.horizonOffset = horizonOffset;
        c.horizonSoftness = horizonSoftness;
        c.horizonCurve = horizonCurve;
        c.belowHorizon = belowHorizon;
        c.groundColor = groundColor;
        c.atmosphereEnabled = atmosphereEnabled;
        c.rayleigh = rayleigh;
        c.mie = mie;
        c.mieG = mieG;
        c.atmosphereDensity = atmosphereDensity;
        c.atmosphereTint = atmosphereTint;
        c.sunsetGlow = sunsetGlow;
        c.drawSun = drawSun;
        c.drawMoon = drawMoon;
        c.sunColor = sunColor;
        c.sunSize = sunSize;
        c.moonColor = moonColor;
        c.moonSize = moonSize;
        c.fogBlend = fogBlend;
        c.fogHeight = fogHeight;
        c.animation = animation.copy();
        c.world = world.copy();
        c.layers = layers.copy();
        for (SkyboxLayer s : skyboxes) c.skyboxes.add(s.copy());
        for (GradientLayer g : gradients) c.gradients.add(g.copy());
        for (StarLayer s : stars) c.stars.add(s.copy());
        for (NebulaLayer n : nebulae) c.nebulae.add(n.copy());
        for (AuroraLayer a : aurorae) c.aurorae.add(a.copy());
        for (BodyLayer b : bodies) c.bodies.add(b.copy());
        for (CloudLayer cl : clouds) c.clouds.add(cl.copy());
        return c;
    }
}
