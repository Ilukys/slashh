package com.misanthropy.slashh.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Where each Better Combat swing animation puts its cut. Read from every assets/*&#47;bc_particle/particle_settings.json
 * (the format Better Combat Particle uses); animations nobody described are guessed from their name.
 */
public final class SwingSpecs {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Map<String, List<Spec>> table = Map.of();

    private SwingSpecs() {}

    /** One cut. {@code tilt} is the roll in degrees; x/y/z nudge it from the swing's centre. */
    public record Spec(boolean stab, double tilt, double pitch, double x, double y, double z) {}

    public static void reload(ResourceManager manager) {
        Map<String, List<Spec>> loaded = new HashMap<>();
        manager.listResources("bc_particle", id -> id.getPath().endsWith("particle_settings.json"))
                .forEach((id, resource) -> readFile(loaded, id, resource));
        table = loaded;
    }

    private static void readFile(Map<String, List<Spec>> into, ResourceLocation id,
                                 net.minecraft.server.packs.resources.Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonObject values = GsonHelper.getAsJsonObject(GsonHelper.parse(reader), "values");
            for (String key : values.keySet()) {
                JsonElement element = values.get(key);
                JsonArray array = new JsonArray();
                if (element.isJsonArray()) {
                    array = element.getAsJsonArray();
                } else {
                    array.add(element);
                }
                List<Spec> specs = new ArrayList<>();
                for (JsonElement entry : array) {
                    Spec spec = parse(entry.getAsJsonObject());
                    if (spec != null) specs.add(spec);
                }
                into.put(key, List.copyOf(specs));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read slash settings {}", id, e);
        }
    }

    static Spec parse(JsonObject json) {
        String type = GsonHelper.getAsString(json, "particle_type", "none");
        if (!type.equals("stab") && !type.startsWith("slash")) return null;
        return new Spec(type.equals("stab"),
                GsonHelper.getAsFloat(json, "roll_set", 0.0f),
                GsonHelper.getAsFloat(json, "pitch_addition", 0.0f),
                GsonHelper.getAsFloat(json, "x_addition", 0.0f),
                GsonHelper.getAsFloat(json, "y_addition", 0.0f),
                GsonHelper.getAsFloat(json, "z_addition", 0.0f));
    }

    public static List<Spec> specs(String animation) {
        List<Spec> known = table.get(animation);
        if (known != null) return known;
        Spec guess = guess(animation);
        return guess == null ? List.of() : List.of(guess);
    }

    static Spec guess(String animation) {
        String name = animation.toLowerCase(Locale.ROOT);
        name = name.substring(name.indexOf(':') + 1);
        List<String> parts = List.of(name.split("[_./]"));

        if (has(name, "throw", "kick", "pong", "gun")) return null;
        if (has(name, "stab", "thrust", "pierce", "punch")) return new Spec(true, 0.0, 0.0, 0.0, -0.1, 0.0);
        if (has(name, "spin", "whirl", "rotate")) return arc(180.0);
        if (has(name, "slam")) return arc(-86.0);
        if (has(name, "uppercut", "upward", "heavy_up")) return arc(75.0);

        boolean left = parts.contains("left") || parts.contains("lr");
        if (has(name, "vert", "updown")) return arc(left ? -100.0 : -80.0);
        if (has(name, "uncross")) return arc(-60.0);
        if (has(name, "cross")) return arc(-120.0);
        return arc(left ? 180.0 : 0.0);
    }

    public static Spec arc(double tiltDegrees) {
        return new Spec(false, tiltDegrees, 0.0, 0.0, -0.1, 0.0);
    }

    private static boolean has(String name, String... needles) {
        for (String needle : needles) {
            if (name.contains(needle)) return true;
        }
        return false;
    }
}
