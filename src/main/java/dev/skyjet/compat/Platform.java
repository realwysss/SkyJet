package dev.skyjet.compat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.skyjet.SkyJet;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

/**
 * Bridges the Fabric API entry points that were renamed partway through the 26.x line.
 *
 * <p>SkyJet supports older loaders, which in practice means older Fabric API builds too. Two
 * helpers were renamed during that window: {@code KeyBindingHelper} became
 * {@code KeyMappingHelper}, and {@code ClientCommandManager} became {@code ClientCommands}.
 * Resolving them reflectively keeps one jar working across the whole API range for its Minecraft
 * version, and degrades to a warning instead of a crash if neither is present.
 *
 * <p>Minecraft-side differences stay on Stonecutter conditionals, because the Minecraft version
 * is fixed for a given jar while the Fabric API version is not.
 */
public final class Platform {
    private static final String[] KEY_HELPERS = {
            "net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper|registerKeyMapping",
            "net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper|registerKeyBinding"
    };

    private static final String[] COMMAND_FACTORIES = {
            "net.fabricmc.fabric.api.client.command.v2.ClientCommands|literal",
            "net.fabricmc.fabric.api.client.command.v2.ClientCommandManager|literal"
    };

    private static Method keyRegister;
    private static Method commandLiteral;
    private static boolean keyLookupDone;
    private static boolean commandLookupDone;

    private Platform() {
    }

    private static Method resolve(String[] candidates, Class<?>... parameterTypes) {
        for (String candidate : candidates) {
            int split = candidate.indexOf('|');
            try {
                return Class.forName(candidate.substring(0, split))
                        .getMethod(candidate.substring(split + 1), parameterTypes);
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                // Try the next naming generation.
            }
        }
        return null;
    }

    /**
     * Registers a key mapping with Fabric API. A failure costs the keybind, not the mod: the
     * {@code /skyjet} command still opens the editor.
     */
    public static KeyMapping registerKey(KeyMapping mapping) {
        if (!keyLookupDone) {
            keyLookupDone = true;
            keyRegister = resolve(KEY_HELPERS, KeyMapping.class);
            if (keyRegister == null) {
                SkyJet.LOG.warn("No Fabric API key mapping helper found; SkyJet keybinds are unavailable. Use /skyjet.");
            }
        }
        if (keyRegister == null) return mapping;
        try {
            Object result = keyRegister.invoke(null, mapping);
            return result instanceof KeyMapping registered ? registered : mapping;
        } catch (ReflectiveOperationException e) {
            SkyJet.LOG.warn("Failed to register the SkyJet keybind", e);
            return mapping;
        }
    }

    /** Returns null when no client command factory is available, so registration can be skipped. */
    @SuppressWarnings("unchecked")
    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        if (!commandLookupDone) {
            commandLookupDone = true;
            commandLiteral = resolve(COMMAND_FACTORIES, String.class);
            if (commandLiteral == null) {
                SkyJet.LOG.warn("No Fabric API client command factory found; /skyjet is unavailable. Use the keybind.");
            }
        }
        if (commandLiteral == null) return null;
        try {
            return (LiteralArgumentBuilder<FabricClientCommandSource>) commandLiteral.invoke(null, name);
        } catch (ReflectiveOperationException e) {
            SkyJet.LOG.warn("Failed to build the /skyjet command", e);
            return null;
        }
    }

    public static void actionBar(Minecraft mc, Component message) {
        if (mc.player == null) return;
        //? if >=26 {
        mc.player.sendSystemMessage(message);
        //?} else {
        /*mc.player.displayClientMessage(message, true);
        *///?}
    }
}
