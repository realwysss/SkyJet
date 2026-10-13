package dev.skyjet.config;

import java.util.ArrayList;
import java.util.List;

/**
 * The order and global treatment of the sky's layer categories.
 *
 * <p>Every category is evaluated once and then composited in the order listed here, each with its
 * own opacity, tint and blend mode. That is what lets a nebula sit in front of a skybox in one
 * profile and behind it in another without touching either layer's own settings.
 */
public final class LayerStack {
    public static final int GRADIENTS = 0;
    public static final int SKYBOX = 1;
    public static final int ATMOSPHERE = 2;
    public static final int COSMOS = 3;
    public static final int STARS = 4;
    public static final int BODIES = 5;
    public static final int SUN_MOON = 6;
    public static final int AURORA = 7;
    public static final int CLOUDS = 8;
    public static final int COUNT = 9;

    public static final String[] CATEGORY_NAMES = {
            "Gradients", "Skybox", "Atmosphere", "Cosmos", "Stars",
            "Bodies", "Sun & Moon", "Aurora", "Clouds"
    };

    /** Speed multiplier applied to every animated layer at once. */
    public float animationSpeed = 1.0f;
    /** Tint multiplied over the whole composited sky. */
    public int masterTint = 0xFFFFFFFF;

    public List<Entry> entries = new ArrayList<>();

    public static final class Entry {
        public int category;
        public boolean enabled = true;
        public float opacity = 1.0f;
        public int tint = 0xFFFFFFFF;
        public int blendMode = -1;

        public Entry() {
        }

        public Entry(int category) {
            this.category = category;
        }

        public Entry copy() {
            Entry e = new Entry();
            e.category = category;
            e.enabled = enabled;
            e.opacity = opacity;
            e.tint = tint;
            e.blendMode = blendMode;
            return e;
        }
    }

    /** The default draw order, which is also the order the sky reads back to front. */
    public static LayerStack defaults() {
        LayerStack s = new LayerStack();
        int[] order = {GRADIENTS, SKYBOX, ATMOSPHERE, COSMOS, STARS, BODIES, SUN_MOON, AURORA, CLOUDS};
        for (int c : order) {
            s.entries.add(new Entry(c));
        }
        return s;
    }

    /**
     * Fills in anything a hand-edited or older config left out, so a missing category cannot
     * silently stop rendering.
     */
    public void sanitize() {
        if (entries == null) entries = new ArrayList<>();
        entries.removeIf(e -> e == null || e.category < 0 || e.category >= COUNT);
        boolean[] seen = new boolean[COUNT];
        List<Entry> unique = new ArrayList<>();
        for (Entry e : entries) {
            if (seen[e.category]) continue;
            seen[e.category] = true;
            unique.add(e);
        }
        entries = unique;
        for (Entry d : defaults().entries) {
            if (!seen[d.category]) entries.add(d);
        }
    }

    public LayerStack copy() {
        LayerStack s = new LayerStack();
        s.animationSpeed = animationSpeed;
        s.masterTint = masterTint;
        s.entries = new ArrayList<>();
        for (Entry e : entries) s.entries.add(e.copy());
        return s;
    }
}
