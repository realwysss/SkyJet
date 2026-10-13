package dev.skyjet.render;

import dev.skyjet.config.ConfigManager;
import dev.skyjet.config.SkyConfig;
import dev.skyjet.config.WorldSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

/**
 * Central gate used by the mixins: decides whether SkyJet owns the sky in the current dimension.
 */
public final class SkyJetRuntime {
    private static boolean previewOverrideActive;
    private static SkyConfig previewOverride;

    private SkyJetRuntime() {
    }

    public static SkyConfig config() {
        if (previewOverrideActive && previewOverride != null) {
            return previewOverride;
        }
        return ConfigManager.active();
    }

    public static void setLivePreview(SkyConfig config) {
        previewOverride = config;
        previewOverrideActive = config != null;
    }

    /** True whenever the mod itself is switched on, regardless of dimension or sky settings. */
    public static boolean modActive() {
        return ConfigManager.modEnabled();
    }

    public static boolean enabled() {
        return ConfigManager.modEnabled() && config().enabled && !SkyJetRenderer.isFailed();
    }

    public static boolean shouldRender() {
        if (!enabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return false;
        WorldSettings w = config().world;
        var key = level.dimension();
        if (key == Level.OVERWORLD) return w.applyInOverworld;
        if (key == Level.NETHER) return w.applyInNether;
        if (key == Level.END) return w.applyInEnd;
        return w.applyInCustom;
    }

    public static WorldSettings world() {
        return config().world;
    }
}
