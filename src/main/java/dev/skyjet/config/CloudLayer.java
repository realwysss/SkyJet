package dev.skyjet.config;

/**
 * One volumetric cloud deck. Several can stack at different altitudes and speeds, which is what
 * gives a sky weather instead of a single painted sheet.
 */
public final class CloudLayer {
    public static final String[] TYPE_NAMES = {"Cumulus", "Stratus", "Cirrus", "Storm"};

    public String name = "Clouds";
    public boolean enabled = true;
    public int type = 0;

    public int color = 0xFFEFF4FF;
    public int shadowColor = 0xFF6A7690;
    public float opacity = 0.85f;

    /** How much of the sky the deck covers. */
    public float coverage = 0.45f;
    /** How many separate cloud masses form within the coverage; higher breaks the deck up. */
    public float quantity = 1.0f;
    public float density = 1.0f;
    public float sharpness = 0.55f;
    public float selfShadow = 0.65f;
    public float altitude = 220.0f;
    public float scale = 1.0f;
    public int octaves = 5;

    /** Drift across the sky along the wind bearing, and the rate shapes churn in place. */
    public float speed = 1.0f;
    public float evolveSpeed = 1.0f;
    public float windAngle = 0.35f;

    public CloudLayer() {
    }

    public CloudLayer(String name, int type, float coverage, float altitude) {
        this.name = name;
        this.type = type;
        this.coverage = coverage;
        this.altitude = altitude;
    }

    public CloudLayer copy() {
        CloudLayer c = new CloudLayer();
        c.name = name;
        c.enabled = enabled;
        c.type = type;
        c.color = color;
        c.shadowColor = shadowColor;
        c.opacity = opacity;
        c.coverage = coverage;
        c.quantity = quantity;
        c.density = density;
        c.sharpness = sharpness;
        c.selfShadow = selfShadow;
        c.altitude = altitude;
        c.scale = scale;
        c.octaves = octaves;
        c.speed = speed;
        c.evolveSpeed = evolveSpeed;
        c.windAngle = windAngle;
        return c;
    }
}
