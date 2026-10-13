package dev.skyjet.gui;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    /** Returns {hue 0-1, saturation 0-1, value 0-1}. */
    public static float[] toHsv(int color) {
        float r = red(color) / 255.0f;
        float g = green(color) / 255.0f;
        float b = blue(color) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h = 0.0f;
        if (delta > 1.0e-5f) {
            if (max == r) {
                h = ((g - b) / delta) % 6.0f;
            } else if (max == g) {
                h = (b - r) / delta + 2.0f;
            } else {
                h = (r - g) / delta + 4.0f;
            }
            h /= 6.0f;
            if (h < 0.0f) h += 1.0f;
        }
        float s = max <= 1.0e-5f ? 0.0f : delta / max;
        return new float[]{h, s, max};
    }

    public static int fromHsv(float h, float s, float v, int alpha) {
        h = h - (float) Math.floor(h);
        float c = v * s;
        float x = c * (1.0f - Math.abs((h * 6.0f) % 2.0f - 1.0f));
        float m = v - c;
        float r, g, b;
        int sector = (int) (h * 6.0f) % 6;
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return argb(alpha, Math.round((r + m) * 255.0f), Math.round((g + m) * 255.0f), Math.round((b + m) * 255.0f));
    }

    public static String toHex(int color, boolean withAlpha) {
        if (withAlpha) {
            return String.format("%08X", color);
        }
        return String.format("%06X", color & 0xFFFFFF);
    }

    public static Integer parseHex(String text, int fallbackAlpha) {
        if (text == null) return null;
        String s = text.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        if (!s.matches("[0-9a-fA-F]+")) return null;
        try {
            if (s.length() == 3) {
                int r = Integer.parseInt(s.substring(0, 1).repeat(2), 16);
                int g = Integer.parseInt(s.substring(1, 2).repeat(2), 16);
                int b = Integer.parseInt(s.substring(2, 3).repeat(2), 16);
                return argb(fallbackAlpha, r, g, b);
            }
            if (s.length() == 6) {
                return (fallbackAlpha << 24) | (int) Long.parseLong(s, 16);
            }
            if (s.length() == 8) {
                return (int) Long.parseLong(s, 16);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    public static boolean isHexCandidate(String text) {
        String s = text.startsWith("#") ? text.substring(1) : text;
        return s.length() <= 8 && s.matches("[0-9a-fA-F]*");
    }
}
