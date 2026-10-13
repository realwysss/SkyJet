package dev.skyjet.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.skyjet.gui.SkyJetScreen;

/**
 * Opens the SkyJet editor from Mod Menu's mod list.
 *
 * <p>Declared as the {@code modmenu} entrypoint in {@code fabric.mod.json}. Mod Menu is a compile
 * only dependency, so this class is never loaded when Mod Menu is not installed and the jar
 * carries no hard dependency on it.
 */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // SkyJet's screen does not take a parent, so the caller's screen is simply dropped; the
        // editor saves and returns to the game on close.
        return parent -> new SkyJetScreen();
    }
}
