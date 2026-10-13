package dev.skyjet.config;

public final class BodyLayer {
    public static final String[] TYPE_NAMES = {"Planet", "Ringed Planet", "Glow Orb", "Black Hole", "Crescent Moon"};
    public static final String[] MOTION_NAMES = {"Fixed", "Orbit", "Follow day cycle", "Drift"};

    public static final int MOTION_FIXED = 0;
    public static final int MOTION_ORBIT = 1;
    public static final int MOTION_DAY_CYCLE = 2;
    public static final int MOTION_DRIFT = 3;

    public String name = "Planet";
    public boolean enabled = true;
    public int color = 0xFFB98A5A;
    public int shadowColor = 0xFF1A1208;
    public float intensity = 1.0f;

    /** Placement in the sky: azimuth in radians around the compass, elevation in radians. */
    public float azimuth = 0.0f;
    public float elevation = 0.45f;
    public float size = 0.06f;
    public int type = 0;

    public int motion = MOTION_ORBIT;
    public float orbitSpeed = 0.35f;
    public float orbitTilt = 0.0f;
    public float orbitPhase = 0.0f;

    public float glow = 0.25f;
    public float ringWidth = 0.6f;
    public float ringTilt = 0.34f;
    public float shading = 0.35f;
    /** Surface banding and mottling strength; 0 is a flat disc. */
    public float detail = 0.6f;
    /** Slow spin of the surface detail, so a planet is not a frozen sticker. */
    public float spinSpeed = 0.2f;

    public BodyLayer() {
    }

    public BodyLayer(String name, int color, float azimuth, float elevation, float size, int type) {
        this.name = name;
        this.color = color;
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.size = size;
        this.type = type;
    }

    public BodyLayer copy() {
        BodyLayer b = new BodyLayer();
        b.name = name;
        b.enabled = enabled;
        b.color = color;
        b.shadowColor = shadowColor;
        b.intensity = intensity;
        b.azimuth = azimuth;
        b.elevation = elevation;
        b.size = size;
        b.type = type;
        b.motion = motion;
        b.orbitSpeed = orbitSpeed;
        b.orbitTilt = orbitTilt;
        b.orbitPhase = orbitPhase;
        b.glow = glow;
        b.ringWidth = ringWidth;
        b.ringTilt = ringTilt;
        b.shading = shading;
        b.detail = detail;
        b.spinSpeed = spinSpeed;
        return b;
    }
}
