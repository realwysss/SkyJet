package dev.skyjet.gui.widget;

import dev.skyjet.gui.Gfx;

public abstract class Widget {
    public int x;
    public int y;
    public int width;
    public int height;
    public boolean visible = true;
    public boolean enabled = true;
    public String tooltip;

    protected Widget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(Gfx g, int mouseX, int mouseY, float partial);

    public boolean isOver(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        return false;
    }

    public boolean charTyped(int codepoint) {
        return false;
    }

    public void tick() {
    }

    /** Overlay widgets (dropdown lists, colour pickers) draw after everything else. */
    public boolean hasOverlay() {
        return false;
    }

    public void renderOverlay(Gfx g, int mouseX, int mouseY, float partial) {
    }

    public void setFocused(boolean focused) {
    }
}
