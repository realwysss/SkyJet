package dev.skyjet.config;

public final class StarLayer {
    public static final String[] SHAPE_NAMES = {
            "Point", "Diffraction", "Soft Glow", "Sparkle",
            "Square pixel", "Cross", "Ring", "Diamond", "Cube"
    };

    public static final int SHAPE_POINT = 0;
    public static final int SHAPE_DIFFRACTION = 1;
    public static final int SHAPE_GLOW = 2;
    public static final int SHAPE_SPARKLE = 3;
    public static final int SHAPE_SQUARE = 4;
    public static final int SHAPE_CROSS = 5;
    public static final int SHAPE_RING = 6;
    public static final int SHAPE_DIAMOND = 7;
    public static final int SHAPE_CUBE = 8;

    public String name = "Stars";
    public boolean enabled = true;
    public int color = 0xFFFFFFFF;
    public int colorB = 0xFFFFC98A;
    public float colorVariance = 0.35f;
    public float density = 0.35f;
    public float size = 0.5f;
    public float sizeVariance = 0.75f;
    public float brightness = 1.0f;
    public float depth = 1.0f;
    public float twinkleSpeed = 1.0f;
    public float twinkleAmount = 0.35f;
    public float rotationSpeed = 1.0f;
    public int shape = 0;
    public float glow = 0.2f;

    /**
     * Floor on a star's on-screen radius, in pixels. Stars thinner than a pixel fall between
     * samples and vanish unless the camera zooms in, so they are widened and dimmed instead.
     */
    public float minPixelSize = 1.0f;

    /**
     * Seed for the field. Every layer hashes the same lattice, so two layers with the same seed
     * put their stars in the same places and the sky reads as one repeated pattern. Changing the
     * seed reshuffles the whole field.
     */
    public int seed = 0;
    /**
     * How far each star may wander inside its lattice cell. At 0 the field sits on a visible
     * grid; at 1 it looks scattered.
     */
    public float jitter = 0.85f;

    /** Stops all motion for this layer: no rotation, no drift, no twinkle. */
    public boolean frozen = false;

    public float orbitTilt = 0.0f;
    public float bandBias = 0.0f;
    public float bandTightness = 1.8f;
    public float driftSpeed = 0.0f;
    /** Fraction of the layer that shows as a brighter, larger "hero" star. */
    public float sparkleAmount = 0.12f;

    public StarLayer() {
    }

    public StarLayer(String name, int color, float density, float size, float brightness, float depth) {
        this.name = name;
        this.color = color;
        this.density = density;
        this.size = size;
        this.brightness = brightness;
        this.depth = depth;
    }

    public StarLayer copy() {
        StarLayer s = new StarLayer();
        s.name = name;
        s.enabled = enabled;
        s.color = color;
        s.colorB = colorB;
        s.colorVariance = colorVariance;
        s.density = density;
        s.size = size;
        s.sizeVariance = sizeVariance;
        s.brightness = brightness;
        s.depth = depth;
        s.twinkleSpeed = twinkleSpeed;
        s.twinkleAmount = twinkleAmount;
        s.rotationSpeed = rotationSpeed;
        s.shape = shape;
        s.glow = glow;
        s.minPixelSize = minPixelSize;
        s.seed = seed;
        s.jitter = jitter;
        s.frozen = frozen;
        s.orbitTilt = orbitTilt;
        s.bandBias = bandBias;
        s.bandTightness = bandTightness;
        s.driftSpeed = driftSpeed;
        s.sparkleAmount = sparkleAmount;
        return s;
    }
}
