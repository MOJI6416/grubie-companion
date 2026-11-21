package com.grubielauncher.companion;

import com.grubielauncher.companion.core.Json;

import java.io.InputStream;
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
            Json.Obj object = Json.parseObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            if (object == null) return result;
            for (Map.Entry<String, Object> entry : object.map().entrySet()) {
                if (entry.getValue() instanceof String) result.put(entry.getKey(), (String) entry.getValue());
            }
        } catch (Exception e) {
            Companion.LOGGER.warn("Could not read {}: {}", path, e.toString());
        }
        return result;
    }
}
