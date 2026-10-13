package dev.skyjet.gui.widget;

import dev.skyjet.gui.Gfx;
import dev.skyjet.gui.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Clipped, scrollable container. Child widgets are laid out in panel space and translated on draw.
 */
public final class ScrollPanel {
    public int x;
    public int y;
    public int width;
    public int height;

    private final List<Widget> children = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private float scroll;
    private float targetScroll;
    private int contentHeight;
    private boolean draggingBar;

    public record Label(String text, int x, int y, int color, boolean header) {
    }

    public void clear() {
        children.clear();
        labels.clear();
        contentHeight = 0;
    }

    public void resetScroll() {
        scroll = 0.0f;
        targetScroll = 0.0f;
    }

    /**
     * Pins the scroll position back where it was after the panel is rebuilt, clamped to whatever
     * the new content allows. Editing anything rebuilds the panel, so without this every change
     * threw you back to the top of the tab.
     */
    public void keepScroll(float previous) {
        int max = maxScroll();
        targetScroll = Math.max(0.0f, Math.min(max, previous));
        scroll = targetScroll;
    }

    public float scrollTarget() {
        return targetScroll;
    }

    public <T extends Widget> T add(T widget) {
        children.add(widget);
        contentHeight = Math.max(contentHeight, widget.y + widget.height);
        return widget;
    }

    public void addLabel(String text, int lx, int ly, int color, boolean header) {
        labels.add(new Label(text, lx, ly, color, header));
        contentHeight = Math.max(contentHeight, ly + 12);
    }

    public void setContentHeight(int value) {
        contentHeight = Math.max(contentHeight, value);
    }

    public List<Widget> children() {
        return children;
    }

    public int scrollOffset() {
        return (int) scroll;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight + 12 - height);
    }

    public void tick() {
        for (Widget w : children) {
            w.tick();
        }
    }

    public void render(Gfx g, int mouseX, int mouseY, float partial) {
        scroll += (targetScroll - scroll) * Math.min(1.0f, 0.35f);
        if (Math.abs(targetScroll - scroll) < 0.4f) scroll = targetScroll;

        g.pushScissor(x, y, width, height);
        g.pushPose();
        g.translate(x, y - scroll);

        int localMouseX = mouseX - x;
        int localMouseY = (int) (mouseY - y + scroll);

        for (Label l : labels) {
            if (l.header()) {
                g.text(l.text().toUpperCase(java.util.Locale.ROOT), l.x(), l.y(), l.color());
                g.fill(l.x(), l.y() + 11, width - 24, 1, Theme.BORDER_SOFT);
            } else {
                g.text(l.text(), l.x(), l.y(), l.color());
            }
        }
        for (Widget w : children) {
            if (!w.visible) continue;
            if (w.y + w.height < scroll - 20 || w.y > scroll + height + 20) continue;
            w.render(g, localMouseX, localMouseY, partial);
        }

        g.popPose();
        g.popScissor();

        int max = maxScroll();
        if (max > 0) {
            int barH = Math.max(24, (int) (height * (height / (float) (contentHeight + 12))));
            int barY = y + (int) ((height - barH) * (scroll / (float) max));
            g.fill(x + width - 4, y, 2, height, Theme.BORDER_SOFT);
            g.rounded(x + width - 5, barY, 4, barH, Theme.withAlpha(Theme.ACCENT, 0.7f));
        }
    }

    public boolean isOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private double localX(double mouseX) {
        return mouseX - x;
    }

    private double localY(double mouseY) {
        return mouseY - y + scroll;
    }

    /** The widget currently drawing an overlay, if any. At most one is ever open. */
    public Widget overlayOwner() {
        for (Widget w : children) {
            if (w.hasOverlay()) return w;
        }
        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Widget overlay = overlayOwner();
        if (overlay != null) {
            // Overlays are clamped to the screen rather than to this panel and are drawn on top
            // of every sibling, so the owner gets the click first and unconditionally. Walking the
            // children in order instead let whatever happened to be underneath the picker consume
            // the click, which read as the picker closing the moment you touched it.
            overlay.mouseClicked(localX(mouseX), localY(mouseY), button);
            return true;
        }
        if (!isOver(mouseX, mouseY)) {
            for (Widget w : children) {
                w.setFocused(false);
            }
            return false;
        }
        int max = maxScroll();
        if (max > 0 && mouseX >= x + width - 6) {
            draggingBar = true;
            applyBarDrag(mouseY);
            return true;
        }
        double lx = localX(mouseX);
        double ly = localY(mouseY);
        boolean handled = false;
        for (Widget w : children) {
            if (w.mouseClicked(lx, ly, button)) {
                handled = true;
                break;
            }
        }
        if (handled) {
            for (Widget w : children) {
                if (!w.isOver(lx, ly)) {
                    w.setFocused(false);
                }
            }
        }
        return handled;
    }

    private void applyBarDrag(double mouseY) {
        int max = maxScroll();
        if (max <= 0) return;
        float t = (float) ((mouseY - y) / (double) height);
        targetScroll = Math.max(0, Math.min(max, t * max));
        scroll = targetScroll;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (draggingBar) {
            applyBarDrag(mouseY);
            return true;
        }
        Widget overlay = overlayOwner();
        if (overlay != null) {
            overlay.mouseDragged(localX(mouseX), localY(mouseY), button);
            return true;
        }
        double lx = localX(mouseX);
        double ly = localY(mouseY);
        for (Widget w : children) {
            if (w.mouseDragged(lx, ly, button)) return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        double lx = localX(mouseX);
        double ly = localY(mouseY);
        boolean handled = false;
        for (Widget w : children) {
            if (w.mouseReleased(lx, ly, button)) handled = true;
        }
        return handled;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isOver(mouseX, mouseY) && overlayOwner() == null) return false;
        double lx = localX(mouseX);
        double ly = localY(mouseY);
        Widget overlay = overlayOwner();
        if (overlay != null) {
            // Scrolling the panel under an open picker would slide it out from under the cursor.
            overlay.mouseScrolled(lx, ly, amount);
            return true;
        }
        int max = maxScroll();
        if (max <= 0) return true;
        targetScroll = (float) Math.max(0, Math.min(max, targetScroll - amount * 24.0));
        return true;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        for (Widget w : children) {
            if (w.keyPressed(keyCode, modifiers)) return true;
        }
        return false;
    }

    public boolean charTyped(int codepoint) {
        for (Widget w : children) {
            if (w.charTyped(codepoint)) return true;
        }
        return false;
    }

    public void renderOverlays(Gfx g, int mouseX, int mouseY, float partial, int screenWidth, int screenHeight) {
        // Overlays are positioned in panel space, so the clamp bounds are converted to match.
        int localRight = screenWidth - x;
        int localBottom = (int) (screenHeight - y + scroll);
        for (Widget w : children) {
            if (!w.hasOverlay()) continue;
            g.pushPose();
            g.translate(x, y - scroll);
            if (w instanceof Dropdown d) {
                d.clampList(localRight, localBottom);
            } else if (w instanceof ColorField c) {
                c.clampPicker(localRight, localBottom);
            }
            w.renderOverlay(g, mouseX - x, (int) (mouseY - y + scroll), partial);
            g.popPose();
        }
    }

    /** Tooltip of whatever the cursor is over, or null. Overlays take priority. */
    public String tooltipAt(double mouseX, double mouseY) {
        if (!isOver(mouseX, mouseY)) return null;
        double lx = localX(mouseX);
        double ly = localY(mouseY);
        for (Widget w : children) {
            if (w.visible && w.tooltip != null && w.isOver(lx, ly)) return w.tooltip;
        }
        return null;
    }

    public boolean hasOpenOverlay() {
        for (Widget w : children) {
            if (w.hasOverlay()) return true;
        }
        return false;
    }
}
