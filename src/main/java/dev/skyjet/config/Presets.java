package dev.skyjet.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The built-in sky library.
 *
 * <p>Layer authoring rules used throughout this file:
 * <ul>
 *   <li>The gradient axis runs 0 at the nadir, 0.5 at the horizon and 1 at the zenith.</li>
 *   <li>{@code colorA} sits at the low end of the axis, {@code colorB} at the high end.</li>
 *   <li>The first layer is an opaque base ramp. Everything after it fades out with a
 *       zero alpha end stop so the layers stack instead of replacing each other.</li>
 *   <li>Daytime colour comes from atmospheric scattering, so bases are authored dark
 *       and stay correct through a full day/night cycle.</li>
 *   <li>Every preset puts something overhead. A sky whose only interest is a band at the
 *       horizon looks empty the moment the player looks up, which is most of the time.</li>
 *   <li>Everything that can move, moves. Bodies orbit, star fields rotate, nebulae drift
 *       and cloud decks run with the wind.</li>
 * </ul>
 */
public final class Presets {
    private static final Map<String, SkyConfig> BUILT_IN = new LinkedHashMap<>();

    private Presets() {
    }

    public static Map<String, SkyConfig> builtIn() {
        if (BUILT_IN.isEmpty()) {
            register(skyJetDefault());
            register(blueHour());
            register(beltOfVenus());
            register(alpenglow());
            register(volcanicSunset());
            register(harmattanDust());
            register(polarDay());
            register(supercell());
            register(mammatus());
            register(monsoonBreak());
            register(dramaticOvercast());
            register(desertNight());
            register(milkyWayCore());
            register(zodiacalLight());
            register(airglow());
            register(noctilucent());
            register(totalEclipse());
            register(auroraStorm());
            register(quietArc());
            register(auroraAustralis());
            register(violetCosmos());
            register(deepAqua());
            register(emberNebula());
            register(ringworld());
            register(twinSuns());
            register(giantsShadow());
            register(synthwave());
        }
        return BUILT_IN;
    }

    public static List<String> names() {
        return new ArrayList<>(builtIn().keySet());
    }

    public static SkyConfig get(String name) {
        SkyConfig c = builtIn().get(name);
        if (c == null) return null;
        SkyConfig copy = c.copy();
        copy.basedOn = name;
        return copy;
    }

    private static void register(SkyConfig config) {
        config.sanitize();
        BUILT_IN.put(config.name, config);
    }

    private static SkyConfig base(String name) {
        SkyConfig c = new SkyConfig();
        c.name = name;
        c.author = "Wayysss & HitsOnly";
        return c;
    }
    // ------------------------------------------------------------- presets
    //
    // Every sky here is taken from something real: an atmospheric effect, a night sky under
    // conditions that actually occur, or the look a well known sky pack is going for. Invented
    // colour schemes are what made the old library forgettable, so the brief for each one is
    // written above it and the numbers follow from the brief.

    // ------------------------------------------------- twilight and daylight

    /** The deep blue window after sunset, when the sun is 4 to 8 degrees down. */
    private static SkyConfig blueHour() {
        SkyConfig c = base("Blue Hour");
        c.gradients.add(ramp("Deep Blue", 0xFF1B3A6B, 0xFF071633));
        c.gradients.add(fade("Warm Rim", 0xFFE7A05C, 0.52f, 0.07f, 0, 0.85f));
        c.gradients.add(fade("Teal Band", 0xFF3D86B8, 0.57f, 0.15f, 0, 0.55f));
        c.gradients.add(crown("Cobalt Ceiling", 0xFF0E2A63, 0.66f, 0.34f, 0, 0.9f));
        c.gradients.add(crown("Zenith Indigo", 0xFF162A7A, 0.86f, 0.18f, 1, 0.45f));
        c.stars.add(spin(zenithBand(star("First Stars", 0xFFEAF1FF, 0.30f, 0.42f, 0.75f, 0.4f)), 0.5f));
        c.bodies.add(orbit(glow(body("Venus", 0xFFFFF6DE, -0.6f, 0.34f, 0.011f, 2), 0.7f), 0.12f, 0.15f));
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.85f;
        c.sunsetGlow = 1.25f;
        c.fogBlend = 0.30f;
        return c;
    }

    /**
     * The pink band opposite the setting sun, with the Earth's own shadow as a slate wedge
     * underneath it. Both are on the anti-solar side, which is why the warm colour is low and
     * the cold wedge is lower still.
     */
    private static SkyConfig beltOfVenus() {
        SkyConfig c = base("Belt of Venus");
        c.gradients.add(ramp("Upper Blue", 0xFF3F72B5, 0xFF12305F));
        c.gradients.add(fade("Earth Shadow", 0xFF3A4A72, 0.515f, 0.035f, 0, 0.95f));
        c.gradients.add(crown("Venus Belt", 0xFFE9A8B8, 0.545f, 0.05f, 0, 0.9f));
        c.gradients.add(fade("Belt Underside", 0xFFC9889C, 0.53f, 0.03f, 0, 0.55f));
        c.gradients.add(crown("Pale Ceiling", 0xFF2E5FA8, 0.70f, 0.30f, 0, 0.75f));
        c.gradients.add(crown("Zenith Depth", 0xFF10306E, 0.88f, 0.16f, 0, 0.6f));
        c.stars.add(spin(zenithBand(star("Faint", 0xFFE8EEFF, 0.16f, 0.36f, 0.35f, 0.3f)), 0.4f));
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.9f;
        c.sunsetGlow = 0.8f;
        c.saturation = 1.05f;
        return c;
    }

    /** Last light on high ground: the sun already down, the sky still lit from below. */
    private static SkyConfig alpenglow() {
        SkyConfig c = base("Alpenglow");
        c.gradients.add(ramp("Cold Upper", 0xFF4A5E9E, 0xFF161E44));
        c.gradients.add(fade("Ember Horizon", 0xFFFF7A4A, 0.515f, 0.05f, 0, 0.9f));
        c.gradients.add(fade("Rose Wash", 0xFFF2A6B4, 0.56f, 0.13f, 0, 0.7f));
        c.gradients.add(crown("Lilac Ceiling", 0xFF7A6BC4, 0.70f, 0.28f, 0, 0.7f));
        c.gradients.add(crown("Zenith Violet", 0xFF3B3080, 0.88f, 0.16f, 1, 0.5f));
        deck(c).coverage = 0.30f;
        deck(c).type = 2;
        deck(c).color = 0xFFFFC6A0;
        deck(c).shadowColor = 0xFF6A4A72;
        deck(c).altitude = 520.0f;
        deck(c).opacity = 0.75f;
        deck(c).speed = 0.7f;
        addDeck(c, "High Cirrus", 2, 0.22f, 900.0f, 0xFFFFE2C8, 0.5f).scale = 1.8f;
        c.stars.add(spin(zenithBand(star("Emerging", 0xFFF0F4FF, 0.20f, 0.38f, 0.45f, 0.3f)), 0.4f));
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.8f;
        c.sunsetGlow = 1.6f;
        c.saturation = 1.12f;
        return c;
    }

    /**
     * Stratospheric dust after a large eruption. Tambora and Krakatoa both gave months of
     * sunsets like this: the colour sits far higher in the sky than an ordinary one.
     */
    private static SkyConfig volcanicSunset() {
        SkyConfig c = base("Volcanic Sunset");
        c.gradients.add(ramp("Upper", 0xFF6A2D5E, 0xFF1E0A2A));
        c.gradients.add(fade("Blood Horizon", 0xFFE83A2E, 0.515f, 0.05f, 0, 0.95f));
        c.gradients.add(fade("Sulphur", 0xFFFFB13C, 0.54f, 0.08f, 0, 0.8f));
        c.gradients.add(crown("Ash Purple", 0xFF8A3A7A, 0.62f, 0.22f, 0, 0.8f));
        c.gradients.add(crown("High Glow", 0xFFD1567A, 0.80f, 0.22f, 1, 0.45f));
        c.gradients.add(crown("Zenith Ember", 0xFF5E1848, 0.92f, 0.12f, 0, 0.5f));
        addDeck(c, "Ash Veil", 2, 0.35f, 700.0f, 0xFFFFA268, 0.55f).scale = 2.2f;
        c.stars.add(spin(zenithBand(star("Through Haze", 0xFFFFE0C0, 0.18f, 0.40f, 0.40f, 0.3f)), 0.4f));
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 1.0f;
        c.sunsetGlow = 2.0f;
        c.saturation = 1.25f;
        c.exposure = 1.05f;
        return c;
    }

    /** Saharan dust carried west: a flat white sun, no blue left anywhere, everything ochre. */
    private static SkyConfig harmattanDust() {
        SkyConfig c = base("Harmattan Dust");
        c.gradients.add(ramp("Dust", 0xFFC79A66, 0xFF8A6B45));
        c.gradients.add(fade("Ground Haze", 0xFFE0BE92, 0.52f, 0.10f, 0, 0.8f));
        c.gradients.add(crown("Ochre Ceiling", 0xFF9C7A4E, 0.66f, 0.30f, 0, 0.7f));
        c.gradients.add(crown("Zenith Grey", 0xFF6E5A42, 0.88f, 0.16f, 0, 0.5f));
        addDeck(c, "Suspended Dust", 1, 0.55f, 400.0f, 0xFFD8B486, 0.45f).scale = 3.0f;
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 1.4f;
        c.rayleigh = 0.35f;
        c.mie = 2.2f;
        c.sunsetGlow = 0.7f;
        c.saturation = 0.85f;
        c.fogBlend = 0.55f;
        c.fogHeight = 0.16f;
        return c;
    }

    /** Sun barely dipping, circling the horizon instead of setting. Pale, flat, endless. */
    private static SkyConfig polarDay() {
        SkyConfig c = base("Midnight Sun");
        c.gradients.add(ramp("Pale Blue", 0xFF8FB4D8, 0xFF3D6A9E));
        c.gradients.add(fade("Gold Rim", 0xFFFFD79A, 0.515f, 0.06f, 0, 0.8f));
        c.gradients.add(fade("Cold Band", 0xFFAFCCE4, 0.56f, 0.14f, 0, 0.5f));
        c.gradients.add(crown("High Blue", 0xFF3A76B4, 0.70f, 0.28f, 0, 0.8f));
        c.gradients.add(crown("Zenith", 0xFF1E4E8E, 0.90f, 0.14f, 0, 0.6f));
        addDeck(c, "Thin Cirrus", 2, 0.26f, 800.0f, 0xFFFFF0DC, 0.45f).scale = 1.6f;
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.7f;
        c.sunsetGlow = 1.1f;
        c.animation.dayNightBlend = false;
        c.saturation = 0.95f;
        return c;
    }

    // -------------------------------------------------------------- weather

    /** The green-tinged sky under a rotating supercell, with the wall cloud low and dark. */
    private static SkyConfig supercell() {
        SkyConfig c = base("Supercell");
        c.gradients.add(ramp("Storm", 0xFF4A5A4E, 0xFF161C1E));
        c.gradients.add(fade("Hail Green", 0xFF7FA870, 0.53f, 0.10f, 0, 0.75f));
        c.gradients.add(fade("Wall Cloud", 0xFF2A2E33, 0.505f, 0.04f, 0, 0.9f));
        c.gradients.add(crown("Anvil Underside", 0xFF32383E, 0.64f, 0.26f, 0, 0.85f));
        c.gradients.add(crown("Anvil Top", 0xFF55606A, 0.86f, 0.18f, 0, 0.6f));
        deck(c).type = 3;
        deck(c).coverage = 0.78f;
        deck(c).quantity = 1.4f;
        deck(c).density = 1.5f;
        deck(c).color = 0xFF8A949E;
        deck(c).shadowColor = 0xFF1A1E24;
        deck(c).altitude = 180.0f;
        deck(c).opacity = 0.95f;
        deck(c).speed = 1.8f;
        deck(c).selfShadow = 0.9f;
        addDeck(c, "Anvil", 1, 0.85f, 760.0f, 0xFF6E7681, 0.7f).scale = 2.6f;
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 1.2f;
        c.saturation = 0.9f;
        c.exposure = 0.9f;
        c.fogBlend = 0.4f;
        return c;
    }

    /** Mammatus pouches on the back of an anvil, lit orange by a sun already on the horizon. */
    private static SkyConfig mammatus() {
        SkyConfig c = base("Mammatus");
        c.gradients.add(ramp("Bruise", 0xFF6B4A52, 0xFF241A26));
        c.gradients.add(fade("Ember Gap", 0xFFFF9A52, 0.515f, 0.045f, 0, 0.9f));
        c.gradients.add(crown("Pouch Shadow", 0xFF4A3038, 0.60f, 0.24f, 0, 0.85f));
        c.gradients.add(crown("Lit Underside", 0xFFC97A5A, 0.78f, 0.22f, 1, 0.5f));
        c.gradients.add(crown("Zenith Slate", 0xFF2E2230, 0.92f, 0.12f, 0, 0.6f));
        deck(c).type = 0;
        deck(c).coverage = 0.82f;
        deck(c).quantity = 1.8f;
        deck(c).density = 1.7f;
        deck(c).sharpness = 0.85f;
        deck(c).scale = 0.55f;
        deck(c).color = 0xFFFFB27A;
        deck(c).shadowColor = 0xFF3A2430;
        deck(c).altitude = 260.0f;
        deck(c).opacity = 0.9f;
        deck(c).selfShadow = 0.95f;
        deck(c).speed = 0.5f;
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.9f;
        c.sunsetGlow = 1.5f;
        c.saturation = 1.1f;
        return c;
    }

    /** Break in a monsoon: towering cumulus, hard sun, shafts through the gaps. */
    private static SkyConfig monsoonBreak() {
        SkyConfig c = base("Monsoon Break");
        c.gradients.add(ramp("Washed Blue", 0xFF6E9AC4, 0xFF234A80));
        c.gradients.add(fade("Rain Haze", 0xFFA8BCCE, 0.53f, 0.11f, 0, 0.6f));
        c.gradients.add(crown("High Blue", 0xFF2C5F9E, 0.70f, 0.28f, 0, 0.8f));
        c.gradients.add(crown("Zenith", 0xFF16407A, 0.90f, 0.14f, 0, 0.6f));
        deck(c).type = 0;
        deck(c).coverage = 0.62f;
        deck(c).quantity = 1.5f;
        deck(c).density = 1.4f;
        deck(c).altitude = 300.0f;
        deck(c).color = 0xFFF6F8FF;
        deck(c).shadowColor = 0xFF48566E;
        deck(c).opacity = 0.92f;
        deck(c).speed = 1.5f;
        deck(c).selfShadow = 0.8f;
        addDeck(c, "Towers", 0, 0.40f, 620.0f, 0xFFFFFFFF, 0.8f).scale = 0.7f;
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 1.0f;
        c.saturation = 1.05f;
        return c;
    }

    /** Heavy, contrasty overcast with light punching through one gap. The sky pack look. */
    private static SkyConfig dramaticOvercast() {
        SkyConfig c = base("Dramatic Overcast");
        c.gradients.add(ramp("Slate", 0xFF5A646E, 0xFF1C2228));
        c.gradients.add(fade("Bright Gap", 0xFFFFE6C0, 0.515f, 0.04f, 1, 0.55f));
        c.gradients.add(fade("Rain Curtain", 0xFF6E7A88, 0.55f, 0.12f, 0, 0.6f));
        c.gradients.add(crown("Ceiling", 0xFF39424C, 0.64f, 0.26f, 0, 0.9f));
        c.gradients.add(crown("Break Light", 0xFF8E9AA8, 0.84f, 0.20f, 1, 0.35f));
        deck(c).type = 1;
        deck(c).coverage = 0.88f;
        deck(c).quantity = 1.2f;
        deck(c).density = 1.6f;
        deck(c).altitude = 220.0f;
        deck(c).color = 0xFFAEB8C4;
        deck(c).shadowColor = 0xFF242A32;
        deck(c).opacity = 0.95f;
        deck(c).speed = 1.2f;
        deck(c).selfShadow = 0.85f;
        addDeck(c, "Scud", 1, 0.5f, 120.0f, 0xFF7E8894, 0.6f).speed = 2.4f;
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 1.1f;
        c.saturation = 0.85f;
        c.exposure = 0.95f;
        c.fogBlend = 0.45f;
        return c;
    }

    // ---------------------------------------------------------- night skies

    /** Atacama-class transparency: no moon, no light, and far more stars than you expect. */
    private static SkyConfig desertNight() {
        SkyConfig c = base("Desert Night");
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.12f;
        c.gradients.add(ramp("Night", 0xFF070C1C, 0xFF01020A));
        c.gradients.add(fade("Ground Glow", 0xFF1A2440, 0.52f, 0.08f, 1, 0.4f));
        c.gradients.add(crown("Zenith Depth", 0xFF060B1E, 0.72f, 0.28f, 0, 0.8f));
        c.stars.add(spin(star("Deep Field", 0xFFCFDCFF, 0.95f, 0.26f, 0.70f, 0.1f), 0.35f));
        c.stars.add(spin(zenithBand(star("Overhead", 0xFFFFFFFF, 0.80f, 0.34f, 1.05f, 0.7f)), 0.55f));
        c.stars.add(spin(bright(star("Named Stars", 0xFFFFF6E4, 0.10f, 0.85f, 1.8f, 2.6f)), 0.8f));
        c.nebulae.add(tilt(drift(neb("Faint Band", 0xFF9FA8D6, 0xFF3A4270, 0.28f, 3.4f, 1, 0.9f), 0.4f, 0.3f), 0.25f));
        c.exposure = 1.1f;
        return c;
    }

    /** The Sagittarius core overhead in July, with the dark dust lane splitting it. */
    private static SkyConfig milkyWayCore() {
        SkyConfig c = base("Milky Way Core");
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.15f;
        c.gradients.add(ramp("Night", 0xFF080F22, 0xFF01030B));
        c.gradients.add(crown("Zenith Wash", 0xFF141E46, 0.70f, 0.30f, 1, 0.45f));
        c.nebulae.add(tilt(drift(neb("Core", 0xFFF2DDB4, 0xFF8A6AC8, 1.15f, 3.0f, 1, 0.80f), 0.7f, 0.5f), 0.55f));
        c.nebulae.add(tilt(drift(neb("Dust Lane", 0xFF241708, 0xFF0E0906, 0.75f, 4.4f, 1, 0.95f), 0.7f, 0.42f), 0.52f));
        c.nebulae.add(tilt(drift(neb("Outer Arm", 0xFF6E86C8, 0xFF20305E, 0.45f, 5.0f, 1, 0.70f), 0.5f, 0.35f), 0.18f));
        c.stars.add(spin(star("Field", 0xFFCFE0FF, 0.92f, 0.28f, 0.80f, 0.1f), 0.4f));
        c.stars.add(band(spin(star("Core Stars", 0xFFFFF0D0, 0.95f, 0.34f, 1.05f, 0.4f), 0.5f), 0.55f, 2.4f));
        c.stars.add(spin(bright(star("Bright", 0xFFFFF7E0, 0.11f, 0.82f, 1.7f, 2.8f)), 0.9f));
        c.exposure = 1.08f;
        return c;
    }

    /** Sunlight scattered off interplanetary dust: a faint cone leaning up from the horizon. */
    private static SkyConfig zodiacalLight() {
        SkyConfig c = base("Zodiacal Light");
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.18f;
        c.gradients.add(ramp("Night", 0xFF070D1E, 0xFF01020A));
        c.gradients.add(crown("Dust Cone", 0xFF3E3A52, 0.56f, 0.30f, 1, 0.42f));
        c.gradients.add(crown("Cone Tip", 0xFF2A2740, 0.82f, 0.20f, 1, 0.3f));
        c.gradients.add(crown("Zenith", 0xFF060A1A, 0.94f, 0.10f, 0, 0.5f));
        c.stars.add(spin(star("Field", 0xFFC8D8FF, 0.85f, 0.28f, 0.72f, 0.1f), 0.35f));
        c.stars.add(spin(zenithBand(star("Overhead", 0xFFEFF4FF, 0.55f, 0.36f, 0.95f, 0.6f)), 0.5f));
        c.stars.add(spin(bright(star("Bright", 0xFFFFF4DC, 0.09f, 0.78f, 1.5f, 2.4f)), 0.8f));
        c.exposure = 1.1f;
        return c;
    }

    /** Green airglow banding, the reason a truly dark sky is never actually black. */
    private static SkyConfig airglow() {
        SkyConfig c = base("Airglow");
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.16f;
        c.gradients.add(ramp("Night", 0xFF06101A, 0xFF010508));
        c.gradients.add(fade("Green Band", 0xFF1E5E4A, 0.54f, 0.12f, 1, 0.5f));
        c.gradients.add(crown("High Airglow", 0xFF14483C, 0.70f, 0.24f, 1, 0.40f));
        c.gradients.add(crown("Red Layer", 0xFF4A2030, 0.88f, 0.16f, 1, 0.28f));
        c.stars.add(spin(star("Field", 0xFFCFE0FF, 0.88f, 0.28f, 0.75f, 0.1f), 0.35f));
        c.stars.add(spin(zenithBand(star("Overhead", 0xFFFFFFFF, 0.60f, 0.36f, 1.0f, 0.6f)), 0.5f));
        c.stars.add(spin(bright(star("Bright", 0xFFFFF7E8, 0.10f, 0.8f, 1.6f, 2.5f)), 0.85f));
        c.exposure = 1.12f;
        return c;
    }

    /**
     * Mesospheric ice at 82km, lit by a sun well below the horizon. Only visible in deep
     * twilight at high latitude, and unmistakable when it is: electric blue, and moving.
     */
    private static SkyConfig noctilucent() {
        SkyConfig c = base("Noctilucent");
        c.gradients.add(ramp("Twilight", 0xFF122A55, 0xFF040B20));
        c.gradients.add(fade("Horizon Glow", 0xFF3E7AA8, 0.52f, 0.08f, 0, 0.7f));
        c.gradients.add(crown("Upper Blue", 0xFF0C2148, 0.68f, 0.30f, 0, 0.85f));
        c.gradients.add(crown("Zenith", 0xFF061434, 0.90f, 0.14f, 0, 0.6f));
        deck(c).type = 2;
        deck(c).coverage = 0.34f;
        deck(c).quantity = 1.6f;
        deck(c).scale = 0.5f;
        deck(c).sharpness = 0.9f;
        deck(c).color = 0xFF9FE4FF;
        deck(c).shadowColor = 0xFF1A4A7A;
        deck(c).altitude = 900.0f;
        deck(c).opacity = 0.8f;
        deck(c).speed = 1.1f;
        deck(c).evolveSpeed = 0.5f;
        c.stars.add(spin(zenithBand(star("Twilight Stars", 0xFFE8F0FF, 0.30f, 0.40f, 0.6f, 0.4f)), 0.45f));
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.4f;
        c.saturation = 1.1f;
        return c;
    }

    /** Totality: corona out, planets visible, and a 360 degree sunset around the horizon. */
    private static SkyConfig totalEclipse() {
        SkyConfig c = base("Total Eclipse");
        c.gradients.add(ramp("Dark Day", 0xFF1A2440, 0xFF060A18));
        c.gradients.add(fade("Ring Horizon", 0xFFE0894A, 0.515f, 0.055f, 0, 0.9f));
        c.gradients.add(fade("Cold Wash", 0xFF2E4270, 0.57f, 0.14f, 0, 0.7f));
        c.gradients.add(crown("Dark Ceiling", 0xFF0A1024, 0.68f, 0.30f, 0, 0.9f));
        c.gradients.add(crown("Zenith Black", 0xFF04060E, 0.90f, 0.14f, 0, 0.7f));
        c.bodies.add(orbit(glow(body("Corona", 0xFFFFFFFF, 0.0f, 0.55f, 0.030f, 2), 1.0f), 0.05f, 0.0f));
        c.bodies.add(orbit(body("Occulting Disc", 0xFF000000, 0.0f, 0.55f, 0.026f, 0), 0.05f, 0.0f));
        c.bodies.add(orbit(glow(body("Venus", 0xFFFFF4DC, -1.5f, 0.30f, 0.010f, 2), 0.7f), 0.05f, 0.1f));
        c.stars.add(spin(zenithBand(star("Daytime Stars", 0xFFE8F0FF, 0.22f, 0.44f, 0.8f, 0.5f)), 0.4f));
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.45f;
        c.sunsetGlow = 1.8f;
        c.animation.dayNightBlend = false;
        c.exposure = 1.0f;
        return c;
    }

    // ------------------------------------------------------------- aurorae

    /** A Kp7 storm: the arc overhead, folded into curls, with the red tops that only show when it is strong. */
    private static SkyConfig auroraStorm() {
        SkyConfig c = base("Aurora Storm");
        c.gradients.add(ramp("Night", 0xFF07122A, 0xFF01040E));
        c.gradients.add(crown("Zenith Depth", 0xFF0A1838, 0.72f, 0.28f, 0, 0.7f));
        c.stars.add(spin(star("Field", 0xFFDDE9FF, 0.55f, 0.32f, 0.75f, 0.2f), 0.4f));
        c.stars.add(spin(bright(star("Bright", 0xFFFFFFFF, 0.12f, 0.78f, 1.4f, 2.4f)), 0.7f));

        AuroraLayer main = borealis("Main Arc", 0xFFFF4FA8, 0xFF39FFA6, 0xFFB03CFF, 1.0f);
        main.arcDistance = 520.0f;
        main.arcWidth = 110.0f;
        main.curvature = 0.62f;
        main.bands = 3;
        main.bandSpacing = 0.7f;
        main.foldAmount = 1.7f;
        main.foldScale = 1.4f;
        main.foldSpeed = 1.2f;
        main.rayAmount = 0.9f;
        main.rayScale = 1.4f;
        main.raySharpness = 0.7f;
        main.pulse = 0.4f;
        main.speed = 0.7f;
        main.groundGlow = 0.5f;
        main.steps = 12;
        c.aurorae.add(main);

        AuroraLayer second = borealis("Poleward Arc", 0xFFFF6FC0, 0xFF6FFFD0, 0xFFFF4F6F, 0.45f);
        second.arcAzimuth = 20.0f;
        second.arcDistance = 1600.0f;
        second.arcWidth = 180.0f;
        second.curvature = 0.18f;
        second.foldAmount = 0.8f;
        second.rayAmount = 0.6f;
        second.groundGlow = 0.0f;
        second.speed = 0.4f;
        second.steps = 8;
        c.aurorae.add(second);

        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.2f;
        return c;
    }

    /** A quiet arc low on the northern horizon, which is what most nights actually look like. */
    private static SkyConfig quietArc() {
        SkyConfig c = base("Quiet Arc");
        c.gradients.add(ramp("Night", 0xFF060E22, 0xFF01030C));
        c.gradients.add(crown("Zenith", 0xFF0A1530, 0.72f, 0.28f, 0, 0.7f));
        c.stars.add(spin(star("Field", 0xFFCFE0FF, 0.80f, 0.28f, 0.80f, 0.1f), 0.35f));
        c.stars.add(spin(zenithBand(star("Overhead", 0xFFFFFFFF, 0.55f, 0.34f, 1.0f, 0.6f)), 0.5f));
        c.stars.add(spin(bright(star("Bright", 0xFFFFF7E8, 0.10f, 0.8f, 1.5f, 2.4f)), 0.8f));
        AuroraLayer a = borealis("Northern Arc", 0xFFE06AA0, 0xFF3FFFA0, 0xFF6F7AE0, 0.6f);
        a.arcDistance = 2100.0f;
        a.arcWidth = 90.0f;
        a.curvature = 0.10f;
        a.bands = 1;
        a.foldAmount = 0.45f;
        a.foldScale = 0.6f;
        a.rayAmount = 0.75f;
        a.rayScale = 1.1f;
        a.pulse = 0.12f;
        a.speed = 0.25f;
        a.topHeight = 380.0f;
        a.groundGlow = 0.25f;
        a.steps = 9;
        c.aurorae.add(a);
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.18f;
        return c;
    }

    /** The southern lights: the same physics, and the deep reds show more often. */
    private static SkyConfig auroraAustralis() {
        SkyConfig c = base("Aurora Australis");
        c.gradients.add(ramp("Night", 0xFF08101F, 0xFF01030A));
        c.gradients.add(crown("Zenith", 0xFF101A38, 0.72f, 0.28f, 0, 0.7f));
        c.stars.add(spin(star("Southern Field", 0xFFD8E4FF, 0.85f, 0.30f, 0.82f, 0.15f), 0.4f));
        c.stars.add(spin(bright(star("Crux", 0xFFFFFFFF, 0.11f, 0.82f, 1.6f, 2.6f)), 0.75f));
        c.nebulae.add(tilt(drift(neb("Magellanic", 0xFFBFC8E8, 0xFF3A4472, 0.35f, 6.0f, 0, 0.8f), 0.4f, 0.45f), 0.40f));
        AuroraLayer a = borealis("Southern Arc", 0xFFFF5A7A, 0xFF4FFFB0, 0xFFE8385A, 0.85f);
        a.arcAzimuth = 180.0f;
        a.arcDistance = 900.0f;
        a.arcWidth = 130.0f;
        a.curvature = 0.42f;
        a.bands = 2;
        a.foldAmount = 1.3f;
        a.rayAmount = 0.85f;
        a.topFalloff = 0.35f;
        a.topHeight = 760.0f;
        a.pulse = 0.3f;
        a.speed = 0.55f;
        a.groundGlow = 0.4f;
        a.steps = 11;
        c.aurorae.add(a);
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.2f;
        return c;
    }

    // ------------------------------------------------------- pack-flavoured

    /** Violet deep space with the nebula overhead rather than parked on the horizon. */
    private static SkyConfig violetCosmos() {
        SkyConfig c = base("Violet Cosmos");
        c.atmosphereEnabled = false;
        c.exposure = 1.1f;
        c.gradients.add(ramp("Void", 0xFF150A32, 0xFF03020C));
        c.gradients.add(fade("Rim", 0xFF4A2A8E, 0.54f, 0.14f, 1, 0.5f));
        c.gradients.add(crown("Overhead Violet", 0xFF2E1470, 0.66f, 0.30f, 1, 0.55f));
        c.gradients.add(crown("Zenith Bloom", 0xFF4A2098, 0.86f, 0.18f, 1, 0.4f));
        c.nebulae.add(tilt(drift(neb("Crown", 0xFFB45CFF, 0xFF3C2AA8, 1.05f, 2.4f, 0, 0.7f), 0.9f, 0.55f), 0.55f));
        c.nebulae.add(tilt(drift(neb("Cyan Wisp", 0xFF3CE0E8, 0xFF1A4A9E, 0.55f, 4.2f, 3, 0.6f), -0.7f, 0.65f), 0.30f));
        c.nebulae.add(drift(neb("Deep Dust", 0xFF6A1F7A, 0xFF1A0A2E, 0.45f, 5.4f, 0, 0.8f), 0.4f, 0.3f));
        c.stars.add(spin(star("Deep Field", 0xFFD6D0FF, 0.90f, 0.28f, 0.80f, 0.1f), 0.4f));
        c.stars.add(spin(zenithBand(star("Overhead", 0xFFFFFFFF, 0.60f, 0.38f, 1.15f, 0.8f)), 0.7f));
        c.stars.add(spin(bright(star("Foreground", 0xFFFFE8FF, 0.12f, 0.85f, 1.7f, 2.8f)), 1.1f));
        c.bodies.add(orbit(ringed(body("Ringed Giant", 0xFF9B6BE8, 1.9f, 0.78f, 0.090f, 1), 0.85f, 0.28f), 0.26f, 0.22f));
        c.bodies.add(orbit(glow(body("Pulsar", 0xFFFFFFFF, 2.6f, 1.25f, 0.009f, 2), 0.9f), 0.2f, 0.1f));
        c.saturation = 1.15f;
        return c;
    }

    /** Teal and cyan, lit like light through water. */
    private static SkyConfig deepAqua() {
        SkyConfig c = base("Deep Aqua");
        c.atmosphereEnabled = false;
        c.gradients.add(ramp("Depth", 0xFF07303E, 0xFF011018));
        c.gradients.add(fade("Shallow Rim", 0xFF2FA8B4, 0.54f, 0.15f, 1, 0.55f));
        c.gradients.add(crown("Overhead Light", 0xFF1E7A96, 0.66f, 0.30f, 1, 0.5f));
        c.gradients.add(crown("Surface Shimmer", 0xFF6FE8DC, 0.88f, 0.16f, 1, 0.38f));
        c.nebulae.add(tilt(drift(neb("Caustic Veil", 0xFF6FF0E0, 0xFF12566E, 0.80f, 3.0f, 3, 0.65f), 0.9f, 0.7f), 0.50f));
        c.nebulae.add(tilt(drift(neb("Plankton Drift", 0xFFB4FFE8, 0xFF0E3A4E, 0.40f, 5.6f, 0, 0.55f), -0.6f, 0.5f), 0.20f));
        c.stars.add(spin(star("Motes", 0xFFCFFFF6, 0.70f, 0.30f, 0.70f, 0.2f), 0.5f));
        c.stars.add(spin(zenithBand(shape(star("Sparks", 0xFFFFFFFF, 0.30f, 0.55f, 1.2f, 1.2f), 3)), 0.9f));
        c.bodies.add(orbit(glow(body("Sunken Orb", 0xFF9FF4FF, 1.2f, 0.85f, 0.055f, 2), 0.5f), 0.18f, 0.3f));
        c.saturation = 1.2f;
        c.exposure = 1.05f;
        return c;
    }

    /** Orange and red deep space, with the dust thick enough to hide half the stars. */
    private static SkyConfig emberNebula() {
        SkyConfig c = base("Ember Nebula");
        c.atmosphereEnabled = false;
        c.gradients.add(ramp("Char", 0xFF2A0F0A, 0xFF080202));
        c.gradients.add(fade("Ember Rim", 0xFF8E2A14, 0.54f, 0.14f, 1, 0.5f));
        c.gradients.add(crown("Overhead Burn", 0xFF6E1C10, 0.66f, 0.30f, 1, 0.5f));
        c.gradients.add(crown("Zenith Gold", 0xFFB8641E, 0.88f, 0.16f, 1, 0.35f));
        c.nebulae.add(tilt(drift(neb("Furnace", 0xFFFF8A3C, 0xFF8E1A2E, 1.10f, 2.6f, 0, 0.7f), 0.8f, 0.6f), 0.50f));
        c.nebulae.add(tilt(drift(neb("Soot", 0xFF200C08, 0xFF080302, 0.70f, 4.0f, 3, 0.9f), 0.5f, 0.45f), 0.35f));
        c.stars.add(spin(star("Field", 0xFFFFD8B4, 0.75f, 0.28f, 0.75f, 0.1f), 0.4f));
        c.stars.add(spin(zenithBand(star("Overhead", 0xFFFFF0DC, 0.50f, 0.36f, 1.1f, 0.8f)), 0.65f));
        c.stars.add(spin(bright(star("Hot Stars", 0xFFFFFFFF, 0.10f, 0.85f, 1.8f, 2.8f)), 1.0f));
        c.bodies.add(orbit(glow(body("Protostar", 0xFFFFB05A, 2.1f, 0.95f, 0.045f, 2), 0.8f), 0.22f, 0.25f));
        c.saturation = 1.15f;
        c.exposure = 1.05f;
        return c;
    }

    /** A habitat ring arcing right over the top of the sky. Everything about it is overhead. */
    private static SkyConfig ringworld() {
        SkyConfig c = base("Ringworld");
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 0.3f;
        c.gradients.add(ramp("Night", 0xFF0A1024, 0xFF02040E));
        c.gradients.add(crown("Ring Glow", 0xFF3A4E8E, 0.74f, 0.26f, 1, 0.45f));
        c.stars.add(spin(star("Field", 0xFFCFE0FF, 0.70f, 0.30f, 0.75f, 0.2f), 0.4f));
        c.stars.add(spin(bright(star("Bright", 0xFFFFFFFF, 0.10f, 0.8f, 1.5f, 2.4f)), 0.8f));
        BodyLayer ring = ringed(body("The Ring", 0xFF8FA8D8, 0.0f, 1.45f, 0.30f, 1), 1.4f, 1.45f);
        ring.motion = BodyLayer.MOTION_FIXED;
        ring.intensity = 0.9f;
        ring.detail = 0.8f;
        ring.glow = 0.2f;
        c.bodies.add(ring);
        c.bodies.add(orbit(body("Shepherd Moon", 0xFFD8E4FF, 1.9f, 0.70f, 0.030f, 4), 0.35f, 0.3f));
        c.bodies.add(orbit(glow(body("Arc Light", 0xFFFFF0C8, -1.2f, 1.10f, 0.014f, 2), 0.7f), 0.28f, -0.2f));
        return c;
    }

    /** Two suns of different colour, which is what most star systems actually have. */
    private static SkyConfig twinSuns() {
        SkyConfig c = base("Twin Suns");
        c.gradients.add(ramp("Dust Sky", 0xFFB8875A, 0xFF4A2E1E));
        c.gradients.add(fade("Low Haze", 0xFFE8B472, 0.52f, 0.10f, 0, 0.8f));
        c.gradients.add(crown("Upper Rust", 0xFF7A4A30, 0.68f, 0.28f, 0, 0.8f));
        c.gradients.add(crown("Zenith Violet", 0xFF3E2A4A, 0.90f, 0.14f, 0, 0.6f));
        c.bodies.add(orbit(glow(body("Primary", 0xFFFFE0A0, 0.35f, 0.42f, 0.055f, 2), 0.8f), 0.18f, 0.08f));
        c.bodies.add(orbit(glow(body("Companion", 0xFFFF9A6A, 0.72f, 0.30f, 0.030f, 2), 0.7f), 0.18f, 0.08f));
        c.bodies.add(orbit(body("Inner World", 0xFFB49A7A, 2.4f, 0.95f, 0.055f, 0), 0.30f, 0.35f));
        c.stars.add(spin(zenithBand(star("Day Stars", 0xFFFFE8D0, 0.25f, 0.40f, 0.55f, 0.4f)), 0.4f));
        c.atmosphereEnabled = true;
        c.atmosphereDensity = 1.1f;
        c.rayleigh = 0.5f;
        c.mie = 1.8f;
        c.sunsetGlow = 1.3f;
        c.saturation = 1.05f;
        return c;
    }

    /** Looking up from inside a gas giant's shadow, with the planet filling half the sky. */
    private static SkyConfig giantsShadow() {
        SkyConfig c = base("Giant's Shadow");
        c.atmosphereEnabled = false;
        c.gradients.add(ramp("Shadow", 0xFF0E0A1E, 0xFF020208));
        c.gradients.add(crown("Reflected Light", 0xFF4A3A6E, 0.70f, 0.30f, 1, 0.4f));
        c.stars.add(spin(star("Field", 0xFFCFE0FF, 0.65f, 0.28f, 0.70f, 0.2f), 0.35f));
        c.stars.add(spin(bright(star("Bright", 0xFFFFFFFF, 0.09f, 0.8f, 1.4f, 2.2f)), 0.7f));
        BodyLayer giant = ringed(body("The Giant", 0xFFD8A874, 0.4f, 1.15f, 0.42f, 1), 1.2f, 0.55f);
        giant.motion = BodyLayer.MOTION_DRIFT;
        giant.orbitSpeed = 0.05f;
        giant.detail = 0.95f;
        giant.spinSpeed = 0.12f;
        giant.shading = 0.65f;
        giant.glow = 0.15f;
        c.bodies.add(giant);
        c.bodies.add(orbit(body("Sister Moon", 0xFFB8C4D8, 2.7f, 0.55f, 0.038f, 4), 0.40f, 0.25f));
        c.exposure = 1.05f;
        return c;
    }

    /** Neon grid dusk. Not real, but it is the look people ask for by name. */
    private static SkyConfig synthwave() {
        SkyConfig c = base("Synthwave");
        c.atmosphereEnabled = false;
        c.exposure = 1.05f;
        c.gradients.add(ramp("Top", 0xFF6E1A8C, 0xFF10052E));
        c.gradients.add(fade("Magenta Band", 0xFFD6207E, 0.56f, 0.12f, 0, 0.85f));
        c.gradients.add(fade("Sunset Strip", 0xFFFF9A3C, 0.51f, 0.045f, 0, 0.90f));
        c.gradients.add(crown("Grid Ceiling", 0xFF4A1287, 0.68f, 0.30f, 1, 0.55f));
        c.gradients.add(crown("Zenith Cyan", 0xFF00C0D8, 0.88f, 0.16f, 1, 0.3f));
        c.stars.add(spin(star("Grid Stars", 0xFF9BF7FF, 0.30f, 0.45f, 1.0f, 0.6f), 1.0f));
        c.stars.add(spin(zenithBand(shape(star("Neon Sparks", 0xFFFF4FA8, 0.14f, 0.9f, 1.4f, 2.0f), 3)), 1.6f));
        c.bodies.add(glow(body("Neon Sun", 0xFFFF4FA8, 0.0f, 0.10f, 0.13f, 2), 0.55f));
        c.bodies.add(orbit(ringed(body("Chrome Giant", 0xFF8B6BFF, 2.4f, 1.05f, 0.075f, 1), 0.9f, 0.22f), 0.45f, 0.5f));
        c.saturation = 1.30f;
        return c;
    }

    /** The default: a believable night that still reads as SkyJet rather than as vanilla. */
    private static SkyConfig skyJetDefault() {
        SkyConfig c = base("SkyJet Default");
        c.gradients.add(ramp("Night Base", 0xFF060B1A, 0xFF030610));
        c.gradients.add(fade("Horizon Lift", 0xFF14305E, 0.55f, 0.16f, 0, 0.75f));
        c.gradients.add(crown("Zenith Bloom", 0xFF141F4E, 0.70f, 0.28f, 1, 0.45f));
        c.gradients.add(crown("Overhead Wash", 0xFF1C2C66, 0.88f, 0.16f, 1, 0.28f));
        c.stars.add(spin(star("Distant", 0xFFBFD4FF, 0.78f, 0.32f, 0.78f, 0.15f), 0.5f));
        c.stars.add(spin(zenithBand(star("Overhead", 0xFFE8F0FF, 0.55f, 0.42f, 1.15f, 0.7f)), 0.7f));
        c.stars.add(spin(bright(star("Near", 0xFFFFFFFF, 0.20f, 0.70f, 1.45f, 2.0f)), 0.9f));
        c.nebulae.add(tilt(drift(neb("Overhead Band", 0xFF8A98DC, 0xFF1E2450, 0.62f, 3.0f, 1, 0.82f), 0.5f, 0.4f), 0.45f));
        c.nebulae.add(tilt(drift(neb("Zenith Wisp", 0xFF6ED0E0, 0xFF16214A, 0.34f, 5.2f, 0, 0.6f), -0.4f, 0.55f), 0.60f));
        c.bodies.add(orbit(body("Moon", 0xFFE8EEFF, 1.4f, 0.65f, 0.045f, 4), 0.22f, 0.25f));
        c.atmosphereEnabled = true;
        c.rayleigh = 1.0f;
        c.mie = 1.0f;
        c.sunsetGlow = 1.0f;
        c.fogBlend = 0.30f;
        return c;
    }

    /** Borealis-style aurora with the three altitude colours already set. */
    private static AuroraLayer borealis(String name, int low, int body, int top, float intensity) {
        AuroraLayer a = new AuroraLayer();
        a.name = name;
        a.style = AuroraLayer.STYLE_BOREALIS;
        a.colorLow = low;
        a.colorBase = body;
        a.colorTip = top;
        a.intensity = intensity;
        return a;
    }

    // ------------------------------------------------------------- helpers

    /** Opaque base ramp across the whole sky: {@code low} near the horizon, {@code high} at the zenith. */
    private static GradientLayer ramp(String name, int low, int high) {
        GradientLayer g = new GradientLayer(name, low, high, 0.62f, 0.45f);
        g.blendMode = 0;
        g.opacity = 1.0f;
        return g;
    }

    /**
     * Accent band that is solid below {@code position} and fades to nothing above it, so it
     * layers on top of the base ramp instead of replacing it.
     */
    private static GradientLayer fade(String name, int color, float position, float softness, int blend, float opacity) {
        GradientLayer g = new GradientLayer(name, color, color & 0x00FFFFFF, position, softness);
        g.blendMode = blend;
        g.opacity = opacity;
        return g;
    }

    /**
     * The mirror of {@link #fade}: transparent below {@code position} and solid above it. This is
     * what stops a preset from being a bright band at eye level and nothing overhead.
     */
    private static GradientLayer crown(String name, int color, float position, float softness, int blend, float opacity) {
        GradientLayer g = new GradientLayer(name, color & 0x00FFFFFF, color, position, softness);
        g.blendMode = blend;
        g.opacity = opacity;
        return g;
    }

    private static StarLayer star(String name, int color, float density, float size, float brightness, float depth) {
        return new StarLayer(name, color, density, size, brightness, depth);
    }

    private static StarLayer bright(StarLayer s) {
        s.shape = 1;
        s.twinkleAmount = 0.5f;
        s.glow = 0.35f;
        return s;
    }

    private static StarLayer shape(StarLayer s, int shape) {
        s.shape = shape;
        return s;
    }

    private static int seedCounter;

    private static StarLayer spin(StarLayer s, float rotationSpeed) {
        s.rotationSpeed = rotationSpeed;
        s.driftSpeed = rotationSpeed * 0.15f;
        // Fields that share a seed put their stars in the same cells, which reads as one pattern
        // repeated rather than a deep sky, so every authored layer gets its own.
        if (s.seed == 0) s.seed = ++seedCounter;
        return s;
    }

    /** Pushes a field into a tight cluster near the zenith, so looking up is not empty. */
    private static StarLayer zenithBand(StarLayer s) {
        s.bandBias = 0.82f;
        s.bandTightness = 1.4f;
        return s;
    }

    private static StarLayer seed(StarLayer s, int value) {
        s.seed = value;
        return s;
    }

    private static StarLayer freeze(StarLayer s) {
        s.frozen = true;
        s.twinkleAmount = 0.0f;
        s.rotationSpeed = 0.0f;
        s.driftSpeed = 0.0f;
        return s;
    }

    private static StarLayer band(StarLayer s, float bias, float tightness) {
        s.bandBias = bias;
        s.bandTightness = tightness;
        return s;
    }

    private static NebulaLayer neb(String name, int a, int b, float intensity, float scale, int shape, float contrast) {
        NebulaLayer n = new NebulaLayer(name, a, b, intensity, scale, shape);
        n.contrast = contrast;
        return n;
    }

    private static NebulaLayer drift(NebulaLayer n, float speed, float warp) {
        n.driftSpeed = speed;
        n.warp = warp;
        return n;
    }

    private static NebulaLayer tilt(NebulaLayer n, float bias) {
        n.elevationBias = bias;
        return n;
    }

    private static BodyLayer body(String name, int color, float az, float el, float size, int type) {
        BodyLayer b = new BodyLayer(name, color, az, el, size, type);
        b.motion = BodyLayer.MOTION_FIXED;
        b.orbitSpeed = 0.0f;
        return b;
    }

    private static BodyLayer orbit(BodyLayer b, float speed, float tilt) {
        b.motion = BodyLayer.MOTION_ORBIT;
        b.orbitSpeed = speed;
        b.orbitTilt = tilt;
        b.spinSpeed = 0.25f;
        return b;
    }

    private static BodyLayer glow(BodyLayer b, float glow) {
        b.glow = glow;
        return b;
    }

    /** First cloud deck of a preset, created on demand. */
    private static CloudLayer deck(SkyConfig c) {
        if (c.clouds.isEmpty()) {
            c.clouds.add(new CloudLayer("Clouds", 0, 0.45f, 220.0f));
        }
        return c.clouds.get(0);
    }

    /** An additional deck, for skies that want weather at more than one altitude. */
    private static CloudLayer addDeck(SkyConfig c, String name, int type, float coverage,
                                      float altitude, int color, float opacity) {
        CloudLayer d = new CloudLayer(name, type, coverage, altitude);
        d.color = color;
        d.opacity = opacity;
        c.clouds.add(d);
        return d;
    }

    private static BodyLayer ringed(BodyLayer b, float ringWidth, float ringTilt) {
        b.ringWidth = ringWidth;
        b.ringTilt = ringTilt;
        return b;
    }
}
