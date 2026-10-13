package dev.skyjet;

import com.mojang.blaze3d.platform.InputConstants;
import dev.skyjet.compat.Platform;
import dev.skyjet.config.ConfigManager;
import dev.skyjet.gui.SkyJetScreen;
import dev.skyjet.render.SkyCompat;
import dev.skyjet.render.SkyJetRenderer;
import dev.skyjet.render.SkyState;
import dev.skyjet.render.SkyTextures;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class SkyJetClient implements ClientModInitializer {
    public static final String MC_VERSION = /*$ mc_version*/ "26.2";

    private static KeyMapping openScreen;
    private static KeyMapping toggleSky;

    @Override
    @SuppressWarnings("deprecation") // resource-loader-v0 is deprecated but is the only reload
    // listener API with the same shape on all three target versions.
    public void onInitializeClient() {
        ConfigManager.init();
        SkyState.resetClock();

        KeyMapping.Category category = KeyMapping.Category.register(SkyJet.id("main"));
        openScreen = Platform.registerKey(new KeyMapping(
                "key.skyjet.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, category));
        toggleSky = Platform.registerKey(new KeyMapping(
                "key.skyjet.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openScreen.consumeClick()) {
                SkyCompat.setScreen(client, new SkyJetScreen());
            }
            while (toggleSky.consumeClick()) {
                ConfigManager.setModEnabled(!ConfigManager.modEnabled());
                ConfigManager.save();
                Platform.actionBar(client, Component.literal(
                        ConfigManager.modEnabled() ? "SkyJet enabled" : "SkyJet disabled"));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            var literal = Platform.literal("skyjet");
            if (literal == null) return;
            dispatcher.register(literal.executes(ctx -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> SkyCompat.setScreen(mc, new SkyJetScreen()));
                return 1;
            }));
        });

        // The resource manager instance is reused across reloads, so nothing about it says the
        // textures behind it were thrown away. Without this the sky keeps sampling GPU views that
        // the texture manager already closed, which reads back as solid black.
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return SkyJet.id("sky_textures");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        SkyTextures.invalidate();
                    }
                });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ConfigManager.save();
            SkyJetRenderer.close();
        });

        SkyJet.LOG.info("SkyJet ready on Minecraft {}", MC_VERSION);
    }
}
