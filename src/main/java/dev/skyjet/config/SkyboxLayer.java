package dev.skyjet.config;

/**
 * A textured skybox layer: a resource pack panorama or cubemap wrapped around the whole sky.
 *
 * <p>This is what makes SkyJet usable as a plain skybox mod, and the fields mirror what the two
 * formats sky packs actually ship use, so an imported pack maps onto it one to one:
 * <ul>
 *   <li><b>OptiFine</b> ({@code assets/minecraft/optifine/sky/world0/skyN.properties}) — one 3:2
 *       image holding all six faces, plus blend, rotate, speed, axis and four fade times.</li>
 *   <li><b>FabricSkyBoxes / Nuit v2</b> ({@code assets/<ns>/sky/*.json}) — six separate face
 *       textures, a blend type, a fade block in ticks, and a rotation block.</li>
 * </ul>
 */
public final class SkyboxLayer {
    /** One equirectangular image. */
    public static final int MODE_PANORAMA = 0;
    /** One 3:2 image holding all six faces, the layout both sky pack formats use. */
    public static final int MODE_CUBE_ATLAS = 1;
    /** Six separate face images, named by suffix or given explicitly. */
    public static final int MODE_CUBE_FACES = 2;
    /** One polar image covering the upper hemisphere. */
    public static final int MODE_DOME = 3;

    public static final String[] MODE_NAMES = {
            "Panorama (equirect)", "Cube sheet (3:2)", "Cube faces (6 files)", "Overhead dome"
    };

    // Ordered to match blendLayer() in the fragment shader, and to cover every OptiFine blend.
    public static final String[] BLEND_NAMES = {
            "Normal", "Add", "Multiply", "Screen", "Overlay", "Soft Light", "Lighten",
            "Subtract", "Dodge", "Burn", "Replace", "Alpha"
    };

    public static final int BLEND_NORMAL = 0;
    public static final int BLEND_ADD = 1;
    public static final int BLEND_MULTIPLY = 2;
    public static final int BLEND_REPLACE = 10;
    public static final int BLEND_ALPHA = 11;

    public String name = "Skybox";
    public boolean enabled = true;

    /**
     * Panorama, dome and cube sheet: the texture itself. Cube faces: the shared prefix, with
     * {@code _top}/{@code _bottom}/{@code _north}/{@code _south}/{@code _east}/{@code _west} or
     * {@code _0}..{@code _5} appended.
     */
    public String texture = "";
    /** Set by the pack importer when a FabricSkyBoxes json names all six faces explicitly. */
    public String[] faceTextures;
    public int mode = MODE_CUBE_ATLAS;

    public int tint = 0xFFFFFFFF;
    public float opacity = 1.0f;
    public float brightness = 1.0f;
    public int blendMode = BLEND_NORMAL;

    /** Constant orientation, in radians. */
    public float yaw = 0.0f;
    public float pitch = 0.0f;
    public float roll = 0.0f;

    /** Rotation with the day cycle, matching OptiFine's rotate/speed/axis. */
    public boolean rotate = false;
    public float rotationSpeed = 1.0f;
    public float axisX = 0.0f;
    public float axisY = 0.0f;
    public float axisZ = 1.0f;

    /**
     * Time of day fade, in ticks over a 24000 tick day. When {@code fadeEnabled} is off the layer
     * is simply always on.
     */
    public boolean fadeEnabled = false;
    public float startFadeIn = 22000.0f;
    public float endFadeIn = 23500.0f;
    public float startFadeOut = 11000.0f;
    public float endFadeOut = 12500.0f;

    public boolean inClear = true;
    public boolean inRain = true;
    public boolean inThunder = true;

    /** Treats pure black as transparent, which is how most overlay sky textures are authored. */
    public boolean blackIsTransparent = false;
    /** 0 keeps the texture at full strength all day; 1 fades it out with the stars. */
    public float nightOnly = 0.0f;

    public SkyboxLayer() {
    }

    public SkyboxLayer(String name, String texture, int mode) {
        this.name = name;
        this.texture = texture;
        this.mode = mode;
    }

    /** Cache key: two layers pointing at the same art share one stitched atlas. */
    public String textureKey() {
        if (mode == MODE_CUBE_FACES && faceTextures != null) {
            return String.join("|", faceTextures);
        }
        return mode + ":" + (texture == null ? "" : texture);
    }

    public SkyboxLayer copy() {
        SkyboxLayer s = new SkyboxLayer();
        s.name = name;
        s.enabled = enabled;
        s.texture = texture;
        s.faceTextures = faceTextures == null ? null : faceTextures.clone();
        s.mode = mode;
        s.tint = tint;
        s.opacity = opacity;
        s.brightness = brightness;
        s.blendMode = blendMode;
        s.yaw = yaw;
        s.pitch = pitch;
        s.roll = roll;
        s.rotate = rotate;
        s.rotationSpeed = rotationSpeed;
        s.axisX = axisX;
        s.axisY = axisY;
        s.axisZ = axisZ;
        s.fadeEnabled = fadeEnabled;
        s.startFadeIn = startFadeIn;
        s.endFadeIn = endFadeIn;
        s.startFadeOut = startFadeOut;
        s.endFadeOut = endFadeOut;
        s.inClear = inClear;
        s.inRain = inRain;
        s.inThunder = inThunder;
        s.blackIsTransparent = blackIsTransparent;
        s.nightOnly = nightOnly;
        return s;
    }
}
