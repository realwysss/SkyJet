package dev.skyjet.gametest;

import dev.skyjet.config.AnimationSettings;
import dev.skyjet.config.ConfigManager;
import dev.skyjet.config.Presets;
import dev.skyjet.config.SkyConfig;
import dev.skyjet.config.SkyboxLayer;
import dev.skyjet.render.SkyPackScanner;
import dev.skyjet.gui.SkyJetScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public final class SkyJetClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitTick();

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            //? if >=26.2 {
            singleplayer.getConnection().waitForChunksRender();
            //?} elif >=26 {
            /*singleplayer.getClientLevel().waitForChunksRender();
            *///?} else {
            /*singleplayer.getClientWorld().waitForChunksRender();
            *///?}

            applyPreset(context, "SkyJet Default", 0.0f);
            context.waitTicks(10);
            context.takeScreenshot("skyjet-01-default-day");

            applyPreset(context, "Violet Cosmos", 0.5f);
            context.waitTicks(10);
            context.takeScreenshot("skyjet-02-deep-cosmos");

            applyPreset(context, "Aurora Storm", 0.5f);
            context.waitTicks(10);
            context.takeScreenshot("skyjet-03-aurora");

            applyPreset(context, "Alpenglow", 0.24f);
            context.waitTicks(10);
            context.takeScreenshot("skyjet-04-sunset");

            applyPreset(context, "Synthwave", 0.3f);
            context.waitTicks(10);
            context.takeScreenshot("skyjet-05-synthwave");

            applyPreset(context, "Milky Way Core", 0.5f);
            context.waitTicks(10);
            context.takeScreenshot("skyjet-06-milkyway");

            // Presets added for overhead interest and moving bodies; these catch a blank zenith.
            applyPreset(context, "Ringworld", 0.5f);
            context.waitTicks(10);
            context.takeScreenshot("skyjet-07-ringworld");

            applyPreset(context, "Aurora Australis", 0.5f);
            context.waitTicks(10);
            context.takeScreenshot("skyjet-08-boreal");

            applyPreset(context, "Twin Suns", 0.05f);
            context.waitTicks(10);
            context.takeScreenshot("skyjet-09-twinsuns");

            // Looking straight up, on the preset most players will actually be running. An empty
            // zenith is the single easiest way for a sky to look unfinished, so it gets its own
            // shot rather than being judged from a horizon view.
            applyPreset(context, "SkyJet Default", 0.5f);
            context.runOnClient(mc -> {
                if (mc.player != null) mc.player.setXRot(-90.0f);
            });
            context.waitTicks(12);
            context.takeScreenshot("skyjet-10-zenith-up");

            // And straight down: the sky is a full sphere, not a dome that stops at the horizon.
            context.runOnClient(mc -> {
                if (mc.player != null) mc.player.setXRot(90.0f);
            });
            context.waitTicks(6);
            context.takeScreenshot("skyjet-11-nadir-down");

            context.runOnClient(mc -> {
                if (mc.player != null) mc.player.setXRot(0.0f);
            });
            context.waitTicks(4);

            // Texture skybox: the vanilla title panorama ships six cube faces named _0 to _5,
            // which is exactly the layout the sky pack support has to handle.
            context.runOnClient(mc -> {
                SkyConfig preset = Presets.get("Desert Night");
                preset.animation.timeMode = AnimationSettings.TIME_FIXED;
                preset.animation.fixedTime = 0.5f;
                SkyboxLayer layer = new SkyboxLayer("Panorama",
                        "minecraft:textures/gui/title/background/panorama", SkyboxLayer.MODE_CUBE_FACES);
                preset.skyboxes.add(layer);
                ConfigManager.replaceActive(preset);
            });
            context.waitTicks(20);
            context.takeScreenshot("skyjet-12-skybox-cubemap");

            // Real sky pack import. The dev run ships Purple Cosmos, which is an OptiFine custom
            // sky: three layers, a 3:2 cube sheet each, with blend and rotation settings. If the
            // scanner and the cube sheet mapping are right, this comes out looking like the pack.
            // A generated OptiFine sky pack with one solid colour per cube face. Pointing the
            // camera at each compass direction then tells us whether the 3:2 cube sheet mapping
            // put every face where the format says it goes.
            context.runOnClient(SkyJetClientTest::writeFaceTestPack);

            // The harness rewrites options.txt every run, so the pack is enabled from code.
            context.runOnClient(mc -> {
                var repo = mc.getResourcePackRepository();
                repo.reload();
                System.out.println("[SkyJetTest] available packs: " + repo.getAvailableIds());
                java.util.List<String> selected = new java.util.ArrayList<>(repo.getSelectedIds());
                for (String id : repo.getAvailableIds()) {
                    if (id.toLowerCase(java.util.Locale.ROOT).contains("skyjetfacetest") && !selected.contains(id)) {
                        selected.add(id);
                    }
                }
                repo.setSelected(selected);
                System.out.println("[SkyJetTest] selected packs: " + repo.getSelectedIds());
            });
            context.runOnClient(mc -> mc.reloadResourcePacks());
            context.waitTicks(120);

            context.runOnClient(mc -> {
                SkyPackScanner.Result found = SkyPackScanner.scan();
                System.out.println("[SkyJetTest] sky pack layers found: " + found.layers().size());
                for (String source : found.sources()) {
                    System.out.println("[SkyJetTest]   " + source);
                }
                SkyConfig sky = SkyConfig.blank("Imported");
                sky.animation.timeMode = AnimationSettings.TIME_FIXED;
                sky.animation.fixedTime = 0.5f;
                for (SkyboxLayer layer : found.layers()) {
                    if (sky.skyboxes.size() >= SkyConfig.MAX_SKYBOXES) break;
                    sky.skyboxes.add(layer);
                }
                ConfigManager.replaceActive(sky);
            });
            context.waitTicks(20);

            // One shot per face. Colours are checked by eye against the legend in the test pack:
            // bottom red, top green, south blue, west yellow, north magenta, east cyan.
            faceShot(context, "north", 180.0f, 0.0f);
            faceShot(context, "east", 270.0f, 0.0f);
            faceShot(context, "south", 0.0f, 0.0f);
            faceShot(context, "west", 90.0f, 0.0f);
            faceShot(context, "top", 0.0f, -89.0f);
            // The bottom face is under the world, so the only way to see it is from the void.
            // y=-100 sits below the world but above the void damage line at minY-64, which
            // kills spectators too and would respawn the player mid-shot with the pitch reset.
            singleplayer.getServer().runCommand("gamemode spectator Player0");
            singleplayer.getServer().runCommand("tp Player0 0 -100 0");
            context.waitTicks(10);
            faceShot(context, "bottom", 0.0f, 89.0f);
            singleplayer.getServer().runCommand("gamemode creative Player0");
            singleplayer.getServer().runCommand("tp Player0 0 90 0");
            context.waitTicks(10);

            context.runOnClient(mc -> {
                if (mc.player != null) {
                    mc.player.setXRot(0.0f);
                    mc.player.setYRot(0.0f);
                }
            });
            context.waitTicks(5);

            // World-driven time: proves the vanilla celestial angle is converted correctly.
            singleplayer.getServer().runCommand("time set midnight");
            context.runOnClient(mc -> {
                SkyConfig preset = Presets.get("Aurora Storm");
                preset.animation.timeMode = AnimationSettings.TIME_WORLD;
                ConfigManager.replaceActive(preset);
            });
            context.waitTicks(20);
            context.takeScreenshot("skyjet-13-worldtime-midnight");

            singleplayer.getServer().runCommand("time set noon");
            context.runOnClient(mc -> {
                SkyConfig preset = Presets.get("SkyJet Default");
                preset.animation.timeMode = AnimationSettings.TIME_WORLD;
                ConfigManager.replaceActive(preset);
            });
            context.waitTicks(20);
            context.takeScreenshot("skyjet-14-worldtime-noon");

            singleplayer.getServer().runCommand("time set 12800");
            context.waitTicks(20);
            context.takeScreenshot("skyjet-15-worldtime-dusk");

            // Local clock: the sky should ignore the world time entirely and follow the machine.
            context.runOnClient(mc -> {
                SkyConfig preset = Presets.get("Blue Hour");
                preset.animation.timeMode = AnimationSettings.TIME_REALTIME;
                ConfigManager.replaceActive(preset);
            });
            context.waitTicks(20);
            context.takeScreenshot("skyjet-16-localclock");

            // The mark is painted on the bottom face of the skybox, so seeing it means getting
            // under the world and looking straight down.
            singleplayer.getServer().runCommand("gamemode spectator Player0");
            singleplayer.getServer().runCommand("tp Player0 0 -100 0");
            applyPreset(context, "Violet Cosmos", 0.5f);
            context.runOnClient(mc -> {
                if (mc.player != null) {
                    mc.player.setXRot(85.0f);
                    mc.player.setYRot(0.0f);
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("skyjet-17-skybox-watermark");

            // Straight down with the sky continuing below the horizon rather than going black.
            context.runOnClient(mc -> {
                SkyConfig preset = Presets.get("Alpenglow");
                preset.animation.timeMode = AnimationSettings.TIME_FIXED;
                preset.animation.fixedTime = 0.24f;
                preset.belowHorizon = SkyConfig.BELOW_CONTINUE;
                ConfigManager.replaceActive(preset);
                if (mc.player != null) mc.player.setXRot(45.0f);
            });
            context.waitTicks(16);
            context.takeScreenshot("skyjet-17b-below-horizon");

            singleplayer.getServer().runCommand("gamemode creative Player0");
            singleplayer.getServer().runCommand("tp Player0 0 90 0");
            context.runOnClient(mc -> {
                if (mc.player != null) mc.player.setXRot(0.0f);
            });
            context.waitTicks(10);

            // View bobbing.
            //
            // The sky is reconstructed from the view matrix, so anything the game folds into that
            // matrix lands on the sky as movement. Bobbing folds in three things: sway, roll and
            // several degrees of pitch per step. SkyCompat now builds the rotation from the
            // camera's own orientation instead, so none of it can reach the sky.
            //
            // This shot pins the resting case: with bobbing switched on and the player standing
            // still, the sky must look exactly the same as it did with the old matrix, which is
            // what proves the new one is not off by a rotation. The moving case cannot be tested
            // here, because the harness does not deliver movement input to the player.
            context.runOnClient(mc -> {
                mc.options.bobView().set(true);
                SkyConfig sky = Presets.get("Blue Hour");
                sky.animation.timeMode = AnimationSettings.TIME_FIXED;
                sky.animation.fixedTime = 0.5f;
                sky.animation.speed = 0.0f;
                ConfigManager.replaceActive(sky);
                if (mc.player != null) {
                    mc.player.setXRot(-20.0f);
                    mc.player.setYRot(0.0f);
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("skyjet-17c-bobbing-on");

            context.runOnClient(mc -> mc.options.bobView().set(false));
            singleplayer.getServer().runCommand("gamemode creative Player0");
            singleplayer.getServer().runCommand("tp Player0 0 90 0");
            context.runOnClient(mc -> {
                if (mc.player != null) mc.player.setXRot(0.0f);
            });
            context.waitTicks(10);

            context.runOnClient(mc -> {
                mc.options.guiScale().set(1);
                //? if >=26 {
                mc.resizeGui();
                //?} else {
                /*mc.resizeDisplay();
                *///?}
            });
            context.waitTicks(5);

            context.setScreen(SkyJetScreen::new);
            context.waitTicks(20);
            context.takeScreenshot("skyjet-18-gui-presets");

            // The first screenshot catches the one-time mode chooser. Answering it with "All
            // settings" is what puts every tab in the sidebar, so the panels below are shot
            // against the sidebar a player would actually be looking at.
            context.runOnClient(mc -> ConfigManager.setGuiMode(ConfigManager.MODE_ADVANCED));
            context.setScreen(SkyJetScreen::new);
            context.waitTicks(12);
            context.takeScreenshot("skyjet-18b-gui-advanced");

            clickTab(context, "Skybox");
            context.takeScreenshot("skyjet-19-gui-skybox");

            clickTab(context, "Gradients");
            context.takeScreenshot("skyjet-20-gui-gradients");

            clickTab(context, "Stars");
            context.takeScreenshot("skyjet-21-gui-stars");

            clickTab(context, "Aurora");
            context.takeScreenshot("skyjet-22-gui-aurora");

            clickTab(context, "Bodies");
            context.takeScreenshot("skyjet-23-gui-bodies");

            clickTab(context, "Image");
            context.takeScreenshot("skyjet-24-gui-image");

            clickTab(context, "World");
            context.takeScreenshot("skyjet-25-gui-world");

            clickTab(context, "Layers");
            context.takeScreenshot("skyjet-26-gui-layers");

            clickTab(context, "Clouds");
            context.takeScreenshot("skyjet-27-gui-clouds");

            clickTab(context, "Profiles");
            context.takeScreenshot("skyjet-28-gui-profiles");

            clickTab(context, "Guide");
            context.takeScreenshot("skyjet-29-gui-guide");

            clickTab(context, "Interface");
            context.takeScreenshot("skyjet-30-gui-interface");

            context.runOnClient(mc -> {
                mc.options.guiScale().set(0);
                //? if >=26 {
                mc.resizeGui();
                //?} else {
                /*mc.resizeDisplay();
                *///?}
            });
            context.waitTicks(5);
        }
    }


    /** Points the camera one way and captures what the imported cube sheet put there. */
    private static void faceShot(ClientGameTestContext context, String name, float yaw, float pitch) {
        context.runOnClient(mc -> {
            if (mc.player != null) {
                mc.player.setYRot(yaw);
                mc.player.setXRot(pitch);
            }
        });
        context.waitTicks(8);
        context.takeScreenshot("skyjet-12-face-" + name);
    }

    /**
     * Writes a minimal OptiFine custom sky pack into the resource pack folder: one properties
     * file and one 3:2 sheet whose six cells are solid, distinguishable colours.
     */
    private static void writeFaceTestPack(net.minecraft.client.Minecraft mc) {
        // Atlas cell order is bottom, top, south on the top row, then west, north, east.
        int[] colours = {
                0xFFFF0000, 0xFF00FF00, 0xFF0000FF,
                0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF
        };
        int cell = 64;
        int width = cell * 3;
        int height = cell * 2;
        byte[] png;
        try (com.mojang.blaze3d.platform.NativeImage image =
                     new com.mojang.blaze3d.platform.NativeImage(width, height, false)) {
            for (int i = 0; i < 6; i++) {
                int ox = (i % 3) * cell;
                int oy = (i / 3) * cell;
                image.fillRect(ox, oy, cell, cell, colours[i]);
            }
            java.nio.file.Path tmp = mc.getResourcePackDirectory().resolve("skyjet-face-test.png");
            java.nio.file.Files.createDirectories(mc.getResourcePackDirectory());
            image.writeToFile(tmp);
            png = java.nio.file.Files.readAllBytes(tmp);
            java.nio.file.Files.deleteIfExists(tmp);
        } catch (Exception e) {
            throw new AssertionError("Could not build the face test image", e);
        }

        java.nio.file.Path zip = mc.getResourcePackDirectory().resolve("SkyJetFaceTest.zip");
        try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(
                java.nio.file.Files.newOutputStream(zip))) {
            put(out, "pack.mcmeta",
                    ("{" + q("pack") + ":{" + q("pack_format") + ":88," + q("description")
                            + ":" + q("SkyJet face test") + "}}")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            put(out, "assets/minecraft/optifine/sky/world0/sky1.properties",
                    String.join("\n", "source=./sky1.png", "blend=replace", "rotate=false", "")
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            put(out, "assets/minecraft/optifine/sky/world0/sky1.png", png);
        } catch (Exception e) {
            throw new AssertionError("Could not write the face test pack", e);
        }
    }

    /** Wraps a value in json quotes without fighting escaping in a source literal. */
    private static String q(String value) {
        return (char) 34 + value + (char) 34;
    }

    private static void put(java.util.zip.ZipOutputStream out, String name, byte[] data) throws java.io.IOException {
        out.putNextEntry(new java.util.zip.ZipEntry(name));
        out.write(data);
        out.closeEntry();
    }

    /** Clicks a sidebar tab by index so the screenshots walk the whole interface. */
    private static void clickTab(ClientGameTestContext context, String title) {
        context.runOnClient(mc -> {
            dev.skyjet.gui.SkyJetScreen screen = (dev.skyjet.gui.SkyJetScreen) SCREEN_ACCESS.get();
            screen.selectTabForTest(title);
        });
        context.waitTicks(12);
    }

    private static final java.util.function.Supplier<Object> SCREEN_ACCESS =
            () -> dev.skyjet.render.SkyCompat.currentScreen(net.minecraft.client.Minecraft.getInstance());

    private static void applyPreset(ClientGameTestContext context, String name, float timeOfDay) {
        context.runOnClient(mc -> {
            SkyConfig preset = Presets.get(name);
            if (preset == null) {
                throw new AssertionError("Missing preset: " + name);
            }
            preset.animation.timeMode = AnimationSettings.TIME_FIXED;
            preset.animation.fixedTime = timeOfDay;
            ConfigManager.replaceActive(preset);
        });
    }
}
