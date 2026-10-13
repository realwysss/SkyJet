package dev.skyjet.gui;

import dev.skyjet.config.AnimationSettings;
import dev.skyjet.config.AuroraLayer;
import dev.skyjet.config.BodyLayer;
import dev.skyjet.config.CloudLayer;
import dev.skyjet.config.ConfigManager;
import dev.skyjet.config.GradientLayer;
import dev.skyjet.config.LayerStack;
import dev.skyjet.config.NebulaLayer;
import dev.skyjet.config.Presets;
import dev.skyjet.config.SkyConfig;
import dev.skyjet.config.SkyboxLayer;
import dev.skyjet.config.StarLayer;
import dev.skyjet.config.WorldSettings;
import dev.skyjet.gui.widget.Button;
import dev.skyjet.gui.widget.ColorField;
import dev.skyjet.gui.widget.Dropdown;
import dev.skyjet.gui.widget.LayerRow;
import dev.skyjet.gui.widget.ScrollPanel;
import dev.skyjet.gui.widget.Slider;
import dev.skyjet.gui.widget.TextField;
import dev.skyjet.gui.widget.Toggle;
import dev.skyjet.gui.widget.Widget;
import dev.skyjet.render.SkyPackScanner;
import dev.skyjet.render.SkyState;
import dev.skyjet.render.SkyTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

public final class SkyJetScreen extends Screen {
    /**
     * {@code simple} tabs are the ones that survive into the beginner layout. The group is the
     * sidebar heading a tab sits under: fourteen flat entries were a wall, and the same fourteen
     * under five headings read as a short list you can scan.
     */
    private enum Tab {
        PRESETS("Presets", true, "Start"),
        GUIDE("Guide", true, "Start"),
        SKYBOX("Skybox", true, "The sky"),
        GRADIENTS("Gradients", false, "The sky"),
        ATMOSPHERE("Atmosphere", false, "The sky"),
        CLOUDS("Clouds", false, "The sky"),
        STARS("Stars", false, "Night"),
        COSMOS("Cosmos", false, "Night"),
        AURORA("Aurora", false, "Night"),
        BODIES("Bodies", false, "Night"),
        LAYERS("Layers", true, "Setup"),
        IMAGE("Image", true, "Setup"),
        ANIMATION("Animation", true, "Setup"),
        WORLD("World", false, "Setup"),
        PROFILES("Profiles", true, "Setup"),
        INTERFACE("Interface", true, "Setup");

        final String title;
        final boolean simple;
        final String group;

        Tab(String title, boolean simple, String group) {
            this.title = title;
            this.simple = simple;
            this.group = group;
        }
    }

    private static final int SIDEBAR_W = 116;
    private static final int GROUP_H = 13;
    private static final int HEADER_H = 36;
    private static final int PAD = 12;

    private static Tab activeTab = Tab.PRESETS;
    private static int skyboxIndex;
    private static int gradientIndex;
    private static int starIndex;
    private static int nebulaIndex;
    private static int auroraIndex;
    private static int bodyIndex;
    private static int cloudIndex;

    private final SkyPreview preview = new SkyPreview();
    private final ScrollPanel content = new ScrollPanel();

    private int winX;
    private int winY;
    private int winW;
    private int winH;
    private int previewX;
    private int previewY;
    private int previewW;
    private int previewH;
    private int tabSpacing = 24;

    private float previewYaw = 30.0f;
    private float previewPitch = -12.0f;
    private float previewFov = 100.0f;
    private boolean draggingPreview;
    private double lastMouseX;
    private double lastMouseY;
    private float previewTime;
    private float previewDay = 0.52f;
    private boolean previewFollowConfig = true;
    private long lastFrameNanos = System.nanoTime();
    private long lastPreviewNanos;

    /** Bodies tab: while armed, clicking the preview drops the selected body where you clicked. */
    private boolean placingBody;
    /** Shown once, until a layout has been chosen. */
    private boolean choosingMode;

    private TextField profileNameField;
    private String status = "";
    private long statusUntil;
    private long resetArmedUntil;
    private long scratchArmedUntil;

    public SkyJetScreen() {
        super(Component.literal("SkyJet"));
        this.choosingMode = !ConfigManager.modeChosen();
    }

    private SkyConfig cfg() {
        return ConfigManager.active();
    }

    private static boolean simple() {
        return ConfigManager.simpleMode();
    }

    private static List<Tab> visibleTabs() {
        List<Tab> tabs = new java.util.ArrayList<>();
        for (Tab t : Tab.values()) {
            if (!simple() || t.simple) tabs.add(t);
        }
        return tabs;
    }

    /** Switches tabs by display name; used by the automated client test. */
    public void selectTabForTest(String title) {
        for (Tab tab : Tab.values()) {
            if (tab.title.equalsIgnoreCase(title)) {
                activeTab = tab;
                choosingMode = false;
                ensureNightPreview(tab);
                rebuildKeepsScroll = false;
                rebuild();
                return;
            }
        }
    }

    @Override
    protected void init() {
        winX = PAD;
        winY = PAD;
        winW = Math.max(360, this.width - PAD * 2);
        winH = Math.max(240, this.height - PAD * 2);

        int contentX = winX + SIDEBAR_W;
        int contentW = winW - SIDEBAR_W;
        previewX = contentX + PAD;
        previewY = winY + HEADER_H + PAD;
        previewW = contentW - PAD * 2;
        previewH = Math.max(90, Math.min(260, (int) ((winH - HEADER_H) * 0.40f)));

        content.x = contentX + PAD;
        content.y = previewY + previewH + 34;
        content.width = contentW - PAD * 2;
        content.height = Math.max(60, winY + winH - content.y - PAD);

        // The tab list grew past what a fixed row height fits at large GUI scales, so the rows
        // shrink to the space available instead of running off the bottom of the sidebar.
        int available = winH - HEADER_H - 20;
        tabSpacing = Math.max(13, Math.min(22, available / Math.max(1, visibleTabs().size())));

        if (simple() && !activeTab.simple) activeTab = Tab.PRESETS;
        rebuild();
    }

    /**
     * Stars, nebulae and aurorae are faded out by daylight, so opening one of their tabs while
     * the preview sits at noon shows an empty sky and reads as the layer being broken. Jumping the
     * preview to midnight is what makes the controls on those tabs do something visible.
     */
    private void ensureNightPreview(Tab tab) {
        if (tab != Tab.STARS && tab != Tab.COSMOS && tab != Tab.AURORA) return;
        AnimationSettings anim = cfg().animation;
        if (!anim.dayNightBlend) return;
        float day = previewTimeValue();
        float sunHeight = (float) Math.cos(day * 2.0 * Math.PI) * 0.5f + 0.5f;
        float start = Math.min(anim.starFadeStart, anim.starFadeEnd);
        float end = Math.max(anim.starFadeStart, anim.starFadeEnd);
        float visibility = 1.0f - Math.max(0.0f, Math.min(1.0f,
                (sunHeight - start) / Math.max(1.0e-4f, end - start)));
        // Anything under about three quarters is too dim to judge a colour or a density against,
        // which is the state that reads as the layer simply not working.
        if (visibility >= 0.75f) return;
        previewFollowConfig = false;
        previewDay = 0.5f;
        setStatus("Preview moved to midnight so you can see what you are editing.");
    }

    private void setStatus(String text) {
        status = text;
        statusUntil = System.currentTimeMillis() + 3200L;
    }

    // ------------------------------------------------------------- building

    private int row;
    private int colW;
    /** Cleared for the one rebuild that follows a tab change, which should start at the top. */
    private boolean rebuildKeepsScroll = true;

    private void rebuild() {
        float keep = rebuildKeepsScroll ? content.scrollTarget() : 0.0f;
        rebuildKeepsScroll = true;
        content.clear();
        content.resetScroll();
        row = 0;
        colW = content.width - 16;
        switch (activeTab) {
            case PRESETS -> buildPresets();
            case SKYBOX -> buildSkybox();
            case LAYERS -> buildLayers();
            case GRADIENTS -> buildGradients();
            case STARS -> buildStars();
            case COSMOS -> buildCosmos();
            case AURORA -> buildAurora();
            case BODIES -> buildBodies();
            case CLOUDS -> buildClouds();
            case ATMOSPHERE -> buildAtmosphere();
            case GUIDE -> buildGuide();
            case INTERFACE -> buildInterface();
            case IMAGE -> buildImage();
            case ANIMATION -> buildAnimation();
            case WORLD -> buildWorld();
            case PROFILES -> buildProfiles();
        }
        content.setContentHeight(row + 8);
        if (keep > 0.0f) content.keepScroll(keep);
    }

    private void header(String text) {
        row += row == 0 ? 0 : 10;
        content.addLabel(text, 0, row, Theme.TEXT_DIM, true);
        row += 18;
    }

    private void note(String text) {
        for (String line : splitNote(text)) {
            content.addLabel(line, 0, row, Theme.TEXT_FAINT, false);
            row += 12;
        }
        row += 2;
    }

    private List<String> splitNote(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return List.of(text);
        List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (mc.font.width(candidate) > colW && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    private Slider slider(String label, double min, double max, java.util.function.DoubleSupplier get,
                          java.util.function.DoubleConsumer set, String tip) {
        Slider s = content.add(new Slider(0, row, colW, label, min, max, get, set));
        s.tooltip = tip;
        row += 30;
        return s;
    }

    private void toggle(String label, java.util.function.BooleanSupplier get,
                        java.util.function.Consumer<Boolean> set, String tip) {
        Toggle t = content.add(new Toggle(0, row, colW, label, get, set));
        t.tooltip = tip;
        row += 22;
    }

    private void color(String label, java.util.function.IntSupplier get,
                       java.util.function.IntConsumer set, boolean alpha, String tip) {
        ColorField f = content.add(new ColorField(0, row, colW, label, get, set, alpha));
        f.tooltip = tip;
        row += 38;
    }

    private void dropdown(String label, String[] options, java.util.function.IntSupplier get,
                          java.util.function.IntConsumer set, String tip) {
        Dropdown d = content.add(new Dropdown(0, row, Math.min(colW, 220), label, options, get, set));
        d.tooltip = tip;
        row += 38;
    }

    private void button(String label, int width, Runnable action, boolean primary, String tip) {
        Button b = new Button(0, row, width, 20, label, action);
        if (primary) b.primary();
        b.tooltip = tip;
        content.add(b);
        row += 26;
    }

    private void buttonRow(String[] labels, Runnable[] actions, int[] widths, boolean[] primary, String[] tips) {
        int cx = 0;
        for (int i = 0; i < labels.length; i++) {
            Button b = new Button(cx, row, widths[i], 20, labels[i], actions[i]);
            if (primary[i]) b.primary();
            if (tips != null && i < tips.length) b.tooltip = tips[i];
            content.add(b);
            cx += widths[i] + 6;
        }
        row += 26;
    }

    private TextField textField(String value, String placeholder, int width,
                                java.util.function.Consumer<String> onCommit, String tip) {
        TextField field = content.add(new TextField(0, row, Math.min(colW, width), 20));
        field.setValue(value);
        field.placeholder(placeholder);
        field.onCommit(onCommit);
        field.tooltip = tip;
        row += 26;
        return field;
    }

    // ------------------------------------------------------- reset actions

    /**
     * Returns the sky to the preset it came from, or to plain defaults when it was never based on
     * one. Both are destructive, so the first click arms and the second confirms.
     */
    private void resetSky() {
        if (System.currentTimeMillis() > resetArmedUntil) {
            resetArmedUntil = System.currentTimeMillis() + 3500L;
            scratchArmedUntil = 0L;
            SkyConfig c = cfg();
            boolean known = !c.basedOn.isBlank() && Presets.builtIn().containsKey(c.basedOn);
            setStatus(known ? "Click again to reset to \"" + c.basedOn + "\"" : "Click again to reset to defaults");
            rebuild();
            return;
        }
        resetArmedUntil = 0L;
        SkyConfig current = cfg();
        SkyConfig replacement = Presets.get(current.basedOn);
        if (replacement == null) {
            // A profile saved against a preset that no longer exists still has to reset to
            // something you can see; a bare SkyConfig has no gradients and renders black.
            replacement = Presets.get("SkyJet Default");
        }
        if (replacement == null) {
            replacement = new SkyConfig();
            replacement.basedOn = "";
        }
        ConfigManager.replaceActive(replacement);
        ConfigManager.save();
        resetLayerSelection();
        rebuild();
        setStatus("Sky reset");
    }

    private void startFromScratch() {
        if (System.currentTimeMillis() > scratchArmedUntil) {
            scratchArmedUntil = System.currentTimeMillis() + 3500L;
            resetArmedUntil = 0L;
            setStatus("Click again to clear every layer");
            rebuild();
            return;
        }
        scratchArmedUntil = 0L;
        ConfigManager.replaceActive(SkyConfig.blank(ConfigManager.activeProfileName()));
        ConfigManager.save();
        resetLayerSelection();
        activeTab = simple() ? Tab.PRESETS : Tab.GRADIENTS;
        rebuildKeepsScroll = false;
        rebuild();
        setStatus("Blank sky. Build it up from here.");
    }

    private void quickActions() {
        boolean resetArmed = System.currentTimeMillis() < resetArmedUntil;
        boolean scratchArmed = System.currentTimeMillis() < scratchArmedUntil;
        buttonRow(
                new String[]{resetArmed ? "Confirm reset" : "Reset sky",
                        scratchArmed ? "Confirm clear" : "Start from scratch"},
                new Runnable[]{this::resetSky, this::startFromScratch},
                new int[]{96, 122},
                new boolean[]{resetArmed, scratchArmed},
                new String[]{
                        "Puts this profile back to the preset it came from, or to plain defaults.",
                        "Empties every layer so you can build a sky from nothing."
                });
    }

    // ------------------------------------------------------------- presets

    private void buildPresets() {
        header("Sky Actions");
        quickActions();

        header("Built-in Skies");
        note("Applying a preset overwrites the current profile.");
        List<String> names = Presets.names();
        for (String name : names) {
            final String presetName = name;
            SkyConfig preset = Presets.builtIn().get(name);
            int swatch = preset != null && !preset.gradients.isEmpty()
                    ? preset.gradients.get(preset.gradients.size() - 1).colorB : Theme.ACCENT;
            PresetRow prow = new PresetRow(0, row, colW, presetName, swatch, describe(preset), () -> {
                SkyConfig applied = Presets.get(presetName);
                if (applied != null) {
                    ConfigManager.replaceActive(applied);
                    ConfigManager.save();
                    resetLayerSelection();
                    rebuild();
                    setStatus("Applied preset: " + presetName);
                }
            });
            prow.tooltip = "Replace the current sky with " + presetName + ".";
            content.add(prow);
            row += 30;
        }
    }

    private static String describe(SkyConfig c) {
        if (c == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(c.gradients.size()).append(" gradients");
        if (!c.stars.isEmpty()) sb.append(" · ").append(c.stars.size()).append(" star layers");
        if (!c.nebulae.isEmpty()) sb.append(" · ").append(c.nebulae.size()).append(" nebulae");
        if (!c.aurorae.isEmpty()) sb.append(" · aurora");
        if (!c.bodies.isEmpty()) sb.append(" · ").append(c.bodies.size()).append(" bodies");
        if (!c.clouds.isEmpty()) sb.append(" · clouds");
        return sb.toString();
    }

    private final class PresetRow extends Widget {
        private final String name;
        private final String sub;
        private final int swatch;
        private final Runnable apply;

        PresetRow(int x, int y, int width, String name, int swatch, String sub, Runnable apply) {
            super(x, y, width, 26);
            this.name = name;
            this.sub = sub;
            this.swatch = swatch;
            this.apply = apply;
        }

        @Override
        public void render(Gfx g, int mouseX, int mouseY, float partial) {
            boolean hover = isOver(mouseX, mouseY);
            g.rounded(x, y, width, height, hover ? Theme.PANEL_HOVER : Theme.PANEL);
            g.outline(x, y, width, height, hover ? Theme.ACCENT : Theme.BORDER_SOFT);
            g.rounded(x + 6, y + 7, 12, 12, swatch);
            g.outline(x + 6, y + 7, 12, 12, Theme.BORDER_SOFT);
            g.text(name, x + 24, y + 4, Theme.TEXT);
            g.text(g.trim(sub, width - 100), x + 24, y + 15, Theme.TEXT_FAINT);
            g.textRight(hover ? "APPLY" : "", x + width - 8, y + 9, Theme.ACCENT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isOver(mouseX, mouseY)) {
                apply.run();
                return true;
            }
            return false;
        }
    }

    private void resetLayerSelection() {
        skyboxIndex = 0;
        gradientIndex = 0;
        starIndex = 0;
        nebulaIndex = 0;
        auroraIndex = 0;
        bodyIndex = 0;
        cloudIndex = 0;
    }

    // -------------------------------------------------------- layer helpers

    private <T> void layerList(String title, List<T> list, int selected, int max,
                               java.util.function.Function<T, String> nameOf,
                               java.util.function.Function<T, Integer> swatchOf,
                               java.util.function.Function<T, Boolean> enabledOf,
                               java.util.function.BiConsumer<T, Boolean> enableSetter,
                               java.util.function.Supplier<T> factory,
                               java.util.function.IntConsumer select) {
        header(title);
        for (int i = 0; i < list.size(); i++) {
            T layer = list.get(i);
            LayerRow lrow = new LayerRow(0, row, colW, nameOf.apply(layer), i, i == selected, swatchOf.apply(layer),
                    () -> enabledOf.apply(layer),
                    v -> enableSetter.accept(layer, v),
                    select,
                    idx -> {
                        if (idx > 0) {
                            T moved = list.remove(idx);
                            list.add(idx - 1, moved);
                            select.accept(idx - 1);
                            rebuild();
                        }
                    },
                    idx -> {
                        if (idx < list.size() - 1) {
                            T moved = list.remove(idx);
                            list.add(idx + 1, moved);
                            select.accept(idx + 1);
                            rebuild();
                        }
                    },
                    idx -> {
                        if (idx < list.size()) {
                            list.remove(idx);
                            select.accept(Math.max(0, Math.min(idx, list.size() - 1)));
                            rebuild();
                        }
                    });
            lrow.tooltip = "Click to edit. The dot switches the layer on and off, the arrows reorder it.";
            content.add(lrow);
            row += 24;
        }
        row += 2;
        if (list.size() < max) {
            button("+ Add layer", 110, () -> {
                list.add(factory.get());
                select.accept(list.size() - 1);
                rebuild();
            }, false, "Add another layer of this kind.");
        } else {
            note("Layer limit reached (" + max + ").");
        }
    }

    // -------------------------------------------------------------- skybox

    private void buildSkybox() {
        SkyConfig c = cfg();

        header("Sky Packs");
        note("SkyJet reads installed sky resource packs directly. Turn the pack on in Options, "
                + "Resource Packs, then import it here.");
        buttonRow(new String[]{"Import installed packs", "Reload textures"},
                new Runnable[]{this::importSkyPacks, () -> {
                    SkyTextures.invalidate();
                    setStatus("Skybox textures reloaded");
                }},
                new int[]{150, 120}, new boolean[]{true, false},
                new String[]{
                        "Scans your resource packs for OptiFine and FabricSkyBoxes sky definitions "
                                + "and turns each one into a layer below.",
                        "Rebuilds the stitched cube sheets. Use after changing resource packs."
                });
        note("Understands OptiFine custom skies (Purple Cosmos, Dramatic Skies) and "
                + "FabricSkyBoxes json skies (Aqua Pack).");

        skyboxIndex = Math.max(0, Math.min(skyboxIndex, Math.max(0, c.skyboxes.size() - 1)));
        layerList("Texture Layers", c.skyboxes, skyboxIndex, SkyConfig.MAX_SKYBOXES,
                s -> s.name, s -> s.tint, s -> s.enabled, (s, v) -> s.enabled = v,
                () -> new SkyboxLayer("Skybox " + (c.skyboxes.size() + 1),
                        "minecraft:textures/gui/title/background/panorama", SkyboxLayer.MODE_CUBE_FACES),
                i -> {
                    skyboxIndex = i;
                    rebuild();
                });

        if (c.skyboxes.isEmpty()) {
            note("No texture layers yet. Import a pack above, or add a layer and point it at any "
                    + "sky art you have.");
            return;
        }

        SkyboxLayer s = c.skyboxes.get(skyboxIndex);
        header("Layer: " + s.name);
        textField(s.name, "Layer name", 200, v -> {
            s.name = v.isBlank() ? "Skybox" : v;
            rebuild();
        }, "What this layer is called in the list.");

        textField(s.texture, "namespace:textures/sky/sky", 320, v -> {
            s.texture = v.trim();
            SkyTextures.invalidate();
            rebuild();
        }, "Resource location of the art. For six-face layers this is the shared prefix, without "
                + "the _top or _north suffix.");

        dropdown("Mapping", SkyboxLayer.MODE_NAMES, () -> s.mode, v -> {
            s.mode = v;
            SkyTextures.invalidate();
        }, "Cube sheet is one 3:2 image holding all six faces, which is what sky packs ship. "
                + "Cube faces is six separate files. Panorama is one wrap-around image.");
        dropdown("Blend mode", SkyboxLayer.BLEND_NAMES, () -> s.blendMode, v -> s.blendMode = v,
                "How this layer mixes with what is behind it. Add is right for glows and star "
                        + "fields, Normal for a solid background.");
        slider("Opacity", 0.0, 1.0, () -> s.opacity, v -> s.opacity = (float) v,
                "How strongly the layer shows at all.");
        slider("Brightness", 0.0, 3.0, () -> s.brightness, v -> s.brightness = (float) v,
                "Multiplies the texture's own colour. Raise it for dark art, lower it for washed out art.");
        color("Tint", () -> s.tint, v -> s.tint = v, false,
                "Colour multiplied over the texture. White leaves it alone.");

        if (!simple()) {
            header("Orientation");
            slider("Rotation", -3.1416, 3.1416, () -> s.yaw, v -> s.yaw = (float) v,
                    "Turns the whole skybox about the vertical axis.");
            slider("Tilt", -1.0, 1.0, () -> s.pitch, v -> s.pitch = (float) v,
                    "Leans the skybox forward or back.");
            slider("Roll", -1.0, 1.0, () -> s.roll, v -> s.roll = (float) v,
                    "Rolls the skybox sideways.");
            toggle("Rotate with the day", () -> s.rotate, v -> s.rotate = v,
                    "Spins the layer once per day, the way sky packs animate their star fields.");
            row += 6;
            slider("Rotation speed", -4.0, 4.0, () -> s.rotationSpeed, v -> s.rotationSpeed = (float) v,
                    "Turns per day. 1 matches the sun.");

            header("When It Shows");
            toggle("Fade with the clock", () -> s.fadeEnabled, v -> s.fadeEnabled = v,
                    "Only show the layer during part of the day. Imported packs set this up for you.");
            row += 6;
            slider("Fade in start", 0, 24000, () -> s.startFadeIn, v -> s.startFadeIn = (float) v,
                    "Tick the layer starts appearing. 0 is sunrise, 6000 noon, 18000 midnight.").decimals(0);
            slider("Fade in end", 0, 24000, () -> s.endFadeIn, v -> s.endFadeIn = (float) v,
                    "Tick the layer reaches full strength.").decimals(0);
            slider("Fade out start", 0, 24000, () -> s.startFadeOut, v -> s.startFadeOut = (float) v,
                    "Tick the layer starts disappearing.").decimals(0);
            slider("Fade out end", 0, 24000, () -> s.endFadeOut, v -> s.endFadeOut = (float) v,
                    "Tick the layer is fully gone.").decimals(0);
            toggle("Show in clear weather", () -> s.inClear, v -> s.inClear = v, "Show when it is not raining.");
            toggle("Show in rain", () -> s.inRain, v -> s.inRain = v, "Show while it rains.");
            toggle("Show in thunder", () -> s.inThunder, v -> s.inThunder = v, "Show during a thunderstorm.");

            header("Extras");
            slider("Night only", 0.0, 1.0, () -> s.nightOnly, v -> s.nightOnly = (float) v,
                    "Fades the layer out in daylight, like a star field.");
            toggle("Treat black as transparent", () -> s.blackIsTransparent, v -> s.blackIsTransparent = v,
                    "For overlay art drawn on black instead of with transparency.");
            row += 6;
        }
    }

    private void importSkyPacks() {
        SkyPackScanner.Result found = SkyPackScanner.scan();
        if (found.layers().isEmpty()) {
            setStatus("No sky packs found. Enable one in Resource Packs first.");
            return;
        }
        SkyConfig c = cfg();
        c.skyboxes.clear();
        int added = 0;
        for (SkyboxLayer layer : found.layers()) {
            if (c.skyboxes.size() >= SkyConfig.MAX_SKYBOXES) break;
            c.skyboxes.add(layer);
            added++;
        }
        // A pack that hides the vanilla sun also wants SkyJet to leave it hidden.
        if (found.hideVanillaSun()) c.world.disableVanillaSun = true;
        if (found.hideVanillaMoon()) c.world.disableVanillaMoon = true;
        if (found.hideVanillaStars()) c.world.disableVanillaStars = true;
        SkyTextures.invalidate();
        skyboxIndex = 0;
        ConfigManager.save();
        rebuild();
        int skipped = found.layers().size() - added;
        setStatus(skipped > 0
                ? "Imported " + added + " layers (" + skipped + " over the limit)"
                : "Imported " + added + " sky layers");
    }

    // -------------------------------------------------------------- layers

    private void buildLayers() {
        SkyConfig c = cfg();
        header("Whole Sky");
        slider("Animation speed", 0.0, 4.0, () -> c.layers.animationSpeed,
                v -> c.layers.animationSpeed = (float) v,
                "One speed for everything that moves: clouds, aurorae, orbits, star rotation.");
        color("Master tint", () -> c.layers.masterTint, v -> c.layers.masterTint = v, false,
                "Colour multiplied over the finished sky. White leaves it alone.");

        header("Stacking Order");
        note("Layers are drawn top to bottom, so the last one sits in front. The arrows move a "
                + "layer through the stack.");
        for (int i = 0; i < c.layers.entries.size(); i++) {
            LayerStack.Entry e = c.layers.entries.get(i);
            final int index = i;
            String name = LayerStack.CATEGORY_NAMES[
                    Math.max(0, Math.min(LayerStack.CATEGORY_NAMES.length - 1, e.category))];
            LayerRow lrow = new LayerRow(0, row, colW, name, i, false, e.tint,
                    () -> e.enabled,
                    v -> e.enabled = v,
                    idx -> {
                    },
                    idx -> {
                        if (idx > 0) {
                            c.layers.entries.add(idx - 1, c.layers.entries.remove(idx));
                            rebuild();
                        }
                    },
                    idx -> {
                        if (idx < c.layers.entries.size() - 1) {
                            c.layers.entries.add(idx + 1, c.layers.entries.remove(idx));
                            rebuild();
                        }
                    },
                    idx -> {
                        // Categories are never removed, only switched off.
                        c.layers.entries.get(idx).enabled = false;
                        rebuild();
                    });
            lrow.tooltip = "The " + name + " layer group. The dot turns the whole group off.";
            content.add(lrow);
            row += 24;

            if (!simple()) {
                slider("   " + name + " opacity", 0.0, 1.0, () -> e.opacity,
                        v -> c.layers.entries.get(index).opacity = (float) v,
                        "How strongly the " + name + " group shows.");
            }
        }

        if (!simple()) {
            header("Per Group Colour");
            note("Pick a group above, then tint or reblend it without touching its own layers.");
            for (int i = 0; i < c.layers.entries.size(); i++) {
                LayerStack.Entry e = c.layers.entries.get(i);
                String name = LayerStack.CATEGORY_NAMES[
                        Math.max(0, Math.min(LayerStack.CATEGORY_NAMES.length - 1, e.category))];
                color(name + " tint", () -> e.tint, v -> e.tint = v, false,
                        "Colour multiplied over the " + name + " group.");
            }
        }
    }

    // ----------------------------------------------------------- gradients

    private void buildGradients() {
        SkyConfig c = cfg();
        gradientIndex = Math.max(0, Math.min(gradientIndex, Math.max(0, c.gradients.size() - 1)));
        layerList("Gradient Layers", c.gradients, gradientIndex, SkyConfig.MAX_GRADIENTS,
                g -> g.name, g -> g.colorB, g -> g.enabled, (g, v) -> g.enabled = v,
                () -> new GradientLayer("Gradient " + (c.gradients.size() + 1), 0xFF101828, 0xFF4EA1FF, 0.5f, 0.35f),
                i -> {
                    gradientIndex = i;
                    rebuild();
                });

        if (c.gradients.isEmpty()) {
            note("Add a gradient layer to start shaping the sky.");
            return;
        }
        GradientLayer g = c.gradients.get(gradientIndex);
        header("Layer: " + g.name);
        textField(g.name, "Layer name", 200, v -> {
            g.name = v.isBlank() ? "Gradient" : v;
            rebuild();
        }, "What this layer is called in the list.");

        color("Colour A (low end)", () -> g.colorA, v -> g.colorA = v, true,
                "Colour at the bottom of the axis. Its transparency matters too.");
        color("Colour B (high end)", () -> g.colorB, v -> g.colorB = v, true,
                "Colour at the top of the axis. Set its transparency to zero to fade the layer out.");
        dropdown("Axis", GradientLayer.AXIS_NAMES, () -> g.axis, v -> g.axis = v,
                "Which direction the gradient runs. Vertical is nadir to zenith.");
        dropdown("Blend mode", GradientLayer.BLEND_NAMES, () -> g.blendMode, v -> g.blendMode = v,
                "How this layer mixes with the ones under it.");
        slider("Position", 0.0, 1.0, () -> g.position, v -> g.position = (float) v,
                "Where the colour change sits. On the vertical axis 0.5 is the horizon.");
        slider("Softness", 0.002, 1.0, () -> g.softness, v -> g.softness = (float) v,
                "How wide the transition is. Small values give a hard band.").decimals(3);
        slider("Opacity", 0.0, 1.0, () -> g.opacity, v -> g.opacity = (float) v,
                "Strength of the whole layer.");
        if (!simple()) {
            slider("Angle", -3.1416, 3.1416, () -> g.angle, v -> g.angle = (float) v,
                    "Rotates the axis, for the compass and directional modes.");
            slider("Noise amount", 0.0, 1.0, () -> g.noiseAmount, v -> g.noiseAmount = (float) v,
                    "Breaks the gradient edge up with drifting noise.");
            slider("Noise scale", 0.1, 20.0, () -> g.noiseScale, v -> g.noiseScale = (float) v,
                    "Size of that noise. Higher is finer.").decimals(1);
        }
        note("On the vertical axis 0 is straight down, 0.5 the horizon and 1 straight up.");
    }

    // --------------------------------------------------------------- stars

    private void buildStars() {
        SkyConfig c = cfg();
        starIndex = Math.max(0, Math.min(starIndex, Math.max(0, c.stars.size() - 1)));
        layerList("Star Layers", c.stars, starIndex, SkyConfig.MAX_STARS,
                s -> s.name, s -> s.color, s -> s.enabled, (s, v) -> s.enabled = v,
                () -> {
                    StarLayer s = new StarLayer("Stars " + (c.stars.size() + 1), 0xFFFFFFFF, 0.4f, 0.5f, 1.0f, 1.0f);
                    s.seed = c.stars.size() * 37 + 11;
                    return s;
                },
                i -> {
                    starIndex = i;
                    rebuild();
                });

        if (c.stars.isEmpty()) {
            note("Star layers stack to create depth. Give each layer a different depth value.");
            return;
        }
        StarLayer s = c.stars.get(starIndex);
        header("Layer: " + s.name);
        textField(s.name, "Layer name", 200, v -> {
            s.name = v.isBlank() ? "Stars" : v;
            rebuild();
        }, "What this layer is called in the list.");

        header("Look");
        color("Primary colour", () -> s.color, v -> s.color = v, false, "Base star colour.");
        color("Secondary colour", () -> s.colorB, v -> s.colorB = v, false,
                "Stars mix between the two colours at random, which is what stops a field looking flat.");
        dropdown("Shape", StarLayer.SHAPE_NAMES, () -> s.shape, v -> s.shape = v,
                "Point is a round star. Square pixel and Cube look like Minecraft particles. "
                        + "Cross, Ring and Diamond are figures.");
        slider("Glow", 0.0, 1.0, () -> s.glow, v -> s.glow = (float) v,
                "Halo around each star.");
        if (!simple()) {
            slider("Colour variance", 0.0, 1.0, () -> s.colorVariance, v -> s.colorVariance = (float) v,
                    "How far individual stars drift from the chosen colours.");
        }

        header("Pattern");
        slider("Seed", 0, 64, () -> s.seed, v -> s.seed = (int) v,
                "Reshuffles where the stars sit. Two layers sharing a seed put their stars in the "
                        + "same places, which reads as one repeated pattern.").integer();
        slider("Scatter", 0.0, 1.0, () -> s.jitter, v -> s.jitter = (float) v,
                "How far each star may wander inside its cell. At 0 the field sits on a visible grid.");
        button("Randomise", 100, () -> {
            s.seed = (int) (Math.random() * 64.0);
            s.jitter = 0.7f + (float) Math.random() * 0.3f;
            setStatus("New star pattern");
        }, false, "Picks a fresh seed and scatter for this layer.");

        header("Size & Density");
        slider("Density", 0.01, 1.0, () -> s.density, v -> s.density = (float) v,
                "How many stars there are.");
        slider("Size", 0.05, 2.0, () -> s.size, v -> s.size = (float) v, "How big each star is.");
        slider("Brightness", 0.0, 3.0, () -> s.brightness, v -> s.brightness = (float) v,
                "How strongly the field shows.");
        if (!simple()) {
            slider("Size variance", 0.0, 1.0, () -> s.sizeVariance, v -> s.sizeVariance = (float) v,
                    "Spread between the smallest and largest stars.");
            slider("Minimum pixel size", 0.4, 3.0, () -> s.minPixelSize, v -> s.minPixelSize = (float) v,
                    "Floor on a star's on-screen size. Below one pixel a star falls between samples "
                            + "and only shows when you zoom in.");
            slider("Hero star fraction", 0.0, 0.5, () -> s.sparkleAmount, v -> s.sparkleAmount = (float) v,
                    "Share of the field drawn brighter and larger.");
        }

        header("Motion");
        toggle("Stand still", () -> s.frozen, v -> s.frozen = v,
                "Stops this field moving completely: no rotation, no drift, no twinkle.");
        row += 6;
        slider("Rotation speed", -8.0, 8.0, () -> s.rotationSpeed, v -> s.rotationSpeed = (float) v,
                "Turns the field about the celestial axis.");
        if (!simple()) {
            slider("Drift speed", -4.0, 4.0, () -> s.driftSpeed, v -> s.driftSpeed = (float) v,
                    "Slow independent turn about the vertical, on top of the rotation.");
            slider("Orbit tilt", -1.5, 1.5, () -> s.orbitTilt, v -> s.orbitTilt = (float) v,
                    "Leans the axis the field turns about.");
            slider("Parallax depth", 0.0, 6.0, () -> s.depth, v -> s.depth = (float) v,
                    "How much the field shifts as you move. Give each layer a different value.");
        }
        slider("Twinkle speed", 0.0, 6.0, () -> s.twinkleSpeed, v -> s.twinkleSpeed = (float) v,
                "How fast stars pulse.");
        slider("Twinkle amount", 0.0, 1.0, () -> s.twinkleAmount, v -> s.twinkleAmount = (float) v,
                "How much they pulse. Zero holds them steady.");

        if (!simple()) {
            header("Clustering");
            slider("Band height", -1.0, 1.0, () -> s.bandBias, v -> s.bandBias = (float) v,
                    "Pulls the field toward a height. 0 is the horizon, 1 straight up.");
            slider("Band tightness", 0.2, 6.0, () -> s.bandTightness, v -> s.bandTightness = (float) v,
                    "How narrow that band is.");
        }
    }

    // -------------------------------------------------------------- cosmos

    private void buildCosmos() {
        SkyConfig c = cfg();
        nebulaIndex = Math.max(0, Math.min(nebulaIndex, Math.max(0, c.nebulae.size() - 1)));
        layerList("Nebulae & Galaxies", c.nebulae, nebulaIndex, SkyConfig.MAX_NEBULAE,
                n -> n.name, n -> n.colorA, n -> n.enabled, (n, v) -> n.enabled = v,
                () -> new NebulaLayer("Nebula " + (c.nebulae.size() + 1), 0xFF5B2BA8, 0xFF1B6FB5, 0.6f, 3.0f, 0),
                i -> {
                    nebulaIndex = i;
                    rebuild();
                });

        if (c.nebulae.isEmpty()) {
            note("Deep space volumes: bands, spirals and filaments with their own parallax depth.");
            return;
        }
        NebulaLayer n = c.nebulae.get(nebulaIndex);
        header("Layer: " + n.name);
        textField(n.name, "Layer name", 200, v -> {
            n.name = v.isBlank() ? "Nebula" : v;
            rebuild();
        }, "What this layer is called in the list.");

        color("Primary colour", () -> n.colorA, v -> n.colorA = v, false, "Main cloud colour.");
        color("Secondary colour", () -> n.colorB, v -> n.colorB = v, false,
                "Colour the denser parts shift toward.");
        dropdown("Shape", NebulaLayer.SHAPE_NAMES, () -> n.shape, v -> n.shape = v,
                "Clouds fill the sky, Galactic Band makes a stripe, Spiral Arm sweeps, "
                        + "Filaments are thin threads.");
        slider("Intensity", 0.0, 2.0, () -> n.intensity, v -> n.intensity = (float) v, "How bright it is.");
        slider("Scale", 0.2, 12.0, () -> n.scale, v -> n.scale = (float) v,
                "Size of the cloud structure. Higher is finer.").decimals(1);
        slider("Contrast", 0.05, 2.0, () -> n.contrast, v -> n.contrast = (float) v,
                "Separation between the wisps and the gaps.");
        slider("Elevation bias", -1.0, 1.0, () -> n.elevationBias, v -> n.elevationBias = (float) v,
                "Moves the band up or down. Raise it to put the cloud over the player instead of "
                        + "along the horizon.");
        if (!simple()) {
            slider("Parallax depth", 0.0, 6.0, () -> n.depth, v -> n.depth = (float) v,
                    "How much it shifts as you move.");
            slider("Detail octaves", 2, 7, () -> n.octaves, v -> n.octaves = (int) v,
                    "Levels of detail. Each one costs GPU time.").integer();
            slider("Drift speed", -6.0, 6.0, () -> n.driftSpeed, v -> n.driftSpeed = (float) v,
                    "How fast the volume turns.");
            slider("Domain warp", 0.0, 2.0, () -> n.warp, v -> n.warp = (float) v,
                    "Swirls the cloud shapes. Zero gives plain blobs.");
        }
    }

    // -------------------------------------------------------------- aurora

    private void buildAurora() {
        SkyConfig c = cfg();
        auroraIndex = Math.max(0, Math.min(auroraIndex, Math.max(0, c.aurorae.size() - 1)));
        layerList("Aurora Curtains", c.aurorae, auroraIndex, SkyConfig.MAX_AURORAE,
                a -> a.name, a -> a.colorBase, a -> a.enabled, (a, v) -> a.enabled = v,
                () -> new AuroraLayer("Aurora " + (c.aurorae.size() + 1), 0xFF39FFA6, 0xFFB03CFF,
                        0.85f, 120.0f, 520.0f),
                i -> {
                    auroraIndex = i;
                    rebuild();
                });

        if (c.aurorae.isEmpty()) {
            note("Add a curtain to start. Borealis is modelled on the real thing: a sheet hanging "
                    + "off an arc, with a hard lower edge and vertical rays.");
            return;
        }
        AuroraLayer a = c.aurorae.get(auroraIndex);
        header("Layer: " + a.name);
        textField(a.name, "Layer name", 200, v -> {
            a.name = v.isBlank() ? "Aurora" : v;
            rebuild();
        }, "What this layer is called in the list.");

        dropdown("Style", AuroraLayer.STYLE_NAMES, () -> a.style, v -> {
            a.style = v;
            rebuild();
        }, "Borealis is the real aurora: an arc with a sharp bottom edge, vertical rays and a "
                + "colour ramp that follows altitude. Veil is the older look, a soft ridged "
                + "sheet painted across the sky.");

        if (a.style == AuroraLayer.STYLE_BOREALIS) {
            buildAuroraBorealis(a);
        } else {
            buildAuroraVeil(a);
        }
    }

    private void buildAuroraBorealis(AuroraLayer a) {
        header("Colour");
        note("A real aurora is layered by altitude rather than by taste: pink at the very bottom, "
                + "green through the body, red up top where the air is thin.");
        color("Lower fringe", () -> a.colorLow, v -> a.colorLow = v, false,
                "The pink N2+ fringe under the curtain. Only shows on bright displays.");
        color("Body", () -> a.colorBase, v -> a.colorBase = v, false,
                "The main colour. Oxygen green at 557.7nm is the one everybody pictures.");
        color("Top", () -> a.colorTip, v -> a.colorTip = v, false,
                "The diffuse top. Deep red on strong nights, violet if you want it unreal.");
        slider("Intensity", 0.0, 2.0, () -> a.intensity, v -> a.intensity = (float) v,
                "Overall brightness.");

        header("The arc");
        slider("Bearing", 0.0, 360.0, () -> a.arcAzimuth, v -> a.arcAzimuth = (float) v,
                "Compass direction the arc sits on. 0 is north, which is where it usually is.")
                .decimals(0);
        slider("Distance", 150.0, 3000.0, () -> a.arcDistance, v -> a.arcDistance = (float) v,
                "How far out the arc hangs. Close puts it overhead, far drops it to the horizon.")
                .decimals(0);
        slider("Curtain thickness", 20.0, 500.0, () -> a.arcWidth, v -> a.arcWidth = (float) v,
                "How thick the sheet is across its face. Real curtains are only a few kilometres "
                        + "thick, which is why they look like hanging fabric.").decimals(0);
        slider("Wrap", 0.05, 1.0, () -> a.curvature, v -> a.curvature = (float) v,
                "Low draws a straight band across the sky. High wraps the arc around you, which "
                        + "is what a display directly overhead looks like.");
        slider("Curtains", 1, 3, () -> a.bands, v -> a.bands = (int) v,
                "How many parallel curtains hang together. Active displays stack up.").integer();
        slider("Curtain spacing", 0.0, 2.0, () -> a.bandSpacing, v -> a.bandSpacing = (float) v,
                "Gap between those curtains.");

        header("Folds and rays");
        slider("Fold amount", 0.0, 3.0, () -> a.foldAmount, v -> a.foldAmount = (float) v,
                "How far the curtain snakes sideways. Zero is a quiet arc, high is the curls and "
                        + "spirals of a substorm.");
        slider("Fold size", 0.2, 4.0, () -> a.foldScale, v -> a.foldScale = (float) v,
                "Long lazy curves at low values, tight curls at high.");
        slider("Fold speed", 0.0, 3.0, () -> a.foldSpeed, v -> a.foldSpeed = (float) v,
                "How fast the folds travel along the arc.");
        slider("Ray strength", 0.0, 1.0, () -> a.rayAmount, v -> a.rayAmount = (float) v,
                "The vertical striations. This is what stops it reading as a flat green smear. "
                        + "Rays run along magnetic field lines, so they hold still while the "
                        + "curtain folds past them.");
        slider("Ray fineness", 0.2, 4.0, () -> a.rayScale, v -> a.rayScale = (float) v,
                "How closely packed the rays are.");
        slider("Ray contrast", 0.0, 1.0, () -> a.raySharpness, v -> a.raySharpness = (float) v,
                "How dark the gap between two rays gets.");

        if (!simple()) {
            header("Shape and motion");
            slider("Bottom edge", 30.0, 900.0, () -> a.baseHeight, v -> a.baseHeight = (float) v,
                    "Where the curtain stops, in blocks above you. The real one stops dead at "
                            + "about 100km, where the air is thick enough to quench it.").decimals(0);
            slider("Top", 100.0, 2000.0, () -> a.topHeight, v -> a.topHeight = (float) v,
                    "How high it reaches.").decimals(0);
            slider("Edge hardness", 0.0, 1.0, () -> a.bottomSharpness,
                    v -> a.bottomSharpness = (float) v,
                    "How abruptly it starts at the bottom. High is the knife edge of a real "
                            + "curtain, and is most of what sells it.");
            slider("Top falloff", 0.0, 1.0, () -> a.topFalloff, v -> a.topFalloff = (float) v,
                    "How quickly it fades out toward the top.");
            slider("Depth shading", 0.0, 1.0, () -> a.depthFade, v -> a.depthFade = (float) v,
                    "How much dimmer the far curtains are than the near ones.");
            slider("Animation speed", 0.0, 3.0, () -> a.speed, v -> a.speed = (float) v,
                    "Master speed for everything that moves.");
            slider("Substorm pulse", 0.0, 1.0, () -> a.pulse, v -> a.pulse = (float) v,
                    "The whole display breathing brighter and dimmer, the way it does when a "
                            + "substorm builds.");
            slider("Pulse speed", 0.0, 3.0, () -> a.pulseSpeed, v -> a.pulseSpeed = (float) v,
                    "How fast that breathing runs.");
            slider("Drift", 0.0, 2.0, () -> a.drift, v -> a.drift = (float) v,
                    "Slow rotation of the whole arc across the sky.");
            slider("Ground glow", 0.0, 2.0, () -> a.groundGlow, v -> a.groundGlow = (float) v,
                    "Light spilled down onto the horizon under the arc.");
            slider("Height samples", 5, 16, () -> a.steps, v -> a.steps = (int) v,
                    "Samples taken up the curtain. Ten is clean; five still reads as an aurora "
                            + "and costs half as much.").integer();
        }
    }

    private void buildAuroraVeil(AuroraLayer a) {
        color("Base colour", () -> a.colorBase, v -> a.colorBase = v, false,
                "Colour at the bottom of the veil.");
        color("Tip colour", () -> a.colorTip, v -> a.colorTip = v, false, "Colour at the top.");
        slider("Intensity", 0.0, 2.0, () -> a.intensity, v -> a.intensity = (float) v,
                "How bright it is.");
        slider("Coverage", 0.0, 1.0, () -> a.coverage, v -> a.coverage = (float) v,
                "How much of the sky has a veil in front of it.");
        slider("Animation speed", 0.0, 3.0, () -> a.speed, v -> a.speed = (float) v,
                "How fast it moves.");

        header("Depth");
        slider("Thickness", 0.0, 600.0, () -> a.thickness, v -> a.thickness = (float) v,
                "How deep the veil is along your line of sight. Zero gives a flat sheet painted "
                        + "on the sky; raising it puts a near face and a far face in the volume.")
                .decimals(0);
        slider("Bands", 1, 3, () -> a.bands, v -> a.bands = (int) v,
                "How many parallel sheets run through that depth.").integer();
        slider("Band spacing", 0.05, 2.0, () -> a.bandSpacing, v -> a.bandSpacing = (float) v,
                "Separation between those sheets.");
        slider("Depth shading", 0.0, 1.0, () -> a.depthFade, v -> a.depthFade = (float) v,
                "How much dimmer the far bands are than the near ones.");

        if (!simple()) {
            header("Shape");
            slider("Base height", 4.0, 600.0, () -> a.baseHeight, v -> a.baseHeight = (float) v,
                    "Altitude the veil starts at, in blocks.").decimals(0);
            slider("Top height", 40.0, 1400.0, () -> a.topHeight, v -> a.topHeight = (float) v,
                    "Altitude it reaches.").decimals(0);
            slider("Waviness", 0.0, 4.0, () -> a.waviness, v -> a.waviness = (float) v,
                    "How much the veil folds sideways.");
            slider("Sharpness", 0.0, 2.0, () -> a.sharpness, v -> a.sharpness = (float) v,
                    "Crisper ribbons at higher values.");
            slider("Ground glow", 0.0, 2.0, () -> a.groundGlow, v -> a.groundGlow = (float) v,
                    "Light spilled down onto the horizon.");
            slider("Height samples", 4, 12, () -> a.steps, v -> a.steps = (int) v,
                    "Samples taken up the veil. More is smoother and costs GPU time.").integer();
        }
    }

    // -------------------------------------------------------------- bodies

    private void buildBodies() {
        SkyConfig c = cfg();
        bodyIndex = Math.max(0, Math.min(bodyIndex, Math.max(0, c.bodies.size() - 1)));
        layerList("Celestial Bodies", c.bodies, bodyIndex, SkyConfig.MAX_BODIES,
                b -> b.name, b -> b.color, b -> b.enabled, (b, v) -> b.enabled = v,
                () -> new BodyLayer("Body " + (c.bodies.size() + 1), 0xFFB98A5A, 0.0f, 0.4f, 0.06f, 0),
                i -> {
                    bodyIndex = i;
                    rebuild();
                });

        if (c.bodies.isEmpty()) {
            note("Planets, ringed worlds, glowing orbs, crescents and black holes.");
            return;
        }
        BodyLayer b = c.bodies.get(bodyIndex);
        header("Layer: " + b.name);
        textField(b.name, "Layer name", 200, v -> {
            b.name = v.isBlank() ? "Body" : v;
            rebuild();
        }, "What this layer is called in the list.");

        header("Placement");
        button(placingBody ? "Click the preview to place" : "Place in preview", 168, () -> {
            placingBody = !placingBody;
            rebuild();
            setStatus(placingBody ? "Click or drag inside the preview to move this body" : "Placement off");
        }, placingBody, "Drop the body wherever you click in the preview above.");
        slider("Azimuth", -3.1416, 3.1416, () -> b.azimuth, v -> b.azimuth = (float) v,
                "Compass direction.");
        slider("Elevation", -1.5708, 1.5708, () -> b.elevation, v -> b.elevation = (float) v,
                "Height above the horizon. 1.57 is straight up.");
        slider("Size", 0.002, 0.45, () -> b.size, v -> b.size = (float) v, "How large it appears.").decimals(3);

        header("Look");
        dropdown("Type", BodyLayer.TYPE_NAMES, () -> b.type, v -> b.type = v, "What kind of body this is.");
        color("Lit colour", () -> b.color, v -> b.color = v, false, "Colour of the sunlit side.");
        color("Dark side colour", () -> b.shadowColor, v -> b.shadowColor = v, false,
                "Colour of the side facing away from the sun.");
        slider("Intensity", 0.0, 3.0, () -> b.intensity, v -> b.intensity = (float) v, "Overall brightness.");
        slider("Glow", 0.0, 1.0, () -> b.glow, v -> b.glow = (float) v, "Halo around the body.");
        if (!simple()) {
            slider("Sun shading", 0.0, 1.0, () -> b.shading, v -> b.shading = (float) v,
                    "How strongly the sun carves a terminator across it.");
            slider("Surface detail", 0.0, 1.0, () -> b.detail, v -> b.detail = (float) v,
                    "Banding and mottling on the surface.");
            slider("Ring width", 0.0, 2.0, () -> b.ringWidth, v -> b.ringWidth = (float) v,
                    "Only used by the ringed planet type.");
            slider("Ring tilt", 0.05, 1.0, () -> b.ringTilt, v -> b.ringTilt = (float) v,
                    "How edge-on the ring sits.");
        }

        header("Motion");
        dropdown("Motion", BodyLayer.MOTION_NAMES, () -> b.motion, v -> b.motion = v,
                "Fixed stays put. Orbit runs on its own clock. Follow day cycle rises and sets "
                        + "with the sun. Drift sways around the compass.");
        slider("Orbit speed", -4.0, 4.0, () -> b.orbitSpeed, v -> b.orbitSpeed = (float) v, "How fast it moves.");
        if (!simple()) {
            slider("Orbit tilt", -1.5, 1.5, () -> b.orbitTilt, v -> b.orbitTilt = (float) v,
                    "Leans the orbit's axis.");
            slider("Orbit phase", -3.1416, 3.1416, () -> b.orbitPhase, v -> b.orbitPhase = (float) v,
                    "Where along the orbit it starts.");
            slider("Surface spin", -4.0, 4.0, () -> b.spinSpeed, v -> b.spinSpeed = (float) v,
                    "Turns the surface detail, so a planet is not a frozen sticker.");
        }
    }

    // -------------------------------------------------------------- clouds

    private void buildClouds() {
        SkyConfig c = cfg();
        cloudIndex = Math.max(0, Math.min(cloudIndex, Math.max(0, c.clouds.size() - 1)));
        layerList("Cloud Decks", c.clouds, cloudIndex, SkyConfig.MAX_CLOUDS,
                cl -> cl.name, cl -> cl.color, cl -> cl.enabled, (cl, v) -> cl.enabled = v,
                () -> new CloudLayer("Deck " + (c.clouds.size() + 1), 0, 0.45f,
                        220.0f + c.clouds.size() * 180.0f),
                i -> {
                    cloudIndex = i;
                    rebuild();
                });

        if (c.clouds.isEmpty()) {
            note("Stack decks at different altitudes and speeds to give the sky real weather.");
            return;
        }
        CloudLayer cl = c.clouds.get(cloudIndex);
        header("Deck: " + cl.name);
        textField(cl.name, "Deck name", 200, v -> {
            cl.name = v.isBlank() ? "Clouds" : v;
            rebuild();
        }, "What this deck is called in the list.");

        dropdown("Cloud type", CloudLayer.TYPE_NAMES, () -> cl.type, v -> cl.type = v,
                "Cumulus are piled heaps, Stratus a flat sheet, Cirrus thin streaks, Storm a "
                        + "ridged anvil.");
        color("Lit colour", () -> cl.color, v -> cl.color = v, false, "Colour of the sunlit tops.");
        color("Shadow colour", () -> cl.shadowColor, v -> cl.shadowColor = v, false,
                "Colour of the shaded undersides.");
        slider("Opacity", 0.0, 1.0, () -> cl.opacity, v -> cl.opacity = (float) v, "How solid the deck is.");
        slider("Coverage", 0.0, 1.0, () -> cl.coverage, v -> cl.coverage = (float) v,
                "How much of the sky the deck fills.");
        slider("Quantity", 0.1, 6.0, () -> cl.quantity, v -> cl.quantity = (float) v,
                "How many separate cloud masses form. Higher breaks one sheet into many clouds "
                        + "without changing how much sky they cover.");
        slider("Altitude", 20.0, 1200.0, () -> cl.altitude, v -> cl.altitude = (float) v,
                "How high the deck sits. Stack decks at different heights for depth.").decimals(0);

        header("Weather");
        slider("Wind speed", -6.0, 6.0, () -> cl.speed, v -> cl.speed = (float) v,
                "How fast the deck drifts.");
        slider("Wind direction", -3.1416, 3.1416, () -> cl.windAngle, v -> cl.windAngle = (float) v,
                "Which way it drifts.");
        slider("Shape churn", 0.0, 4.0, () -> cl.evolveSpeed, v -> cl.evolveSpeed = (float) v,
                "How fast clouds build and break up in place, on top of the drift.");

        if (!simple()) {
            header("Detail");
            slider("Density", 0.2, 4.0, () -> cl.density, v -> cl.density = (float) v,
                    "How thick the cloud material is.");
            slider("Sharpness", 0.0, 1.0, () -> cl.sharpness, v -> cl.sharpness = (float) v,
                    "Crisper edges at higher values.");
            slider("Self shadowing", 0.0, 1.5, () -> cl.selfShadow, v -> cl.selfShadow = (float) v,
                    "How dark the insides go.");
            slider("Scale", 0.2, 4.0, () -> cl.scale, v -> cl.scale = (float) v, "Overall size of the shapes.");
            slider("Detail octaves", 2, 7, () -> cl.octaves, v -> cl.octaves = (int) v,
                    "Levels of detail. Each one costs GPU time.").integer();
        }
    }

    // ---------------------------------------------------------- atmosphere

    private void buildAtmosphere() {
        SkyConfig c = cfg();
        header("Atmospheric Scattering");
        toggle("Enable scattering", () -> c.atmosphereEnabled, v -> c.atmosphereEnabled = v,
                "Physically shaded daytime blue and sunset colour. Turn it off for a space sky.");
        row += 6;
        slider("Rayleigh (blue sky)", 0.0, 4.0, () -> c.rayleigh, v -> c.rayleigh = (float) v,
                "Strength of the blue daylight scattering.");
        slider("Mie (haze)", 0.0, 4.0, () -> c.mie, v -> c.mie = (float) v,
                "Strength of the white haze around the sun.");
        slider("Density", 0.0, 3.0, () -> c.atmosphereDensity, v -> c.atmosphereDensity = (float) v,
                "How much air there is overall.");
        color("Atmosphere tint", () -> c.atmosphereTint, v -> c.atmosphereTint = v, false,
                "Colours the whole atmosphere, for alien skies.");
        slider("Sunset glow", 0.0, 3.0, () -> c.sunsetGlow, v -> c.sunsetGlow = (float) v,
                "Strength of the warm band at sunrise and sunset.");
        if (!simple()) {
            slider("Mie directionality", -0.95, 0.95, () -> c.mieG, v -> c.mieG = (float) v,
                    "How tightly the haze hugs the sun.");
        }

        header("Sun & Moon");
        color("Sun colour", () -> c.sunColor, v -> c.sunColor = v, false,
                "Also tints scattering, haze and cloud lighting.");
        toggle("Draw SkyJet sun disc", () -> c.drawSun, v -> c.drawSun = v,
                "Draws SkyJet's own sun. The vanilla one is hidden by default in the World tab.");
        row += 6;
        slider("Sun size", 0.002, 0.30, () -> c.sunSize, v -> c.sunSize = (float) v, "How large it is.").decimals(3);
        color("Moon colour", () -> c.moonColor, v -> c.moonColor = v, false, "Colour of SkyJet's moon.");
        toggle("Draw SkyJet moon disc", () -> c.drawMoon, v -> c.drawMoon = v, "Draws SkyJet's own moon.");
        row += 6;
        slider("Moon size", 0.002, 0.30, () -> c.moonSize, v -> c.moonSize = (float) v, "How large it is.").decimals(3);

        header("Horizon & Below");
        dropdown("Below the horizon", SkyConfig.BELOW_NAMES, () -> c.belowHorizon, v -> c.belowHorizon = v,
                "Continue carries the sky's own colours under you. Mirror reflects the sky above. "
                        + "Solid ground paints a flat colour.");
        color("Ground colour", () -> c.groundColor, v -> c.groundColor = v, false,
                "Only used by the solid ground option.");
        if (!simple()) {
            slider("Horizon offset", -0.5, 0.5, () -> c.horizonOffset, v -> c.horizonOffset = (float) v,
                    "Raises or lowers where the sky treats the horizon as being.");
            slider("Horizon softness", 0.002, 0.5, () -> c.horizonSoftness,
                    v -> c.horizonSoftness = (float) v, "How soft the horizon line is.").decimals(3);
            slider("Horizon curve", 0.05, 3.0, () -> c.horizonCurve, v -> c.horizonCurve = (float) v,
                    "Bends the vertical gradient toward or away from the horizon.");

            header("Fog Blending");
            slider("Blend sky into fog", 0.0, 1.0, () -> c.fogBlend, v -> c.fogBlend = (float) v,
                    "How much the world's fog colour bleeds into the sky at the horizon.");
            slider("Fog band height", 0.01, 0.5, () -> c.fogHeight, v -> c.fogHeight = (float) v,
                    "How far up that blend reaches.").decimals(3);
        }
    }

    // --------------------------------------------------------------- image

    private void buildImage() {
        SkyConfig c = cfg();
        header("Image");
        slider("Master opacity", 0.0, 1.0, () -> c.opacity, v -> c.opacity = (float) v,
                "How strongly SkyJet's sky shows over whatever is behind it.");
        slider("Exposure", 0.0, 4.0, () -> c.exposure, v -> c.exposure = (float) v, "Overall brightness.");
        slider("Saturation", 0.0, 2.5, () -> c.saturation, v -> c.saturation = (float) v,
                "Colour intensity. Zero is black and white.");
        if (!simple()) {
            slider("Gamma", 0.3, 3.0, () -> c.gamma, v -> c.gamma = (float) v,
                    "Bends the midtones without moving black or white.");
        }

        header("Clean Up");
        slider("Dithering", 0.0, 3.0, () -> c.dither, v -> c.dither = (float) v,
                "Eight bit output bands a smooth sky into visible steps. Around 1.0 scatters that away.");
        toggle("Smooth hard edges", () -> c.antiAlias, v -> c.antiAlias = v,
                "Widens gradient stops and body rims to a pixel so they stop stair-stepping.");
        row += 6;
        slider("Zoom sharpening", 0.0, 3.0, () -> c.zoomDetail, v -> c.zoomDetail = (float) v,
                "Adds procedural detail as the view narrows. Without it, zooming in just magnifies "
                        + "the same noise and the sky goes soft and blocky.");

        header("Performance");
        slider("Detail level", 1, 5, () -> c.quality, v -> c.quality = (int) v,
                "Scales noise octaves and aurora height samples.").integer();
        toggle("Drop detail when frames are slow", () -> c.autoQuality, v -> c.autoQuality = v,
                "Walks the detail level down below 45 fps and back up above 75.");
        row += 6;
        note("Aurorae and nebulae are the expensive layers. Anything switched off costs nothing.");
    }

    // ----------------------------------------------------------- animation

    private void buildAnimation() {
        AnimationSettings a = cfg().animation;
        header("Time");
        dropdown("Time source", AnimationSettings.TIME_MODE_NAMES, () -> a.timeMode, v -> a.timeMode = v,
                "Local Clock drives the sky from your computer's own wall clock. World Time "
                        + "follows the Minecraft day. Frozen holds one time of day.");
        slider("Clock offset (hours)", -12.0, 12.0, () -> a.realTimeOffsetHours,
                v -> a.realTimeOffsetHours = (float) v,
                "Shifts the local clock, for running the sky ahead or behind.").decimals(1);
        if (!simple()) {
            slider("Clock speed", 0.0, 12.0, () -> a.realTimeScale, v -> a.realTimeScale = (float) v,
                    "1 is a true 24 hour day. 4 runs four sky days per real day.").decimals(2);
        }
        slider("Frozen time of day", 0.0, 1.0, () -> a.fixedTime, v -> a.fixedTime = (float) v,
                "Used by the Frozen source. 0 is noon, 0.25 sunset, 0.5 midnight.");

        header("Motion");
        slider("Animation speed", 0.0, 6.0, () -> a.speed, v -> a.speed = (float) v,
                "Drives clouds, aurorae, orbits and star rotation. Zero stops everything.");
        if (!simple()) {
            toggle("Pause animation in menus", () -> a.pauseInMenus, v -> a.pauseInMenus = v,
                    "Freezes the sky while a screen is open.");
        }

        header("Day & Night");
        toggle("Fade stars with daylight", () -> a.dayNightBlend, v -> a.dayNightBlend = v,
                "Hides stars and nebulae once the sun is up.");
        row += 6;
        slider("Night brightness boost", 0.0, 3.0, () -> a.nightBoost, v -> a.nightBoost = (float) v,
                "Multiplies star brightness at night.");
        if (!simple()) {
            slider("Star fade start", 0.0, 1.0, () -> a.starFadeStart, v -> a.starFadeStart = (float) v,
                    "Sun height where stars begin to fade.");
            slider("Star fade end", 0.0, 1.0, () -> a.starFadeEnd, v -> a.starFadeEnd = (float) v,
                    "Sun height where they are fully gone.");

            header("Sun Path");
            toggle("Sun follows the time source", () -> a.sunFollowsWorld, v -> a.sunFollowsWorld = v,
                    "Off lets the sun run on the animation clock instead.");
            row += 6;
            slider("Sun azimuth offset", -3.1416, 3.1416, () -> a.sunAzimuthOffset,
                    v -> a.sunAzimuthOffset = (float) v, "Rotates where the sun rises.");
            slider("Sun path tilt", -1.5, 1.5, () -> a.sunTilt, v -> a.sunTilt = (float) v,
                    "Leans the arc the sun travels.");

            header("Weather Response");
            toggle("React to rain and storms", () -> a.weatherReactive, v -> a.weatherReactive = v,
                    "Dims the sky and hides stars while it rains.");
            row += 6;
            slider("Rain dimming", 0.0, 1.0, () -> a.rainDim, v -> a.rainDim = (float) v,
                    "How much rain dims the sky.");
        }
    }

    // --------------------------------------------------------------- world

    private void buildGuide() {
        header("Making a sky, start to finish");
        note("1. Presets. Pick the one closest to what you want. Everything below is you "
                + "nudging it, not starting from nothing.");
        note("2. Gradients are the sky itself. The first one is the base colour, the ones after "
                + "it paint over the top. Horizon and zenith colours are set per gradient.");
        note("3. Stars, Cosmos, Bodies and Aurora only show at night. Drag the time slider under "
                + "the preview to the middle before you touch them, or you are editing something "
                + "you cannot see.");
        note("4. Layers decides what is drawn over what. If something is hiding behind something "
                + "else, that is the tab that fixes it.");
        note("5. Profiles saves it under a name. Presets overwrite the current profile, so save "
                + "before you go preset hunting.");

        header("The preview");
        note("Drag to look around. Scroll to zoom. Double-click to put the camera back.");
        note("The time chip on the left switches between following the world clock and scrubbing "
                + "the slider yourself.");

        header("Using a sky pack");
        note("SkyJet reads OptiFine custom skies and FabricSkyBoxes packs. Drop the pack in your "
                + "resource packs and turn it on, then open Skybox and hit Scan resource packs.");
        note("Pack layers are ordinary layers once imported. Tint them, rotate them, stack your "
                + "own stars over the top.");

        header("If it looks wrong");
        note("Nothing visible at all: check the toggle in the title bar, and check World in case "
                + "a dimension is switched off.");
        note("Vanilla sun or moon still showing: World, then Take over the sky completely.");
        note("Running slow: Animation, then drop Quality. Star and cloud counts cost the most.");

        header("Shortcuts");
        note("Z opens this screen. /skyjet does the same. Both are rebindable in Controls.");
    }

    private void buildInterface() {
        header("Layout");
        dropdown("Settings shown", new String[]{"Simple", "Everything"},
                () -> ConfigManager.simpleMode() ? 0 : 1,
                v -> {
                    ConfigManager.setGuiMode(v == 0 ? ConfigManager.MODE_SIMPLE : ConfigManager.MODE_ADVANCED);
                    ConfigManager.save();
                    init();
                },
                "Simple keeps the tabs most people use. Everything shows all of them.");
        note("Simple hides Gradients, Stars, Cosmos, Aurora, Bodies, Clouds, Atmosphere and "
                + "World. Nothing is lost, and switching back brings them straight up.");

        header("Scale");
        note("This is Minecraft's own GUI scale. Smaller fits more on screen, which is usually "
                + "what you want in here.");
        int[] scales = {0, 1, 2, 3, 4};
        String[] names = {"Auto", "1x (smallest)", "2x", "3x", "4x (largest)"};
        dropdown("GUI scale", names, SkyJetScreen::currentGuiScaleIndex,
                v -> applyGuiScale(scales[Math.max(0, Math.min(scales.length - 1, v))]),
                "Applies to the whole game, the same as the slider in Video Settings.");

        note("Reset and start-from-scratch live on the Presets tab, next to the skies they undo.");
    }

    private static int currentGuiScaleIndex() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return 0;
        int value = mc.options.guiScale().get();
        return Math.max(0, Math.min(4, value));
    }

    private void applyGuiScale(int value) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.options.guiScale().set(value);
        mc.options.save();
        //? if >=26 {
        mc.resizeGui();
        //?} else {
        /*mc.resizeDisplay();
        *///?}
        init();
    }

    private void buildWorld() {
        WorldSettings w = cfg().world;
        header("Vanilla Sky Override");
        toggle("Take over the sky completely", () -> w.takeOverSky, v -> w.takeOverSky = v,
                "Forces every vanilla and resource pack sky element off. Turn it off to control "
                        + "them one at a time, which is what you want when layering SkyJet over a "
                        + "sky pack's own sun and moon.");
        row += 6;

        boolean forced = w.takeOverSky;
        toggle(forced ? "Replace base sky (forced)" : "Replace base sky",
                w::replaceBaseSky, v -> w.overrideResourcePackSkies = v,
                "Draw SkyJet instead of the vanilla sky disc rather than over it.");
        toggle(forced ? "Hide vanilla clouds (forced)" : "Hide vanilla clouds",
                w::hideClouds, v -> w.disableVanillaClouds = v, "Hide the blocky vanilla cloud layer.");
        toggle(forced ? "Hide vanilla sun (forced)" : "Hide vanilla sun",
                w::hideSun, v -> w.disableVanillaSun = v, "Hide the vanilla sun sprite.");
        toggle(forced ? "Hide vanilla moon (forced)" : "Hide vanilla moon",
                w::hideMoon, v -> w.disableVanillaMoon = v, "Hide the vanilla moon sprite.");
        toggle(forced ? "Hide vanilla stars (forced)" : "Hide vanilla stars",
                w::hideStars, v -> w.disableVanillaStars = v, "Hide the vanilla star field.");
        toggle(forced ? "Hide sunrise glow (forced)" : "Hide sunrise/sunset glow",
                w::hideSunriseGlow, v -> w.disableSunriseGlow = v, "Hide the vanilla horizon glow.");
        toggle(forced ? "Hide void plane (forced)" : "Hide void plane",
                w::hideDarkDisc, v -> w.disableDarkDisc = v,
                "Hide the black disc vanilla paints under you, which is what stopped the sky "
                        + "continuing below the horizon.");

        header("Weather");
        toggle("Disable rain", () -> w.disableRain, v -> w.disableRain = v, "Stop rain drawing.");
        toggle("Disable snow", () -> w.disableSnow, v -> w.disableSnow = v, "Stop snow drawing.");
        toggle("Disable weather particles", () -> w.disableWeatherParticles,
                v -> w.disableWeatherParticles = v, "Stop rain splash particles.");
        toggle("Disable lightning flashes", () -> w.disableLightningFlash,
                v -> w.disableLightningFlash = v, "Stop the screen flashing white during storms.");

        header("Fog");
        toggle("Override fog colour", () -> w.overrideFogColor, v -> w.overrideFogColor = v,
                "Replace the world's fog colour with your own.");
        row += 6;
        color("Fog colour", () -> w.fogColor, v -> w.fogColor = v, false, "The replacement colour.");
        slider("Fog override strength", 0.0, 1.0, () -> w.fogColorBlend, v -> w.fogColorBlend = (float) v,
                "How far toward that colour to push the fog.");

        header("Dimensions");
        toggle("Overworld", () -> w.applyInOverworld, v -> w.applyInOverworld = v, "Use SkyJet in the Overworld.");
        toggle("The Nether", () -> w.applyInNether, v -> w.applyInNether = v, "Use SkyJet in the Nether.");
        toggle("The End", () -> w.applyInEnd, v -> w.applyInEnd = v, "Use SkyJet in the End.");
        toggle("Modded dimensions", () -> w.applyInCustom, v -> w.applyInCustom = v,
                "Use SkyJet in dimensions added by other mods.");
        note("Dimensions without a skybox (the Nether by default) have no sky pass to take over.");
    }

    // ------------------------------------------------------------ profiles

    private void buildProfiles() {
        header("Editor Layout");
        buttonRow(new String[]{"Simple", "All settings"},
                new Runnable[]{
                        () -> {
                            ConfigManager.setGuiMode(ConfigManager.MODE_SIMPLE);
                            ConfigManager.save();
                            init();
                            setStatus("Simple layout");
                        },
                        () -> {
                            ConfigManager.setGuiMode(ConfigManager.MODE_ADVANCED);
                            ConfigManager.save();
                            init();
                            setStatus("All settings shown");
                        }
                },
                new int[]{80, 110},
                new boolean[]{simple(), !simple()},
                new String[]{"Only the controls most people reach for.", "Every control SkyJet has."});

        header("Sky Actions");
        quickActions();

        header("Active Profile");
        profileNameField = content.add(new TextField(0, row, Math.min(colW, 220), 20));
        profileNameField.setValue(ConfigManager.activeProfileName());
        profileNameField.placeholder("Profile name");
        profileNameField.tooltip = "Rename this profile.";
        profileNameField.onCommit(v -> {
            if (!v.isBlank() && !v.equals(ConfigManager.activeProfileName())) {
                ConfigManager.renameProfile(ConfigManager.activeProfileName(), v);
                ConfigManager.save();
                rebuild();
                setStatus("Renamed profile");
            }
        });
        row += 28;

        buttonRow(new String[]{"New", "Duplicate", "Delete"},
                new Runnable[]{
                        () -> {
                            String created = ConfigManager.addProfile("New Sky", new SkyConfig());
                            ConfigManager.selectProfile(created);
                            ConfigManager.save();
                            resetLayerSelection();
                            rebuild();
                            setStatus("Created profile");
                        },
                        () -> {
                            String created = ConfigManager.addProfile(ConfigManager.activeProfileName() + " copy", cfg());
                            ConfigManager.selectProfile(created);
                            ConfigManager.save();
                            rebuild();
                            setStatus("Duplicated profile");
                        },
                        () -> {
                            ConfigManager.deleteProfile(ConfigManager.activeProfileName());
                            ConfigManager.save();
                            resetLayerSelection();
                            rebuild();
                            setStatus("Deleted profile");
                        }
                },
                new int[]{72, 82, 72}, new boolean[]{true, false, false},
                new String[]{"Start a new empty profile.", "Copy this profile.", "Delete this profile."});

        header("Saved Profiles");
        for (String name : ConfigManager.profileNames()) {
            final String profile = name;
            boolean active = name.equals(ConfigManager.activeProfileName());
            ProfileRow prow = new ProfileRow(0, row, colW, profile, active, () -> {
                ConfigManager.selectProfile(profile);
                ConfigManager.save();
                resetLayerSelection();
                rebuild();
                setStatus("Switched to " + profile);
            });
            prow.tooltip = "Switch to " + profile + ".";
            content.add(prow);
            row += 24;
        }

        header("Share");
        buttonRow(new String[]{"Copy to clipboard", "Paste from clipboard"},
                new Runnable[]{
                        () -> {
                            Minecraft.getInstance().keyboardHandler.setClipboard(ConfigManager.exportActive());
                            setStatus("Sky copied to clipboard");
                        },
                        () -> {
                            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                            SkyConfig imported;
                            try {
                                imported = ConfigManager.importJson(clip);
                            } catch (Exception ignored) {
                                imported = null;
                            }
                            if (imported != null) {
                                ConfigManager.replaceActive(imported);
                                ConfigManager.save();
                                resetLayerSelection();
                                rebuild();
                                setStatus("Imported sky");
                            } else {
                                setStatus("Clipboard did not contain a SkyJet sky");
                            }
                        }
                },
                new int[]{130, 140}, new boolean[]{false, false},
                new String[]{"Copy this sky as JSON.", "Replace this sky with JSON from the clipboard."});

        button("Restore built-in presets", 170, () -> {
            ConfigManager.restoreBuiltInPresets();
            ConfigManager.save();
            rebuild();
            setStatus("Built-in presets restored");
        }, false, "Adds any built-in preset that is missing from your profile list.");

        header("Credits");
        note("SkyJet by Wayysss and HitsOnly.");
    }

    private final class ProfileRow extends Widget {
        private final String name;
        private final boolean active;
        private final Runnable select;

        ProfileRow(int x, int y, int width, String name, boolean active, Runnable select) {
            super(x, y, width, 20);
            this.name = name;
            this.active = active;
            this.select = select;
        }

        @Override
        public void render(Gfx g, int mouseX, int mouseY, float partial) {
            boolean hover = isOver(mouseX, mouseY);
            g.rounded(x, y, width, height, active ? Theme.PANEL_ACTIVE : (hover ? Theme.PANEL_HOVER : Theme.PANEL));
            if (active) g.fill(x, y + 2, 2, height - 4, Theme.ACCENT);
            g.text(g.trim(name, width - 60), x + 10, y + 6, active ? Theme.ACCENT : Theme.TEXT);
            if (active) g.textRight("ACTIVE", x + width - 8, y + 6, Theme.TEXT_FAINT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isOver(mouseX, mouseY) && !active) {
                select.run();
                return true;
            }
            return false;
        }
    }

    // -------------------------------------------------------------- render

    //? if >=26 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        draw(new Gfx(graphics), mouseX, mouseY, partial);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        draw(new Gfx(graphics), mouseX, mouseY, partial);
    }
    *///?}

    private void draw(Gfx g, int mouseX, int mouseY, float partial) {
        long now = System.nanoTime();
        float delta = (float) ((now - lastFrameNanos) / 1.0e9);
        lastFrameNanos = now;
        if (delta < 0.0f || delta > 0.5f) delta = 0.0f;
        previewTime += delta * Math.max(0.0f, cfg().animation.speed);
        content.tick();

        SkyConfig config = cfg();
        float day = previewTimeValue();

        // The preview is a full sky render. Drawing it at native size and capping the rate is
        // what stops dragging a colour from stuttering: the picker updates every frame while the
        // expensive pass runs at a fixed cadence.
        if (preview.ensure(previewW, previewH)) {
            if (now - lastPreviewNanos > 16_000_000L) {
                lastPreviewNanos = now;
                preview.render(config, previewYaw, previewPitch, previewFov, previewTime, day);
            }
        }

        g.fill(0, 0, this.width, this.height, Theme.BACKDROP);
        g.rounded(winX, winY, winW, winH, Theme.WINDOW);
        g.outline(winX, winY, winW, winH, Theme.BORDER);

        drawHeader(g, mouseX, mouseY);
        drawSidebar(g, mouseX, mouseY);

        g.fill(previewX - 1, previewY - 1, previewW + 2, previewH + 2, Theme.BORDER);
        if (preview.isReady()) {
            preview.draw(g, previewX, previewY, previewW, previewH);
        } else {
            g.fill(previewX, previewY, previewW, previewH, Theme.WINDOW_ALT);
            g.textCentered("Preview unavailable", previewX + previewW / 2, previewY + previewH / 2 - 4, Theme.TEXT_DIM);
        }
        drawPreviewOverlay(g, mouseX, mouseY);
        drawPreviewControls(g, mouseX, mouseY);

        content.render(g, mouseX, mouseY, partial);
        content.renderOverlays(g, mouseX, mouseY, partial, this.width, this.height);

        if (System.currentTimeMillis() < statusUntil && !status.isEmpty()) {
            int w = g.width(status) + 20;
            int sx = winX + winW / 2 - w / 2;
            int sy = winY + winH - 30;
            g.rounded(sx, sy, w, 20, Theme.PANEL_ACTIVE);
            g.outline(sx, sy, w, 20, Theme.ACCENT);
            g.textCentered(status, sx + w / 2, sy + 6, Theme.TEXT);
        }

        if (choosingMode) {
            drawModeChooser(g, mouseX, mouseY);
            return;
        }

        if (!content.hasOpenOverlay()) {
            String tip = content.tooltipAt(mouseX, mouseY);
            if (tip == null) tip = sidebarTooltip(mouseX, mouseY);
            g.tooltip(tip, mouseX, mouseY, this.width, this.height);
        }
    }

    // ---------------------------------------------------------- mode chooser

    private int[] modeButtons() {
        int boxW = Math.min(420, winW - 60);
        int boxH = 150;
        int bx = winX + (winW - boxW) / 2;
        int by = winY + (winH - boxH) / 2;
        int bw = (boxW - 48) / 2;
        return new int[]{bx, by, boxW, boxH, bx + 16, by + 84, bw, bx + 32 + bw, by + 84, bw};
    }

    private void drawModeChooser(Gfx g, int mouseX, int mouseY) {
        int[] l = modeButtons();
        g.fill(0, 0, this.width, this.height, 0xE005070C);
        g.rounded(l[0], l[1], l[2], l[3], Theme.WINDOW);
        g.outline(l[0], l[1], l[2], l[3], Theme.ACCENT);
        g.textShadow("How much do you want to see?", l[0] + 16, l[1] + 16, Theme.TEXT);
        g.text("SkyJet has a lot of controls. Pick a layout; you can change it any time", l[0] + 16, l[1] + 36, Theme.TEXT_DIM);
        g.text("from the Profiles tab.", l[0] + 16, l[1] + 48, Theme.TEXT_DIM);

        boolean hoverSimple = mouseX >= l[4] && mouseX < l[4] + l[6] && mouseY >= l[5] && mouseY < l[5] + 26;
        g.rounded(l[4], l[5], l[6], 26, hoverSimple ? Theme.lerp(Theme.ACCENT, 0xFFFFFFFF, 0.15f) : Theme.ACCENT);
        g.textCentered("Simple", l[4] + l[6] / 2, l[5] + 9, 0xFF05070C);

        boolean hoverAll = mouseX >= l[7] && mouseX < l[7] + l[9] && mouseY >= l[8] && mouseY < l[8] + 26;
        g.rounded(l[7], l[8], l[9], 26, hoverAll ? Theme.PANEL_HOVER : Theme.PANEL);
        g.outline(l[7], l[8], l[9], 26, Theme.BORDER);
        g.textCentered("All settings", l[7] + l[9] / 2, l[8] + 9, Theme.TEXT);

        g.text("presets, skyboxes and the basics", l[4], l[5] + 32, Theme.TEXT_FAINT);
        g.text("every layer and every knob", l[7], l[8] + 32, Theme.TEXT_FAINT);
    }

    private boolean handleModeChooser(double mouseX, double mouseY) {
        int[] l = modeButtons();
        if (mouseY >= l[5] && mouseY < l[5] + 26) {
            if (mouseX >= l[4] && mouseX < l[4] + l[6]) {
                ConfigManager.setGuiMode(ConfigManager.MODE_SIMPLE);
                ConfigManager.save();
                choosingMode = false;
                init();
                return true;
            }
            if (mouseX >= l[7] && mouseX < l[7] + l[9]) {
                ConfigManager.setGuiMode(ConfigManager.MODE_ADVANCED);
                ConfigManager.save();
                choosingMode = false;
                init();
                return true;
            }
        }
        return true;
    }

    private void drawHeader(Gfx g, int mouseX, int mouseY) {
        g.gradient(winX + 1, winY + 1, winW - 2, HEADER_H - 1, Theme.WINDOW_ALT, Theme.WINDOW);
        g.fill(winX, winY + HEADER_H, winW, 1, Theme.BORDER);

        g.fill(winX + 14, winY + 11, 3, 14, Theme.ACCENT);
        g.fill(winX + 19, winY + 14, 3, 11, Theme.ACCENT_ALT);
        g.textShadow("SkyJet", winX + 28, winY + 10, Theme.TEXT);
        g.text("by Wayysss & HitsOnly", winX + 28, winY + 21, Theme.TEXT_FAINT);

        String profile = ConfigManager.activeProfileName();
        int profileW = g.width(profile) + 16;
        int profileX = winX + winW - 150 - profileW;
        g.rounded(profileX, winY + 9, profileW, 18, Theme.PANEL);
        g.outline(profileX, winY + 9, profileW, 18, Theme.BORDER_SOFT);
        g.textCentered(profile, profileX + profileW / 2, winY + 14, Theme.TEXT_DIM);

        boolean on = ConfigManager.modEnabled();
        int toggleX = winX + winW - 136;
        boolean toggleHover = mouseX >= toggleX && mouseX < toggleX + 76 && mouseY >= winY + 9 && mouseY < winY + 27;
        g.rounded(toggleX, winY + 9, 76, 18, on ? Theme.withAlpha(Theme.SUCCESS, 0.20f) : Theme.PANEL);
        g.outline(toggleX, winY + 9, 76, 18, toggleHover ? Theme.ACCENT : Theme.BORDER_SOFT);
        g.rounded(toggleX + 8, winY + 15, 6, 6, on ? Theme.SUCCESS : Theme.TEXT_FAINT);
        g.text(on ? "ENABLED" : "DISABLED", toggleX + 20, winY + 14, on ? Theme.SUCCESS : Theme.TEXT_DIM);

        int closeX = winX + winW - 34;
        boolean closeHover = mouseX >= closeX && mouseX < closeX + 22 && mouseY >= winY + 8 && mouseY < winY + 28;
        g.rounded(closeX, winY + 8, 22, 20, closeHover ? Theme.withAlpha(Theme.DANGER, 0.25f) : Theme.PANEL);
        g.textCentered("✕", closeX + 11, winY + 14, closeHover ? Theme.DANGER : Theme.TEXT_DIM);
    }

    private void drawSidebar(Gfx g, int mouseX, int mouseY) {
        g.fill(winX + 1, winY + HEADER_H + 1, SIDEBAR_W - 1, winH - HEADER_H - 2, Theme.SIDEBAR);
        g.fill(winX + SIDEBAR_W, winY + HEADER_H, 1, winH - HEADER_H, Theme.BORDER);

        int rowH = tabSpacing - 2;
        int ty = winY + HEADER_H + 8;
        String group = null;
        boolean headings = showGroupHeadings();
        for (Tab tab : visibleTabs()) {
            if (headings && !tab.group.equals(group)) {
                group = tab.group;
                g.text(group.toUpperCase(java.util.Locale.ROOT), winX + 12, ty + 3, Theme.TEXT_FAINT);
                ty += GROUP_H;
            }
            boolean hover = mouseX >= winX + 6 && mouseX < winX + SIDEBAR_W - 6
                    && mouseY >= ty && mouseY < ty + rowH;
            boolean selected = tab == activeTab;
            if (selected) {
                g.rounded(winX + 6, ty, SIDEBAR_W - 12, rowH, Theme.PANEL_ACTIVE);
                g.fill(winX + 6, ty + 4, 2, Math.max(4, rowH - 8), Theme.ACCENT);
            } else if (hover) {
                g.rounded(winX + 6, ty, SIDEBAR_W - 12, rowH, Theme.PANEL);
            }
            g.text(tab.title, winX + 16, ty + (rowH - 8) / 2,
                    selected || hover ? Theme.TEXT : Theme.TEXT_DIM);
            ty += tabSpacing;
        }
    }

    /** Headings cost a row each, so they are dropped when the sidebar is too short to hold them. */
    private boolean showGroupHeadings() {
        int groups = 0;
        String seen = null;
        for (Tab t : visibleTabs()) {
            if (!t.group.equals(seen)) {
                seen = t.group;
                groups++;
            }
        }
        int needed = visibleTabs().size() * tabSpacing + groups * GROUP_H;
        return needed <= winH - HEADER_H - 14;
    }

    private String sidebarTooltip(int mouseX, int mouseY) {
        if (mouseX < winX + 6 || mouseX >= winX + SIDEBAR_W - 6) return null;
        int rowH = tabSpacing - 2;
        int ty = winY + HEADER_H + 8;
        String group = null;
        boolean headings = showGroupHeadings();
        for (Tab tab : visibleTabs()) {
            if (headings && !tab.group.equals(group)) {
                group = tab.group;
                ty += GROUP_H;
            }
            if (mouseY >= ty && mouseY < ty + rowH) return tabTooltip(tab);
            ty += tabSpacing;
        }
        return null;
    }

    private static String tabTooltip(Tab tab) {
        return switch (tab) {
            case PRESETS -> "Ready-made skies, plus reset and start from scratch.";
            case SKYBOX -> "Wrap sky pack art around the world. Import installed packs here.";
            case LAYERS -> "Reorder, tint and fade whole groups of layers at once.";
            case GRADIENTS -> "The coloured ramps the sky is built from.";
            case STARS -> "Star fields: shape, pattern, density and motion.";
            case COSMOS -> "Nebulae and galaxies.";
            case AURORA -> "Aurora curtains, including how deep they sit in the world.";
            case BODIES -> "Planets, moons, orbs and black holes.";
            case CLOUDS -> "Volumetric cloud decks.";
            case ATMOSPHERE -> "Scattering, sun and moon discs, the horizon and what is below it.";
            case IMAGE -> "Brightness, clean-up and performance.";
            case ANIMATION -> "What clock the sky runs on and how fast it moves.";
            case WORLD -> "What SkyJet hides of the vanilla sky, and where it applies.";
            case PROFILES -> "Saved skies, and sharing them.";
            case GUIDE -> "How to build a sky, and what to check when one looks wrong.";
            case INTERFACE -> "Editor layout, GUI scale, and resetting the current sky.";
        };
    }

    private void drawPreviewOverlay(Gfx g, int mouseX, int mouseY) {
        boolean hover = inPreview(mouseX, mouseY);
        if (placingBody) {
            g.outline(previewX, previewY, previewW, previewH, Theme.ACCENT);
            g.text("click to place the selected body", previewX + 8, previewY + previewH - 14, Theme.ACCENT);
        } else if (hover && !draggingPreview) {
            g.text("drag to look · scroll to zoom", previewX + 8, previewY + previewH - 14,
                    Theme.withAlpha(Theme.TEXT, 0.7f));
        }
        String info = String.format("%.0f° fov  ·  yaw %.0f  ·  pitch %.0f", previewFov, previewYaw, previewPitch);
        g.textRight(info, previewX + previewW - 8, previewY + 8, Theme.withAlpha(Theme.TEXT, 0.55f));
    }

    private int controlsY() {
        return previewY + previewH + 8;
    }

    /**
     * Lays the preview control strip out: a source chip, then the time slider takes the rest.
     *
     * <p>The play/pause and reset-view buttons that used to sit here are gone. Nobody needs to
     * pause a preview, and resetting the view is a double-click on the preview itself.
     */
    private int[] controlLayout() {
        int right = previewX + previewW;
        int sourceW = 96;
        int sourceX = previewX;
        int sliderX = sourceX + sourceW + 10;
        int sliderW = right - sliderX - 34;
        if (sliderW < 40) {
            sourceW = 0;
            sourceX = -1000;
            sliderX = previewX;
            sliderW = right - sliderX - 34;
        }
        if (sliderW < 24) {
            sliderX = -1000;
            sliderW = 0;
        }
        return new int[]{sourceX, sourceW, sliderX, sliderW};
    }

    private void drawPreviewControls(Gfx g, int mouseX, int mouseY) {
        int cy = controlsY();
        int[] l = controlLayout();
        boolean inRow = mouseY >= cy && mouseY < cy + 18;

        if (l[1] > 0) {
            boolean hover = inRow && mouseX >= l[0] && mouseX < l[0] + l[1];
            g.rounded(l[0], cy, l[1], 18, previewFollowConfig ? Theme.withAlpha(Theme.ACCENT, 0.18f)
                    : (hover ? Theme.PANEL_HOVER : Theme.PANEL));
            g.textCentered(previewFollowConfig ? "live time" : "scrubbing", l[0] + l[1] / 2, cy + 5,
                    previewFollowConfig ? Theme.ACCENT : Theme.TEXT_DIM);
        }

        if (l[3] > 0) {
            int trackY = cy + 8;
            g.rounded(l[2], trackY, l[3], 3, Theme.TRACK);
            float t = previewTimeValue();
            int fill = (int) (l[3] * t);
            g.rounded(l[2], trackY, fill, 3, Theme.TRACK_FILL);
            g.rounded(l[2] + Math.max(0, Math.min(l[3] - 6, fill - 3)), trackY - 3, 6, 9, Theme.TEXT);
            g.textRight(String.format("%02d:%02d", (int) ((t * 24.0f + 12.0f) % 24.0f),
                            (int) ((t * 1440.0f) % 60.0f)),
                    previewX + previewW, cy + 5, Theme.TEXT_FAINT);
        }
    }

    private float previewTimeValue() {
        if (!previewFollowConfig) return previewDay;
        AnimationSettings anim = cfg().animation;
        if (anim.timeMode == AnimationSettings.TIME_REALTIME) {
            return SkyState.localClockDayFraction(anim.realTimeOffsetHours, anim.realTimeScale);
        }
        if (anim.timeMode == AnimationSettings.TIME_FIXED) {
            return anim.fixedTime;
        }
        return SkyState.vanillaStateValid ? SkyState.vanillaSunAngle : previewDay;
    }

    // -------------------------------------------------- preview ray casting

    /**
     * World direction under a point in the preview, matching the projection the preview pass
     * builds. Used to drop a celestial body exactly where the mouse is.
     */
    private void placeBodyAt(double mouseX, double mouseY) {
        SkyConfig c = cfg();
        if (c.bodies.isEmpty() || previewW <= 0 || previewH <= 0) return;
        BodyLayer b = c.bodies.get(Math.max(0, Math.min(bodyIndex, c.bodies.size() - 1)));

        float aspect = (float) previewW / (float) previewH;
        float horizontalFov = (float) Math.toRadians(previewFov);
        float verticalFov = (float) (2.0 * Math.atan(Math.tan(horizontalFov * 0.5) / Math.max(0.2f, aspect)));

        double nx = 2.0 * (mouseX - previewX) / previewW - 1.0;
        double ny = 2.0 * (mouseY - previewY) / previewH - 1.0;

        // The preview projection is flipped vertically so the GUI blitter can sample it top-left
        // first, so screen-down is +y in clip space and the camera y is negated back here.
        float camX = (float) (nx * Math.tan(horizontalFov * 0.5));
        float camY = (float) (-ny * Math.tan(verticalFov * 0.5));
        float camZ = -1.0f;

        Matrix4f view = new Matrix4f()
                .rotateX((float) Math.toRadians(previewPitch))
                .rotateY((float) Math.toRadians(previewYaw + 180.0f));
        Vector4f dir = view.invert().transform(new Vector4f(camX, camY, camZ, 0.0f));
        float len = (float) Math.sqrt(dir.x * dir.x + dir.y * dir.y + dir.z * dir.z);
        if (len < 1.0e-5f) return;

        b.elevation = (float) Math.asin(Math.max(-1.0, Math.min(1.0, dir.y / len)));
        b.azimuth = (float) Math.atan2(dir.z, dir.x);
    }

    // -------------------------------------------------------------- events

    private boolean handlePreviewControls(double mouseX, double mouseY) {
        int cy = controlsY();
        if (mouseY < cy - 4 || mouseY >= cy + 20) return false;
        int[] l = controlLayout();
        if (l[1] > 0 && mouseX >= l[0] && mouseX < l[0] + l[1]) {
            previewFollowConfig = !previewFollowConfig;
            return true;
        }
        if (l[3] > 0 && mouseX >= l[2] - 3 && mouseX <= l[2] + l[3] + 3) {
            previewFollowConfig = false;
            previewDay = (float) Math.max(0.0, Math.min(1.0, (mouseX - l[2]) / (double) l[3]));
            return true;
        }
        return false;
    }

    private void resetPreviewCamera() {
        previewYaw = 30.0f;
        previewPitch = -12.0f;
        previewFov = 100.0f;
    }

    private boolean handleSidebar(double mouseX, double mouseY) {
        if (mouseX < winX + 6 || mouseX >= winX + SIDEBAR_W - 6) return false;
        int rowH = tabSpacing - 2;
        int ty = winY + HEADER_H + 8;
        String group = null;
        boolean headings = showGroupHeadings();
        for (Tab tab : visibleTabs()) {
            if (headings && !tab.group.equals(group)) {
                group = tab.group;
                ty += GROUP_H;
            }
            if (mouseY >= ty && mouseY < ty + rowH) {
                if (activeTab != tab) {
                    activeTab = tab;
                    if (tab != Tab.BODIES) placingBody = false;
                    ensureNightPreview(tab);
                    rebuildKeepsScroll = false;
                    rebuild();
                }
                return true;
            }
            ty += tabSpacing;
        }
        return false;
    }

    private boolean handleHeader(double mouseX, double mouseY) {
        if (mouseY < winY + 8 || mouseY >= winY + 28) return false;
        int closeX = winX + winW - 34;
        if (mouseX >= closeX && mouseX < closeX + 22) {
            onClose();
            return true;
        }
        int toggleX = winX + winW - 136;
        if (mouseX >= toggleX && mouseX < toggleX + 76) {
            ConfigManager.setModEnabled(!ConfigManager.modEnabled());
            ConfigManager.save();
            return true;
        }
        return false;
    }

    private boolean inPreview(double mouseX, double mouseY) {
        return mouseX >= previewX && mouseX < previewX + previewW
                && mouseY >= previewY && mouseY < previewY + previewH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        if (choosingMode) {
            return handleModeChooser(mx, my);
        }
        if (content.hasOpenOverlay()) {
            // An open dropdown or colour picker swallows the click, even when it just closes it.
            content.mouseClicked(mx, my, button);
            return true;
        }
        if (handleHeader(mx, my)) return true;
        if (handleSidebar(mx, my)) return true;
        if (handlePreviewControls(mx, my)) return true;

        if (inPreview(mx, my)) {
            if (placingBody && button == 0 && activeTab == Tab.BODIES) {
                placeBodyAt(mx, my);
                return true;
            }
            if (doubleClick) {
                resetPreviewCamera();
                return true;
            }
            draggingPreview = true;
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        if (content.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (choosingMode) return true;
        double mx = event.x();
        double my = event.y();
        if (placingBody && activeTab == Tab.BODIES && inPreview(mx, my) && !draggingPreview) {
            placeBodyAt(mx, my);
            return true;
        }
        if (draggingPreview) {
            previewYaw = (float) ((previewYaw + (mx - lastMouseX) * 0.35) % 360.0);
            previewPitch = (float) Math.max(-89.0, Math.min(89.0, previewPitch + (my - lastMouseY) * 0.30));
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }
        if (handlePreviewControls(mx, my)) return true;
        if (content.mouseDragged(mx, my, event.button())) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingPreview = false;
        content.mouseReleased(event.x(), event.y(), event.button());
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (choosingMode) return true;
        if (!content.hasOpenOverlay() && inPreview(mouseX, mouseY)) {
            previewFov = (float) Math.max(30.0, Math.min(150.0, previewFov - scrollY * 5.0));
            return true;
        }
        if (content.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (choosingMode) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                ConfigManager.setGuiMode(ConfigManager.MODE_SIMPLE);
                ConfigManager.save();
                choosingMode = false;
                init();
            }
            return true;
        }
        if (content.keyPressed(event.key(), event.modifiers())) return true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (placingBody) {
                placingBody = false;
                rebuild();
                return true;
            }
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (choosingMode) return true;
        if (content.charTyped(event.codepoint())) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        preview.close();
        super.onClose();
    }

    @Override
    public void removed() {
        preview.close();
        super.removed();
    }
}
