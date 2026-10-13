package dev.skyjet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.skyjet.SkyJet;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Path root;
    private static Path file;
    private static Store store = new Store();
    private static SkyConfig active;
    private static int revision;

    private ConfigManager() {
    }

    public static final int MODE_SIMPLE = 0;
    public static final int MODE_ADVANCED = 1;

    public static final class Store {
        public boolean modEnabled = true;
        public String activeProfile = "SkyJet Default";
        /** Simple hides everything but the controls most people actually reach for. */
        public int guiMode = MODE_SIMPLE;
        /** Cleared once the player has picked a mode, so the chooser only appears once. */
        public boolean modeChosen = false;
        public Map<String, SkyConfig> profiles = new LinkedHashMap<>();
    }

    public static int guiMode() {
        return store.guiMode == MODE_ADVANCED ? MODE_ADVANCED : MODE_SIMPLE;
    }

    public static boolean simpleMode() {
        return guiMode() == MODE_SIMPLE;
    }

    public static void setGuiMode(int mode) {
        store.guiMode = mode;
        store.modeChosen = true;
    }

    public static boolean modeChosen() {
        return store.modeChosen;
    }

    public static void init() {
        root = FabricLoader.getInstance().getConfigDir().resolve(SkyJet.MOD_ID);
        file = root.resolve("skyjet.json");
        load();
    }

    public static synchronized void load() {
        Store loaded = null;
        try {
            if (file != null && Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                loaded = GSON.fromJson(json, Store.class);
            }
        } catch (Exception e) {
            SkyJet.LOG.error("Failed to read SkyJet config, falling back to defaults", e);
        }

        store = loaded != null ? loaded : new Store();
        if (store.profiles == null) store.profiles = new LinkedHashMap<>();

        if (store.profiles.isEmpty()) {
            for (Map.Entry<String, SkyConfig> e : Presets.builtIn().entrySet()) {
                store.profiles.put(e.getKey(), e.getValue().copy());
            }
            store.activeProfile = "SkyJet Default";
        }

        for (SkyConfig c : store.profiles.values()) {
            c.sanitize();
        }

        active = store.profiles.get(store.activeProfile);
        if (active == null) {
            active = store.profiles.values().iterator().next();
            store.activeProfile = active.name;
        }
        bumpRevision();
    }

    public static synchronized void save() {
        if (file == null) return;
        try {
            Files.createDirectories(root);
            Files.writeString(file, GSON.toJson(store), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SkyJet.LOG.error("Failed to save SkyJet config", e);
        }
    }

    public static SkyConfig active() {
        if (active == null) {
            active = new SkyConfig();
        }
        return active;
    }

    public static boolean modEnabled() {
        return store.modEnabled;
    }

    public static void setModEnabled(boolean value) {
        store.modEnabled = value;
        bumpRevision();
    }

    public static String activeProfileName() {
        return store.activeProfile;
    }

    public static List<String> profileNames() {
        return new ArrayList<>(store.profiles.keySet());
    }

    public static void selectProfile(String name) {
        SkyConfig c = store.profiles.get(name);
        if (c == null) return;
        store.activeProfile = name;
        active = c;
        bumpRevision();
    }

    public static String addProfile(String name, SkyConfig source) {
        String unique = uniqueName(name);
        SkyConfig copy = source.copy();
        copy.name = unique;
        store.profiles.put(unique, copy);
        return unique;
    }

    public static void deleteProfile(String name) {
        if (store.profiles.size() <= 1) return;
        store.profiles.remove(name);
        if (name.equals(store.activeProfile)) {
            String next = store.profiles.keySet().iterator().next();
            selectProfile(next);
        }
    }

    public static void renameProfile(String oldName, String newName) {
        SkyConfig c = store.profiles.remove(oldName);
        if (c == null) return;
        String unique = uniqueName(newName);
        c.name = unique;
        store.profiles.put(unique, c);
        if (oldName.equals(store.activeProfile)) {
            store.activeProfile = unique;
            active = c;
        }
    }

    public static void replaceActive(SkyConfig source) {
        String name = store.activeProfile;
        SkyConfig copy = source.copy();
        copy.name = name;
        copy.sanitize();
        store.profiles.put(name, copy);
        active = copy;
        bumpRevision();
    }

    public static void restoreBuiltInPresets() {
        for (Map.Entry<String, SkyConfig> e : Presets.builtIn().entrySet()) {
            if (!store.profiles.containsKey(e.getKey())) {
                store.profiles.put(e.getKey(), e.getValue().copy());
            }
        }
    }

    public static String exportActive() {
        return GSON.toJson(active());
    }

    public static SkyConfig importJson(String json) {
        SkyConfig c = GSON.fromJson(json, SkyConfig.class);
        if (c != null) c.sanitize();
        return c;
    }

    private static String uniqueName(String base) {
        String candidate = base == null || base.isBlank() ? "Sky" : base.trim();
        if (!store.profiles.containsKey(candidate)) return candidate;
        int i = 2;
        while (store.profiles.containsKey(candidate + " " + i)) i++;
        return candidate + " " + i;
    }

    public static int revision() {
        return revision;
    }

    public static void bumpRevision() {
        revision++;
    }
}
