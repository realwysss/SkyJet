package dev.skyjet.gui;

public final class Theme {
    public static final int BACKDROP = 0xD805070C;
    public static final int WINDOW = 0xFF0D1017;
    public static final int WINDOW_ALT = 0xFF111621;
    public static final int PANEL = 0xFF141A26;
    public static final int PANEL_HOVER = 0xFF1B2333;
    public static final int PANEL_ACTIVE = 0xFF212C40;
    public static final int SIDEBAR = 0xFF0A0D14;
    public static final int BORDER = 0xFF232C3C;
    public static final int BORDER_SOFT = 0xFF1A2130;

    public static final int ACCENT = 0xFF4EA1FF;
    public static final int ACCENT_ALT = 0xFF8B6BFF;
    public static final int ACCENT_DIM = 0x554EA1FF;
    public static final int DANGER = 0xFFE05A5A;
    public static final int SUCCESS = 0xFF48D18A;

    public static final int TEXT = 0xFFE6EAF2;
    public static final int TEXT_DIM = 0xFF8B94A7;
    public static final int TEXT_FAINT = 0xFF5C6577;

    public static final int TRACK = 0xFF1A2130;
    public static final int TRACK_FILL = 0xFF4EA1FF;

    private Theme() {
    }

    public static int withAlpha(int color, float alpha) {
        int a = (int) (Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}
