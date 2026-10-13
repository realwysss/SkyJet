package dev.skyjet.gui.widget;

import dev.skyjet.gui.Gfx;
import dev.skyjet.gui.Theme;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class Slider extends Widget {
    private final String label;
    private final double min;
    private final double max;
    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private boolean dragging;
    private int decimals = 2;
    private String suffix = "";
    private double step;

    public Slider(int x, int y, int width, String label, double min, double max,
                  DoubleSupplier getter, DoubleConsumer setter) {
        super(x, y, width, 28);
        this.label = label;
        this.min = min;
        this.max = max;
        this.getter = getter;
        this.setter = setter;
    }

    public Slider decimals(int value) {
        this.decimals = value;
        return this;
    }

    public Slider step(double value) {
        this.step = value;
        return this;
    }

    public Slider suffix(String value) {
        this.suffix = value;
        return this;
    }

    public Slider integer() {
        this.decimals = 0;
        this.step = 1.0;
        return this;
    }

    private double normalized() {
        double v = getter.getAsDouble();
        return Math.max(0.0, Math.min(1.0, (v - min) / (max - min)));
    }

    private String formatted() {
        double v = getter.getAsDouble();
        if (decimals <= 0) {
            return Math.round(v) + suffix;
        }
        return String.format("%." + decimals + "f", v) + suffix;
    }

    @Override
    public void render(Gfx g, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        boolean hover = isOver(mouseX, mouseY) || dragging;
        int trackY = y + 18;
        int trackH = 4;

        g.text(label, x, y + 2, enabled ? Theme.TEXT : Theme.TEXT_FAINT);
        g.textRight(formatted(), x + width, y + 2, hover ? Theme.ACCENT : Theme.TEXT_DIM);

        g.rounded(x, trackY, width, trackH, Theme.TRACK);
        int fill = (int) (width * normalized());
        if (fill > 0) {
            g.rounded(x, trackY, fill, trackH, Theme.TRACK_FILL);
        }
        int knobX = x + Math.max(0, Math.min(width - 8, fill - 4));
        g.rounded(knobX, trackY - 3, 8, trackH + 6, hover ? 0xFFFFFFFF : Theme.TEXT);
    }

    private void applyFromMouse(double mouseX) {
        double t = Math.max(0.0, Math.min(1.0, (mouseX - x) / (double) width));
        double value = min + t * (max - min);
        if (step > 0.0) {
            value = Math.round(value / step) * step;
        }
        setter.accept(value);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || button != 0 || !isOver(mouseX, mouseY)) return false;
        dragging = true;
        applyFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging) return false;
        applyFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = dragging;
        dragging = false;
        return was;
    }

}
