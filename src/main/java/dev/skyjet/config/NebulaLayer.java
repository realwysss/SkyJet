package dev.skyjet.config;

public final class NebulaLayer {
    public static final String[] SHAPE_NAMES = {"Clouds", "Galactic Band", "Spiral Arm", "Filaments"};

    public String name = "Nebula";
    public boolean enabled = true;
    public int colorA = 0xFF5B2BA8;
    public int colorB = 0xFF1B6FB5;
    public float intensity = 0.7f;
    public float scale = 2.2f;
    public float contrast = 0.6f;
    public float depth = 1.0f;
    public int octaves = 5;
    public float driftSpeed = 1.0f;
    public float warp = 0.4f;
    public float elevationBias = 0.0f;
    public int shape = 0;

    public NebulaLayer() {
    }

    public NebulaLayer(String name, int colorA, int colorB, float intensity, float scale, int shape) {
        this.name = name;
        this.colorA = colorA;
        this.colorB = colorB;
        this.intensity = intensity;
        this.scale = scale;
        this.shape = shape;
    }

    public NebulaLayer copy() {
        NebulaLayer n = new NebulaLayer();
        n.name = name;
        n.enabled = enabled;
        n.colorA = colorA;
        n.colorB = colorB;
        n.intensity = intensity;
        n.scale = scale;
        n.contrast = contrast;
        n.depth = depth;
        n.octaves = octaves;
        n.driftSpeed = driftSpeed;
        n.warp = warp;
        n.elevationBias = elevationBias;
        n.shape = shape;
        return n;
    }
}
