package dev.skyjet.gui.widget;

import dev.skyjet.gui.Gfx;
import dev.skyjet.gui.Theme;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class Toggle extends Widget {
    private final String label;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private float anim;

    public Toggle(int x, int y, int width, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, width, 20);
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.anim = getter.getAsBoolean() ? 1.0f : 0.0f;
    }

    @Override
    public void render(Gfx g, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        boolean on = getter.getAsBoolean();
        float target = on ? 1.0f : 0.0f;
        anim += (target - anim) * Math.min(1.0f, partial * 0.5f + 0.25f);
        boolean hover = isOver(mouseX, mouseY);

        int knobW = 26;
        int knobH = 14;
        int trackX = x + width - knobW;
        int trackY = y + (height - knobH) / 2;

        g.text(g.trim(label, width - knobW - 10), x, y + (height - 8) / 2, enabled ? Theme.TEXT : Theme.TEXT_FAINT);
        g.rounded(trackX, trackY, knobW, knobH, Theme.lerp(Theme.TRACK, Theme.ACCENT, anim));
        if (hover) {
            g.outline(trackX, trackY, knobW, knobH, Theme.withAlpha(Theme.ACCENT, 0.5f));
        }
        int dotX = trackX + 2 + (int) ((knobW - knobH + 2 - 2) * anim);
        g.rounded(dotX, trackY + 2, knobH - 4, knobH - 4, on ? 0xFF06090F : Theme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || button != 0 || !isOver(mouseX, mouseY)) return false;
        setter.accept(!getter.getAsBoolean());
        return true;
    }
}
