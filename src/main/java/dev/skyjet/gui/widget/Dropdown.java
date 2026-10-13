package dev.skyjet.gui.widget;

import dev.skyjet.gui.Gfx;
import dev.skyjet.gui.Theme;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class Dropdown extends Widget {
    private final String label;
    private final String[] options;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private boolean open;
    private int listX;
    private int listY;
    private int listHeight;

    public Dropdown(int x, int y, int width, String label, String[] options, IntSupplier getter, IntConsumer setter) {
        super(x, y, width, 34);
        this.label = label;
        this.options = options;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void render(Gfx g, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        g.text(label, x, y, Theme.TEXT_DIM);
        int boxY = y + 12;
        boolean hover = mouseX >= x && mouseX < x + width && mouseY >= boxY && mouseY < boxY + 20;
        g.rounded(x, boxY, width, 20, hover || open ? Theme.PANEL_HOVER : Theme.PANEL);
        g.outline(x, boxY, width, 20, open ? Theme.ACCENT : (hover ? Theme.BORDER : Theme.BORDER_SOFT));
        int index = Math.max(0, Math.min(options.length - 1, getter.getAsInt()));
        g.text(g.trim(options[index], width - 22), x + 6, boxY + 6, Theme.TEXT);
        g.text(open ? "▲" : "▼", x + width - 14, boxY + 6, Theme.TEXT_DIM);
    }

    @Override
    public boolean hasOverlay() {
        return open;
    }

    @Override
    public void renderOverlay(Gfx g, int mouseX, int mouseY, float partial) {
        if (!open) return;
        g.fill(listX + 2, listY + 2, width, listHeight, 0x66000000);
        g.rounded(listX, listY, width, listHeight, Theme.WINDOW);
        g.outline(listX, listY, width, listHeight, Theme.ACCENT);
        int selected = getter.getAsInt();
        for (int i = 0; i < options.length; i++) {
            int rowY = listY + 2 + i * 18;
            boolean hover = mouseX >= listX && mouseX < listX + width && mouseY >= rowY && mouseY < rowY + 18;
            if (hover) {
                g.fill(listX + 2, rowY, width - 4, 18, Theme.PANEL_HOVER);
            }
            if (i == selected) {
                g.fill(listX + 2, rowY, 2, 18, Theme.ACCENT);
            }
            g.text(g.trim(options[i], width - 16), listX + 8, rowY + 5, i == selected ? Theme.ACCENT : Theme.TEXT);
        }
    }

    public void close() {
        open = false;
    }

    public void clampList(int rightBound, int bottomBound) {
        if (!open) return;
        listHeight = options.length * 18 + 4;
        listX = Math.max(0, Math.min(x, rightBound - width));
        listY = y + 33;
        if (listY + listHeight > bottomBound - 4) {
            listY = y + 12 - listHeight;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || button != 0) return false;
        if (open) {
            if (mouseX >= listX && mouseX < listX + width && mouseY >= listY && mouseY < listY + listHeight) {
                int index = (int) ((mouseY - listY - 2) / 18);
                if (index >= 0 && index < options.length) {
                    setter.accept(index);
                }
                open = false;
                return true;
            }
            open = false;
            return true;
        }
        int boxY = y + 12;
        if (mouseX >= x && mouseX < x + width && mouseY >= boxY && mouseY < boxY + 20) {
            open = true;
            return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) open = false;
    }
}
