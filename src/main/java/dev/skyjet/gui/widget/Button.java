package dev.skyjet.gui.widget;

import dev.skyjet.gui.Gfx;
import dev.skyjet.gui.Theme;

public final class Button extends Widget {
    public String label;
    private final Runnable action;
    private int accent = Theme.ACCENT;
    private boolean primary;
    private boolean danger;

    public Button(int x, int y, int width, int height, String label, Runnable action) {
        super(x, y, width, height);
        this.label = label;
        this.action = action;
    }

    public Button primary() {
        this.primary = true;
        return this;
    }

    public Button danger() {
        this.danger = true;
        this.accent = Theme.DANGER;
        return this;
    }

    @Override
    public void render(Gfx g, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        boolean hover = enabled && isOver(mouseX, mouseY);
        int base;
        int textColor;
        if (primary) {
            base = hover ? Theme.lerp(accent, 0xFFFFFFFF, 0.15f) : accent;
            textColor = 0xFF05070C;
        } else if (danger) {
            base = hover ? Theme.withAlpha(Theme.DANGER, 0.30f) : Theme.withAlpha(Theme.DANGER, 0.16f);
            textColor = Theme.DANGER;
        } else {
            base = hover ? Theme.PANEL_HOVER : Theme.PANEL;
            textColor = enabled ? Theme.TEXT : Theme.TEXT_FAINT;
        }
        g.rounded(x, y, width, height, base);
        if (!primary) {
            g.outline(x, y, width, height, hover ? Theme.BORDER : Theme.BORDER_SOFT);
        }
        g.textCentered(g.trim(label, width - 8), x + width / 2, y + (height - 8) / 2, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || button != 0 || !isOver(mouseX, mouseY)) return false;
        action.run();
        return true;
    }
}
