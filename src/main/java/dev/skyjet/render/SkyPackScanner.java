package dev.skyjet.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.skyjet.SkyJet;
import dev.skyjet.config.SkyboxLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Turns an installed sky resource pack into SkyJet skybox layers.
 *
 * <p>Sky packs ship in one of two formats, and the fields of both map cleanly onto
 * {@link SkyboxLayer}:
 *
 * <ul>
 *   <li><b>OptiFine custom sky</b> — {@code assets/minecraft/optifine/sky/world0/skyN.properties}
 *       naming a 3:2 source image, with {@code blend}, {@code rotate}, {@code speed},
 *       {@code axis}, {@code weather} and four {@code hh:mm} fade stops. This is what Purple
 *       Cosmos and the other overlay packs use.</li>
 *   <li><b>FabricSkyBoxes / Nuit v2</b> — {@code assets/<ns>/sky/*.json} with
 *       {@code "type": "square-textured"}, either six explicit face textures or one sheet, a
 *       {@code blend} type, a {@code fade} block already in ticks, and a {@code rotation} block.
 *       This is what Aqua Pack ships.</li>
 * </ul>
 *
 * <p>Nothing here is pack specific. Anything following either spec imports.
 */
public final class SkyPackScanner {
    /** What a scan found, so the editor can tell the player where the layers came from. */
    public record Result(List<SkyboxLayer> layers, List<String> sources, boolean hideVanillaSun,
                         boolean hideVanillaMoon, boolean hideVanillaStars) {
    }

    private SkyPackScanner() {
    }

    public static Result scan() {
        Minecraft mc = Minecraft.getInstance();
        List<SkyboxLayer> layers = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        boolean[] hide = {false, false, false};
        if (mc == null) return new Result(layers, sources, false, false, false);

        ResourceManager resources = mc.getResourceManager();
        try {
            scanFabricSkyboxes(resources, layers, sources, hide);
        } catch (Exception e) {
            SkyJet.LOG.warn("SkyJet could not read the FabricSkyBoxes sky definitions", e);
        }
        try {
            scanOptifine(resources, layers, sources);
        } catch (Exception e) {
            SkyJet.LOG.warn("SkyJet could not read the OptiFine sky definitions", e);
        }
        return new Result(layers, sources, hide[0], hide[1], hide[2]);
    }

    // ------------------------------------------------------ FabricSkyBoxes

    private static void scanFabricSkyboxes(ResourceManager resources, List<SkyboxLayer> out,
                                           List<String> sources, boolean[] hide) {
        Map<Identifier, Resource> found =
                resources.listResources("sky", id -> id.getPath().endsWith(".json"));
        List<Map.Entry<Identifier, Resource>> ordered = new ArrayList<>(found.entrySet());
        ordered.sort(Comparator.comparing(e -> e.getKey().toString()));

        for (Map.Entry<Identifier, Resource> entry : ordered) {
            Identifier id = entry.getKey();
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) continue;
                JsonObject root = parsed.getAsJsonObject();

                String type = string(root, "type", "");
                if (!type.contains("square-textured") && !type.contains("multi-texture")
                        && !root.has("textures") && !root.has("texture")) {
                    continue;
                }

                SkyboxLayer layer = new SkyboxLayer();
                layer.name = prettyName(id);
                layer.blendMode = blendFromName(nestedString(root, "blend", "type"));

                if (root.has("textures") && root.get("textures").isJsonObject()) {
                    JsonObject textures = root.getAsJsonObject("textures");
                    layer.mode = SkyboxLayer.MODE_CUBE_FACES;
                    layer.faceTextures = new String[SkyTextures.FACE_KEYS.length];
                    boolean complete = true;
                    for (int i = 0; i < SkyTextures.FACE_KEYS.length; i++) {
                        String face = string(textures, SkyTextures.FACE_KEYS[i], "");
                        if (face.isEmpty()) {
                            complete = false;
                            break;
                        }
                        layer.faceTextures[i] = face;
                    }
                    if (!complete) continue;
                    layer.texture = layer.faceTextures[0];
                } else {
                    layer.mode = SkyboxLayer.MODE_CUBE_ATLAS;
                    layer.texture = string(root, "texture", "");
                    if (layer.texture.isEmpty()) continue;
                }

                JsonObject properties = root.has("properties") && root.get("properties").isJsonObject()
                        ? root.getAsJsonObject("properties") : new JsonObject();

                if (properties.has("fade") && properties.get("fade").isJsonObject()) {
                    JsonObject fade = properties.getAsJsonObject("fade");
                    boolean always = bool(fade, "alwaysOn", false);
                    if (!always && fade.has("startFadeIn")) {
                        layer.fadeEnabled = true;
                        layer.startFadeIn = number(fade, "startFadeIn", 22000);
                        layer.endFadeIn = number(fade, "endFadeIn", 23500);
                        layer.startFadeOut = number(fade, "startFadeOut", 11000);
                        layer.endFadeOut = number(fade, "endFadeOut", 12500);
                    }
                }

                if (properties.has("rotation") && properties.get("rotation").isJsonObject()) {
                    JsonObject rotation = properties.getAsJsonObject("rotation");
                    layer.rotate = bool(properties, "shouldRotate", true);
                    layer.rotationSpeed = number(rotation, "rotationSpeed", 1.0f);
                    float[] axis = vec3(rotation, "axis", 0.0f, 0.0f, 1.0f);
                    // The spec writes the axis in degrees per component; what matters to us is
                    // which way it points, so it is used as a direction.
                    layer.axisX = axis[0];
                    layer.axisY = axis[1];
                    layer.axisZ = axis[2];
                    float[] fixed = vec3(rotation, "static", 0.0f, 0.0f, 0.0f);
                    layer.pitch = (float) Math.toRadians(fixed[0]);
                    layer.yaw = (float) Math.toRadians(fixed[1]);
                    layer.roll = (float) Math.toRadians(fixed[2]);
                }

                if (root.has("conditions") && root.get("conditions").isJsonObject()) {
                    JsonObject conditions = root.getAsJsonObject("conditions");
                    if (conditions.has("weather") && conditions.get("weather").isJsonArray()) {
                        layer.inClear = false;
                        layer.inRain = false;
                        layer.inThunder = false;
                        for (JsonElement w : conditions.getAsJsonArray("weather")) {
                            String name = w.getAsString().toLowerCase(java.util.Locale.ROOT);
                            if (name.contains("clear")) layer.inClear = true;
                            if (name.contains("rain")) layer.inRain = true;
                            if (name.contains("thunder")) layer.inThunder = true;
                        }
                    }
                }

                if (root.has("decorations") && root.get("decorations").isJsonObject()) {
                    JsonObject decorations = root.getAsJsonObject("decorations");
                    if (!bool(decorations, "showSun", true)) hide[0] = true;
                    if (!bool(decorations, "showMoon", true)) hide[1] = true;
                    if (!bool(decorations, "showStars", true)) hide[2] = true;
                }

                out.add(layer);
                sources.add(id.toString() + " (FabricSkyBoxes)");
            } catch (Exception e) {
                SkyJet.LOG.warn("Skipping unreadable sky definition {}", id, e);
            }
        }
    }

    // ---------------------------------------------------------- OptiFine

    private static void scanOptifine(ResourceManager resources, List<SkyboxLayer> out,
                                     List<String> sources) {
        Map<Identifier, Resource> found = resources.listResources(
                "optifine/sky", id -> id.getPath().endsWith(".properties"));
        List<Map.Entry<Identifier, Resource>> ordered = new ArrayList<>(found.entrySet());
        ordered.sort(Comparator.comparing(e -> e.getKey().toString()));

        for (Map.Entry<Identifier, Resource> entry : ordered) {
            Identifier id = entry.getKey();
            String path = id.getPath();
            // world0 is the overworld. Other dimensions are imported only if nothing else exists,
            // because SkyJet already has its own per-dimension switches.
            if (!path.contains("/world0/")) continue;
            String file = path.substring(path.lastIndexOf('/') + 1);
            if (file.startsWith("sun") || file.startsWith("moon")) continue;

            try (BufferedReader reader = entry.getValue().openAsReader()) {
                Properties props = new Properties();
                props.load(reader);

                SkyboxLayer layer = new SkyboxLayer();
                layer.name = prettyName(id);
                layer.mode = SkyboxLayer.MODE_CUBE_ATLAS;

                String source = props.getProperty("source", "./" + file.replace(".properties", ".png"));
                layer.texture = resolveOptifineSource(id, source);

                layer.blendMode = blendFromName(props.getProperty("blend", "add"));
                layer.rotate = Boolean.parseBoolean(props.getProperty("rotate", "false").trim());
                layer.rotationSpeed = parseFloat(props.getProperty("speed"), 1.0f);

                float[] axis = parseVec3(props.getProperty("axis"), 0.0f, 0.0f, 1.0f);
                layer.axisX = axis[0];
                layer.axisY = axis[1];
                layer.axisZ = axis[2];

                Float startIn = parseClock(props.getProperty("startFadeIn"));
                Float endIn = parseClock(props.getProperty("endFadeIn"));
                Float startOut = parseClock(props.getProperty("startFadeOut"));
                Float endOut = parseClock(props.getProperty("endFadeOut"));
                if (startIn != null && endIn != null && startOut != null && endOut != null) {
                    layer.fadeEnabled = true;
                    layer.startFadeIn = startIn;
                    layer.endFadeIn = endIn;
                    layer.startFadeOut = startOut;
                    layer.endFadeOut = endOut;
                }

                String weather = props.getProperty("weather");
                if (weather != null && !weather.isBlank()) {
                    layer.inClear = weather.contains("clear");
                    layer.inRain = weather.contains("rain");
                    layer.inThunder = weather.contains("thunder");
                }

                out.add(layer);
                sources.add(id.toString() + " (OptiFine)");
            } catch (Exception e) {
                SkyJet.LOG.warn("Skipping unreadable sky properties {}", id, e);
            }
        }
    }

    /** OptiFine sources are usually {@code ./skyN.png}, relative to the properties file. */
    private static String resolveOptifineSource(Identifier id, String source) {
        String value = source.trim();
        if (value.contains(":")) return value;
        String dir = id.getPath().substring(0, id.getPath().lastIndexOf('/') + 1);
        if (value.startsWith("./")) value = value.substring(2);
        else if (value.startsWith("/")) return id.getNamespace() + ":" + value.substring(1);
        return id.getNamespace() + ":" + dir + value;
    }

    // ------------------------------------------------------------ helpers

    /** Minecraft time runs from tick 0 at 06:00, and an in-game hour is a thousand ticks. */
    static Float parseClock(String value) {
        if (value == null || value.isBlank()) return null;
        String[] parts = value.trim().split(":");
        try {
            int hours = Integer.parseInt(parts[0].trim());
            int minutes = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            float ticks = ((hours * 60 + minutes) - 360) * (1000.0f / 60.0f);
            ticks = ticks % 24000.0f;
            if (ticks < 0) ticks += 24000.0f;
            return ticks;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static int blendFromName(String name) {
        if (name == null) return SkyboxLayer.BLEND_ADD;
        return switch (name.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "add" -> 1;
            case "multiply" -> 2;
            case "screen" -> 3;
            case "overlay" -> 4;
            case "subtract" -> 7;
            case "dodge" -> 8;
            case "burn" -> 9;
            case "replace" -> SkyboxLayer.BLEND_REPLACE;
            case "alpha", "normal", "" -> SkyboxLayer.BLEND_ALPHA;
            default -> SkyboxLayer.BLEND_ADD;
        };
    }

    private static String prettyName(Identifier id) {
        String path = id.getPath();
        String file = path.substring(path.lastIndexOf('/') + 1);
        int dot = file.lastIndexOf('.');
        if (dot > 0) file = file.substring(0, dot);
        return id.getNamespace() + ":" + file;
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float[] parseVec3(String value, float x, float y, float z) {
        if (value == null || value.isBlank()) return new float[]{x, y, z};
        String[] parts = value.trim().split("\\s+");
        if (parts.length < 3) return new float[]{x, y, z};
        return new float[]{
                parseFloat(parts[0], x), parseFloat(parts[1], y), parseFloat(parts[2], z)
        };
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement e = object.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    private static String nestedString(JsonObject object, String key, String inner) {
        JsonElement e = object.get(key);
        if (e == null) return null;
        if (e.isJsonPrimitive()) return e.getAsString();
        if (e.isJsonObject()) return string(e.getAsJsonObject(), inner, null);
        return null;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        JsonElement e = object.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : fallback;
    }

    private static float number(JsonObject object, String key, float fallback) {
        JsonElement e = object.get(key);
        try {
            return e != null && e.isJsonPrimitive() ? e.getAsFloat() : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static float[] vec3(JsonObject object, String key, float x, float y, float z) {
        JsonElement e = object.get(key);
        if (e == null || !e.isJsonArray()) return new float[]{x, y, z};
        JsonArray array = e.getAsJsonArray();
        if (array.size() < 3) return new float[]{x, y, z};
        try {
            return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
        } catch (NumberFormatException ex) {
            return new float[]{x, y, z};
        }
    }
}
