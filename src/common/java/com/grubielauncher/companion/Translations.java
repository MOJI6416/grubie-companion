package com.grubielauncher.companion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Translations {
    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

    private Translations() {
    }

    public static Map<String, String> merge(Map<String, String> storage, List<String> languages) {
        Map<String, String> ours = new HashMap<>();
        for (String language : languages) ours.putAll(load(language));
        boolean missing = false;
        for (String key : ours.keySet()) {
            if (!storage.containsKey(key)) {
                missing = true;
                break;
            }
        }
        if (!missing) return null;
        Map<String, String> merged = new HashMap<>(storage);
        ours.forEach(merged::putIfAbsent);
        return merged;
    }

    static Map<String, String> load(String language) {
        return CACHE.computeIfAbsent(language.toLowerCase(Locale.ROOT), Translations::read);
    }

    private static Map<String, String> read(String language) {
        Map<String, String> result = new HashMap<>();
        String path = "/assets/" + Companion.MOD_ID + "/lang/" + language + ".json";
        try (InputStream stream = Translations.class.getResourceAsStream(path)) {
            if (stream == null) return result;
            JsonElement root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) return result;
            JsonObject object = root.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) result.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (Exception e) {
            Companion.LOGGER.warn("Could not read {}: {}", path, e.toString());
        }
        return result;
    }
}
