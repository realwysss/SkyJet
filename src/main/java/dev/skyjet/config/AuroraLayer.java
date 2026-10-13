package dev.skyjet.config;

public final class AuroraLayer {
    /**
     * Borealis models the real thing: a curtain hanging off an auroral arc, with a hard lower
     * edge, vertical rays along the field lines and a colour ramp that follows altitude. Veil is
     * the older look, a soft ridged sheet painted across the sky.
     */
    public static final String[] STYLE_NAMES = {"Aurora borealis", "Veil"};
    public static final int STYLE_BOREALIS = 0;
    public static final int STYLE_VEIL = 1;

    public String name = "Aurora";
    public boolean enabled = true;
    public int style = STYLE_BOREALIS;

    /** Bottom of the curtain, where N2+ emission puts a pink fringe under the green. */
    public int colorLow = 0xFFFF4FA8;
    /** The body of the curtain: atomic oxygen at 557.7nm, the green everyone pictures. */
    public int colorBase = 0xFF39FFA6;
    /** The top, where thin air and 630nm oxygen turn it deep red. */
    public int colorTip = 0xFFB03CFF;

    public float intensity = 0.85f;
    /** Curtain foot, in blocks above the camera. */
    public float baseHeight = 120.0f;
    /** Curtain top. Real aurorae run 100km to 300km; the ratio matters, not the units. */
    public float topHeight = 520.0f;
    public float speed = 0.35f;
    /** Height samples taken up the curtain. */
    public int steps = 10;
    public float groundGlow = 0.35f;

    // ------------------------------------------------------------- the arc

    /** Compass bearing the arc sits on, in degrees. 0 is north, which is where it usually is. */
    public float arcAzimuth = 0.0f;
    /** How far out the arc hangs, in blocks. */
    public float arcDistance = 900.0f;
    /** Thickness of the curtain across its face. Real curtains are only a few km thick. */
    public float arcWidth = 130.0f;
    /** 0 draws a straight arc, 1 wraps it right around the player. */
    public float curvature = 0.35f;
    /** A second and third curtain behind the first, the way active displays stack up. */
    public int bands = 2;
    public float bandSpacing = 0.55f;

    // ----------------------------------------------------------- the folds

    /** How far the curtain snakes sideways along its length. */
    public float foldAmount = 1.0f;
    /** Size of those folds. Low is long lazy curves, high is tight curls. */
    public float foldScale = 1.0f;
    /** How fast the folds travel along the arc. */
    public float foldSpeed = 1.0f;

    // ------------------------------------------------------------ the rays

    /** Strength of the vertical striations. This is what stops it reading as a flat sheet. */
    public float rayAmount = 0.8f;
    /** How fine the rays are. */
    public float rayScale = 1.0f;
    /** Contrast between a lit ray and the gap beside it. */
    public float raySharpness = 0.6f;

    // --------------------------------------------------------- the profile

    /** How abruptly the curtain starts at its lower edge. High is the knife edge of a real one. */
    public float bottomSharpness = 0.75f;
    /** How quickly it fades out toward the top. */
    public float topFalloff = 0.55f;
    /** Substorm breathing: the whole display brightening and dimming. */
    public float pulse = 0.25f;
    public float pulseSpeed = 0.6f;
    /** Slow drift of the whole arc across the sky. */
    public float drift = 0.15f;

    // ----------------------------------------------- veil style only

    public float waviness = 1.0f;
    public float sharpness = 0.55f;
    public float coverage = 0.55f;
    public float thickness = 140.0f;
    public float depthFade = 0.6f;

    public AuroraLayer() {
    }

    public AuroraLayer(String name, int colorBase, int colorTip, float intensity, float baseHeight, float topHeight) {
        this.name = name;
        this.colorBase = colorBase;
        this.colorTip = colorTip;
        this.intensity = intensity;
        this.baseHeight = baseHeight;
        this.topHeight = topHeight;
    }

    public AuroraLayer copy() {
        AuroraLayer a = new AuroraLayer();
        a.name = name;
        a.enabled = enabled;
        a.style = style;
        a.colorLow = colorLow;
        a.colorBase = colorBase;
        a.colorTip = colorTip;
        a.intensity = intensity;
        a.baseHeight = baseHeight;
        a.topHeight = topHeight;
        a.speed = speed;
        a.steps = steps;
        a.groundGlow = groundGlow;
        a.arcAzimuth = arcAzimuth;
        a.arcDistance = arcDistance;
        a.arcWidth = arcWidth;
        a.curvature = curvature;
        a.bands = bands;
        a.bandSpacing = bandSpacing;
        a.foldAmount = foldAmount;
        a.foldScale = foldScale;
        a.foldSpeed = foldSpeed;
        a.rayAmount = rayAmount;
        a.rayScale = rayScale;
        a.raySharpness = raySharpness;
        a.bottomSharpness = bottomSharpness;
        a.topFalloff = topFalloff;
        a.pulse = pulse;
        a.pulseSpeed = pulseSpeed;
        a.drift = drift;
        a.waviness = waviness;
        a.sharpness = sharpness;
        a.coverage = coverage;
        a.thickness = thickness;
        a.depthFade = depthFade;
        return a;
    }
}
