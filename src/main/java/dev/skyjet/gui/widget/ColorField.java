package dev.skyjet.gui.widget;

import dev.skyjet.gui.ColorUtil;
import dev.skyjet.gui.Gfx;
import dev.skyjet.gui.Theme;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Colour swatch plus hex entry. Clicking the swatch opens the full picker as an overlay.
 */
public final class ColorField extends Widget {
    private static final int[] QUICK = {
            0xFFFFFFFF, 0xFF000000, 0xFF4EA1FF, 0xFF8B6BFF, 0xFF2BFF9B, 0xFFFFC46B,
            0xFFE8688A, 0xFFFF4FA8, 0xFF17C3D6, 0xFF7A1FD4, 0xFFD4623F, 0xFF0A0D12
    };

    private final String label;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final boolean useAlpha;
    private final TextField hex;
    private boolean pickerOpen;

    private float hue;
    private float sat;
    private float val;
    private int alpha = 255;

    private int pickerX;
    private int pickerY;
    private static final int PICKER_W = 210;
    // Tall enough for the square, the alpha bar, two rows of swatches and the preview strip.
    // The old height cut the bottom row off, which is what left swatches sitting outside the
    // panel with nothing drawn behind them.
    private static final int PICKER_H = 208;
    private static final int SQUARE = 120;
    private static final int SHADOW_PAD = 6;

    private int dragTarget = -1;

    public ColorField(int x, int y, int width, String label, IntSupplier getter, IntConsumer setter, boolean useAlpha) {
        super(x, y, width, 34);
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.useAlpha = useAlpha;
        this.hex = new TextField(x, y + 12, 78, 20)
                .maxLength(useAlpha ? 9 : 7)
                .filter(s -> ColorUtil.isHexCandidate(s))
                .onChange(this::applyHex);
        syncFromValue();
    }

    private void applyHex(String text) {
        Integer parsed = ColorUtil.parseHex(text, useAlpha ? alpha : 255);
        if (parsed != null) {
            setter.accept(parsed);
            syncFromValue();
        }
    }

    private void syncFromValue() {
        int color = getter.getAsInt();
        float[] hsv = ColorUtil.toHsv(color);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        alpha = useAlpha ? ColorUtil.alpha(color) : 255;
        if (!hex.isFocused()) {
            hex.setValue(ColorUtil.toHex(color, useAlpha));
        }
    }

    private void commit() {
        setter.accept(ColorUtil.fromHsv(hue, sat, val, useAlpha ? alpha : 255));
        if (!hex.isFocused()) {
            hex.setValue(ColorUtil.toHex(getter.getAsInt(), useAlpha));
        }
    }

    public void layout(int newX, int newY, int newWidth) {
        this.x = newX;
        this.y = newY;
        this.width = newWidth;
        hex.x = newX;
        hex.y = newY + 12;
    }

    @Override
    public void tick() {
        hex.tick();
        if (!hex.isFocused()) {
            syncFromValue();
        }
    }

    @Override
    public void render(Gfx g, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        int color = getter.getAsInt();
        g.text(label, x, y, Theme.TEXT_DIM);

        hex.render(g, mouseX, mouseY, partial);

        int swatchX = x + 84;
        int swatchW = Math.max(24, width - 84);
        drawChecker(g, swatchX, y + 12, swatchW, 20);
        g.rounded(swatchX, y + 12, swatchW, 20, color);
        boolean hover = mouseX >= swatchX && mouseX < swatchX + swatchW && mouseY >= y + 12 && mouseY < y + 32;
        g.outline(swatchX, y + 12, swatchW, 20, hover || pickerOpen ? Theme.ACCENT : Theme.BORDER_SOFT);
    }

    private static void drawChecker(Gfx g, int x, int y, int w, int h) {
        int cell = 5;
        for (int cy = 0; cy < h; cy += cell) {
            for (int cx = 0; cx < w; cx += cell) {
                boolean dark = ((cx / cell) + (cy / cell)) % 2 == 0;
                g.fill(x + cx, y + cy, Math.min(cell, w - cx), Math.min(cell, h - cy),
                        dark ? 0xFF2A2F3A : 0xFF3A404D);
            }
        }
    }

    @Override
    public boolean hasOverlay() {
        return pickerOpen;
    }

    @Override
    public void renderOverlay(Gfx g, int mouseX, int mouseY, float partial) {
        if (!pickerOpen) return;
        int px = pickerX;
        int py = pickerY;
        // A backdrop that is larger than the panel on every side, so nothing behind the picker
        // shows through around its edge.
        g.fill(px - SHADOW_PAD, py - SHADOW_PAD, PICKER_W + SHADOW_PAD * 2, PICKER_H + SHADOW_PAD * 2,
                0xCC05070C);
        g.rounded(px, py, PICKER_W, PICKER_H, Theme.WINDOW);
        g.outline(px, py, PICKER_W, PICKER_H, Theme.ACCENT);

        int sx = px + 10;
        int sy = py + 10;
        int pureHue = ColorUtil.fromHsv(hue, 1.0f, 1.0f, 255);
        for (int i = 0; i < SQUARE; i++) {
            float s = i / (float) (SQUARE - 1);
            int top = Theme.lerp(0xFFFFFFFF, pureHue, s);
            g.gradient(sx + i, sy, 1, SQUARE, top, 0xFF000000);
        }
        g.outline(sx - 1, sy - 1, SQUARE + 2, SQUARE + 2, Theme.BORDER);
        int cursorX = sx + (int) (sat * (SQUARE - 1));
        int cursorY = sy + (int) ((1.0f - val) * (SQUARE - 1));
        g.fill(cursorX - 3, cursorY, 7, 1, 0xFFFFFFFF);
        g.fill(cursorX, cursorY - 3, 1, 7, 0xFFFFFFFF);

        int hueX = sx + SQUARE + 10;
        int hueW = 14;
        for (int i = 0; i < SQUARE; i++) {
            float h = i / (float) (SQUARE - 1);
            g.fill(hueX, sy + i, hueW, 1, ColorUtil.fromHsv(h, 1.0f, 1.0f, 255));
        }
        g.outline(hueX - 1, sy - 1, hueW + 2, SQUARE + 2, Theme.BORDER);
        int hueY = sy + (int) (hue * (SQUARE - 1));
        g.fill(hueX - 2, hueY - 1, hueW + 4, 2, 0xFFFFFFFF);

        int barY = sy + SQUARE + 8;
        if (useAlpha) {
            drawChecker(g, sx, barY, SQUARE, 12);
            int opaque = ColorUtil.fromHsv(hue, sat, val, 255);
            for (int i = 0; i < SQUARE; i++) {
                float a = i / (float) (SQUARE - 1);
                g.fill(sx + i, barY, 1, 12, Theme.withAlpha(opaque, a));
            }
            g.outline(sx - 1, barY - 1, SQUARE + 2, 14, Theme.BORDER);
            int ax = sx + (int) (alpha / 255.0f * (SQUARE - 1));
            g.fill(ax - 1, barY - 2, 2, 16, 0xFFFFFFFF);
        }

        int quickY = barY + (useAlpha ? 18 : 4);
        for (int i = 0; i < QUICK.length; i++) {
            int qx = sx + (i % 6) * 15;
            int qy = quickY + (i / 6) * 15;
            g.rounded(qx, qy, 13, 13, QUICK[i]);
            g.outline(qx, qy, 13, 13, Theme.BORDER_SOFT);
        }

        int previewX = hueX;
        drawChecker(g, previewX, barY, 14, 44);
        g.rounded(previewX, barY, 14, 44, ColorUtil.fromHsv(hue, sat, val, useAlpha ? alpha : 255));
        g.outline(previewX, barY, 14, 44, Theme.BORDER);
    }

    private boolean handlePicker(double mouseX, double mouseY, boolean click) {
        int sx = pickerX + 10;
        int sy = pickerY + 10;
        int hueX = sx + SQUARE + 10;
        int barY = sy + SQUARE + 8;

        if (click) {
            if (mouseX >= sx && mouseX < sx + SQUARE && mouseY >= sy && mouseY < sy + SQUARE) {
                dragTarget = 0;
            } else if (mouseX >= hueX && mouseX < hueX + 14 && mouseY >= sy && mouseY < sy + SQUARE) {
                dragTarget = 1;
            } else if (useAlpha && mouseX >= sx && mouseX < sx + SQUARE && mouseY >= barY && mouseY < barY + 12) {
                dragTarget = 2;
            } else {
                int quickY = barY + (useAlpha ? 18 : 4);
                for (int i = 0; i < QUICK.length; i++) {
                    int qx = sx + (i % 6) * 15;
                    int qy = quickY + (i / 6) * 15;
                    if (mouseX >= qx && mouseX < qx + 13 && mouseY >= qy && mouseY < qy + 13) {
                        float[] hsv = ColorUtil.toHsv(QUICK[i]);
                        hue = hsv[0];
                        sat = hsv[1];
                        val = hsv[2];
                        commit();
                        return true;
                    }
                }
                return false;
            }
        }

        if (dragTarget == 0) {
            sat = clamp01((float) (mouseX - sx) / (SQUARE - 1));
            val = 1.0f - clamp01((float) (mouseY - sy) / (SQUARE - 1));
        } else if (dragTarget == 1) {
            hue = clamp01((float) (mouseY - sy) / (SQUARE - 1));
        } else if (dragTarget == 2) {
            alpha = Math.round(clamp01((float) (mouseX - sx) / (SQUARE - 1)) * 255.0f);
        } else {
            return false;
        }
        commit();
        return true;
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    public void closePicker() {
        pickerOpen = false;
        dragTarget = -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled) return false;
        if (pickerOpen) {
            if (mouseX >= pickerX && mouseX < pickerX + PICKER_W && mouseY >= pickerY && mouseY < pickerY + PICKER_H) {
                handlePicker(mouseX, mouseY, true);
                return true;
            }
            closePicker();
            // Clicking the swatch again closes the picker rather than closing and reopening it
            // in the same frame, which looked like the picker refusing to shut.
            return true;
        }
        if (hex.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int swatchX = x + 84;
        int swatchW = Math.max(24, width - 84);
        if (button == 0 && mouseX >= swatchX && mouseX < swatchX + swatchW && mouseY >= y + 12 && mouseY < y + 32) {
            pickerOpen = true;
            syncFromValue();
            pickerX = Math.max(0, (int) mouseX - PICKER_W / 2);
            pickerY = y + 34;
            return true;
        }
        return false;
    }

    public void clampPicker(int rightBound, int bottomBound) {
        if (!pickerOpen) return;
        pickerX = Math.max(SHADOW_PAD, Math.min(pickerX, rightBound - PICKER_W - SHADOW_PAD));
        if (pickerY + PICKER_H > bottomBound) {
            pickerY = Math.max(SHADOW_PAD, y + 12 - PICKER_H - 4);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (pickerOpen && dragTarget >= 0) {
            handlePicker(mouseX, mouseY, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = dragTarget >= 0;
        dragTarget = -1;
        return was;
    }

    @Override
    public boolean keyPressed(int keyCode, int modifiers) {
        return hex.keyPressed(keyCode, modifiers);
    }

    @Override
    public boolean charTyped(int codepoint) {
        return hex.charTyped(codepoint);
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) {
            hex.setFocused(false);
            closePicker();
        }
    }

    public boolean isEditing() {
        return hex.isFocused() || pickerOpen;
    }
}
