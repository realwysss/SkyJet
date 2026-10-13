package dev.skyjet.gui.widget;

import dev.skyjet.gui.Gfx;
import dev.skyjet.gui.Theme;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class TextField extends Widget {
    private String value = "";
    private int cursor;
    private int scrollOffset;
    private boolean focused;
    private int blink;
    private String placeholder = "";
    private int maxLength = 64;
    private Predicate<String> filter = s -> true;
    private Consumer<String> onChange = s -> {
    };
    private Consumer<String> onCommit = s -> {
    };

    public TextField(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public TextField placeholder(String value) {
        this.placeholder = value;
        return this;
    }

    public TextField maxLength(int value) {
        this.maxLength = value;
        return this;
    }

    public TextField filter(Predicate<String> value) {
        this.filter = value;
        return this;
    }

    public TextField onChange(Consumer<String> value) {
        this.onChange = value;
        return this;
    }

    public TextField onCommit(Consumer<String> value) {
        this.onCommit = value;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String newValue) {
        this.value = newValue == null ? "" : newValue;
        this.cursor = Math.min(cursor, this.value.length());
    }

    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean value) {
        if (focused && !value) {
            onCommit.accept(this.value);
        }
        this.focused = value;
        this.blink = 0;
    }

    @Override
    public void tick() {
        blink++;
    }

    @Override
    public void render(Gfx g, int mouseX, int mouseY, float partial) {
        if (!visible) return;
        boolean hover = isOver(mouseX, mouseY);
        g.rounded(x, y, width, height, Theme.WINDOW_ALT);
        g.outline(x, y, width, height, focused ? Theme.ACCENT : (hover ? Theme.BORDER : Theme.BORDER_SOFT));

        int textY = y + (height - 8) / 2;
        int innerX = x + 5;
        int innerW = width - 10;

        String shown = value;
        if (g.width(shown) > innerW) {
            scrollOffset = Math.min(scrollOffset, Math.max(0, shown.length() - 1));
            shown = shown.substring(scrollOffset);
            while (g.width(shown) > innerW && shown.length() > 1) {
                shown = shown.substring(0, shown.length() - 1);
            }
        } else {
            scrollOffset = 0;
        }

        if (value.isEmpty() && !focused) {
            g.text(placeholder, innerX, textY, Theme.TEXT_FAINT);
        } else {
            g.text(shown, innerX, textY, Theme.TEXT);
        }

        if (focused && (blink / 12) % 2 == 0) {
            int caret = Math.max(0, Math.min(cursor - scrollOffset, shown.length()));
            int caretX = innerX + g.width(shown.substring(0, caret));
            g.fill(caretX, textY - 2, 1, 11, Theme.ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled) return false;
        boolean over = isOver(mouseX, mouseY);
        if (over && button == 0) {
            setFocused(true);
            cursor = value.length();
            return true;
        }
        if (!over && focused) {
            setFocused(false);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int modifiers) {
        if (!focused) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                for (int i = 0; i < clip.length(); i++) {
                    insert(clip.charAt(i));
                }
            }
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            Minecraft.getInstance().keyboardHandler.setClipboard(value);
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            cursor = value.length();
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursor > 0) {
                    value = value.substring(0, cursor - 1) + value.substring(cursor);
                    cursor--;
                    onChange.accept(value);
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursor < value.length()) {
                    value = value.substring(0, cursor) + value.substring(cursor + 1);
                    onChange.accept(value);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> cursor = Math.max(0, cursor - 1);
            case GLFW.GLFW_KEY_RIGHT -> cursor = Math.min(value.length(), cursor + 1);
            case GLFW.GLFW_KEY_HOME -> cursor = 0;
            case GLFW.GLFW_KEY_END -> cursor = value.length();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                onCommit.accept(value);
                setFocused(false);
            }
            default -> {
            }
        }
        return true;
    }

    private void insert(char c) {
        if (c < 32 || value.length() >= maxLength) return;
        String candidate = value.substring(0, cursor) + c + value.substring(cursor);
        if (!filter.test(candidate)) return;
        value = candidate;
        cursor++;
        onChange.accept(value);
    }

    @Override
    public boolean charTyped(int codepoint) {
        if (!focused) return false;
        insert((char) codepoint);
        return true;
    }
}
