package dev.skyjet.config;

public final class GradientLayer {
    public static final int AXIS_VERTICAL = 0;
    public static final int AXIS_AZIMUTH = 1;
    public static final int AXIS_SUN = 2;
    public static final int AXIS_DIRECTIONAL = 3;
    public static final int AXIS_HORIZON_BAND = 4;

    public static final String[] AXIS_NAMES = {"Vertical", "Compass", "Around Sun", "Directional", "Horizon Band"};
    public static final String[] BLEND_NAMES = {"Normal", "Add", "Multiply", "Screen", "Overlay", "Soft Light", "Lighten"};

    public String name = "Gradient";
    public boolean enabled = true;
    public int colorA = 0xFF0B1026;
    public int colorB = 0xFF4E7FCB;
    public float position = 0.5f;
    public float softness = 0.35f;
    public float opacity = 1.0f;
    public int blendMode = 0;
    public float angle = 0.0f;
    public float noiseAmount = 0.0f;
    public float noiseScale = 1.0f;
    public int axis = AXIS_VERTICAL;

    public GradientLayer() {
    }

    public GradientLayer(String name, int colorA, int colorB, float position, float softness) {
        this.name = name;
        this.colorA = colorA;
        this.colorB = colorB;
        this.position = position;
        this.softness = softness;
    }

    public GradientLayer copy() {
        GradientLayer g = new GradientLayer();
        g.name = name;
        g.enabled = enabled;
        g.colorA = colorA;
        g.colorB = colorB;
        g.position = position;
        g.softness = softness;
        g.opacity = opacity;
        g.blendMode = blendMode;
        g.angle = angle;
        g.noiseAmount = noiseAmount;
        g.noiseScale = noiseScale;
        g.axis = axis;
        return g;
    }
}
