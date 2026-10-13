package dev.skyjet.gui.widget;

import dev.skyjet.gui.Gfx;
import dev.skyjet.gui.Theme;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class LayerRow extends Widget {
    private final String name;
    private final int index;
    private final boolean selected;
    private final BooleanSupplier enabledGetter;
    private final Consumer<Boolean> enabledSetter;
    private final IntConsumer onSelect;
    private final IntConsumer onMoveUp;
    private final IntConsumer onMoveDown;
    private final IntConsumer onDelete;
    private final int swatch;

    public LayerRow(int x, int y, int width, String name, int index, boolean selected, int swatch,
                    BooleanSupplier enabledGetter, Consumer<Boolean> enabledSetter,
                    IntConsumer onSelect, IntConsumer onMoveUp, IntConsumer onMoveDown, IntConsumer onDelete) {
        super(x, y, width, 22);
        this.name = name;
        this.index = index;
        this.selected = selected;
        this.swatch = swatch;
        this.enabledGetter = enabledGetter;
        this.enabledSetter = enabledSetter;
        this.onSelect = onSelect;
        this.onMoveUp = onMoveUp;
        this.onMoveDown = onMoveDown;
        this.onDelete = onDelete;
    }

    private int dotX() {
        return x + 6;
    }

    private int upX() {
        return x + width - 52;
    }

    private int downX() {
        return x + width - 36;
    }

    private int delX() {
        return x + width - 18;
    }

    @Override
    public void render(Gfx g, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        boolean hover = isOver(mouseX, mouseY);
        boolean on = enabledGetter.getAsBoolean();
        g.rounded(x, y, width, height, selected ? Theme.PANEL_ACTIVE : (hover ? Theme.PANEL_HOVER : Theme.PANEL));
        if (selected) {
            g.fill(x, y + 2, 2, height - 4, Theme.ACCENT);
        }
        g.rounded(dotX(), y + 7, 8, 8, on ? Theme.SUCCESS : Theme.TEXT_FAINT);
        if (swatch != 0) {
            g.rounded(dotX() + 12, y + 6, 10, 10, swatch);
            g.outline(dotX() + 12, y + 6, 10, 10, Theme.BORDER_SOFT);
            g.text(g.trim(name, width - 90), dotX() + 27, y + 7, on ? Theme.TEXT : Theme.TEXT_FAINT);
        } else {
            g.text(g.trim(name, width - 78), dotX() + 14, y + 7, on ? Theme.TEXT : Theme.TEXT_FAINT);
        }

        g.text("▲", upX(), y + 7, iconColor(mouseX, mouseY, upX()));
        g.text("▼", downX(), y + 7, iconColor(mouseX, mouseY, downX()));
        g.text("✕", delX(), y + 7, hitIcon(mouseX, mouseY, delX()) ? Theme.DANGER : Theme.TEXT_FAINT);
    }

    private int iconColor(int mouseX, int mouseY, int iconX) {
        return hitIcon(mouseX, mouseY, iconX) ? Theme.ACCENT : Theme.TEXT_FAINT;
    }

    private boolean hitIcon(double mouseX, double mouseY, int iconX) {
        return mouseX >= iconX - 3 && mouseX < iconX + 13 && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isOver(mouseX, mouseY)) return false;
        if (mouseX >= dotX() - 3 && mouseX < dotX() + 11) {
            enabledSetter.accept(!enabledGetter.getAsBoolean());
            return true;
        }
        if (hitIcon(mouseX, mouseY, delX())) {
            onDelete.accept(index);
            return true;
        }
        if (hitIcon(mouseX, mouseY, downX())) {
            onMoveDown.accept(index);
            return true;
        }
        if (hitIcon(mouseX, mouseY, upX())) {
            onMoveUp.accept(index);
            return true;
        }
        onSelect.accept(index);
        return true;
    }
}
