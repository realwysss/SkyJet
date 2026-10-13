package dev.skyjet.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;

//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * Wraps the vanilla GUI draw target so the rest of the SkyJet interface stays version agnostic.
 * The class was renamed from {@code GuiGraphics} to {@code GuiGraphicsExtractor} in 26.1.
 */
public final class Gfx {
    //? if >=26 {
    public final GuiGraphicsExtractor raw;

    public Gfx(GuiGraphicsExtractor raw) {
        this.raw = raw;
    }
    //?} else {
    /*public final GuiGraphics raw;

    public Gfx(GuiGraphics raw) {
        this.raw = raw;
    }
    *///?}

    public final Font font = Minecraft.getInstance().font;

    public void fill(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        raw.fill(x, y, x + width, y + height, color);
    }

    public void gradient(int x, int y, int width, int height, int top, int bottom) {
        if (width <= 0 || height <= 0) return;
        raw.fillGradient(x, y, x + width, y + height, top, bottom);
    }

    public void outline(int x, int y, int width, int height, int color) {
        fill(x, y, width, 1, color);
        fill(x, y + height - 1, width, 1, color);
        fill(x, y + 1, 1, height - 2, color);
        fill(x + width - 1, y + 1, 1, height - 2, color);
    }

    public void panel(int x, int y, int width, int height, int fill, int border) {
        fill(x, y, width, height, fill);
        outline(x, y, width, height, border);
    }

    /** Rounded-looking rectangle built from three stacked fills; cheap and crisp at GUI scale. */
    public void rounded(int x, int y, int width, int height, int color) {
        if (width <= 2 || height <= 2) {
            fill(x, y, width, height, color);
            return;
        }
        fill(x + 1, y, width - 2, 1, color);
        fill(x, y + 1, width, height - 2, color);
        fill(x + 1, y + height - 1, width - 2, 1, color);
    }

    private void drawText(String value, int x, int y, int color, boolean shadow) {
        //? if >=26 {
        raw.text(font, value, x, y, color, shadow);
        //?} else {
        /*raw.drawString(font, value, x, y, color, shadow);
        *///?}
    }

    public void text(String value, int x, int y, int color) {
        drawText(value, x, y, color, false);
    }

    public void textShadow(String value, int x, int y, int color) {
        drawText(value, x, y, color, true);
    }

    public void textCentered(String value, int centerX, int y, int color) {
        drawText(value, centerX - font.width(value) / 2, y, color, false);
    }

    public void textRight(String value, int rightX, int y, int color) {
        drawText(value, rightX - font.width(value), y, color, false);
    }

    public int width(String value) {
        return font.width(value);
    }

    public String trim(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (font.width(sb.toString() + value.charAt(i) + "\u2026") > maxWidth) break;
            sb.append(value.charAt(i));
        }
        return sb + "\u2026";
    }

    public void pushScissor(int x, int y, int width, int height) {
        raw.enableScissor(x, y, x + width, y + height);
    }

    public void popScissor() {
        raw.disableScissor();
    }

    public void translate(float x, float y) {
        raw.pose().translate(x, y);
    }

    public void pushPose() {
        raw.pose().pushMatrix();
    }

    public void popPose() {
        raw.pose().popMatrix();
    }

    /** Wraps text to a width and returns the lines, for tooltips. */
    public java.util.List<String> wrap(String value, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(candidate) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    /** Draws an explanation near the cursor, nudged to stay on screen. */
    public void tooltip(String text, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (text == null || text.isEmpty()) return;
        java.util.List<String> lines = wrap(text, 190);
        int width = 0;
        for (String line : lines) width = Math.max(width, font.width(line));
        width += 12;
        int height = lines.size() * 11 + 9;
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + width > screenWidth - 4) x = Math.max(4, mouseX - width - 8);
        if (y + height > screenHeight - 4) y = Math.max(4, screenHeight - height - 4);
        fill(x - 1, y - 1, width + 2, height + 2, 0xF005070C);
        outline(x, y, width, height, Theme.ACCENT_DIM);
        int ty = y + 5;
        for (String line : lines) {
            text(line, x + 6, ty, Theme.TEXT);
            ty += 11;
        }
    }

    public void blit(Identifier texture, int x, int y, int width, int height, int texWidth, int texHeight) {
        raw.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, width, height,
                texWidth, texHeight, texWidth, texHeight);
    }
}
