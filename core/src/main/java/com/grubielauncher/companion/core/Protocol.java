package com.grubielauncher.companion.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Protocol {
    public static final int VERSION = 1;
    static final int MAX_TOASTS = 8;
    static final int MAX_GUESTS = 64;
    static final int MAX_TEXT = 300;

    private Protocol() {
    }

    public record ToastItem(String id, String kind, String title, String body, int count, boolean actionable) {
    }

    public record Guest(String key, String name, String transport) {
    }

    public record HostState(boolean active, String kind, List<Guest> guests) {
        public static final HostState NONE = new HostState(false, "", List.of());
    }

    public record ConnectRequest(String address, String name) {
    }

    public static JsonObject hello(String token, String modVersion, String minecraft, String loader) {
        JsonObject message = new JsonObject();
        message.addProperty("t", "hello");
        message.addProperty("protocol", VERSION);
        message.addProperty("token", token);
        message.addProperty("mod", modVersion);
        message.addProperty("minecraft", minecraft);
        message.addProperty("loader", loader);
        return message;
    }

    public static JsonObject accept(String toastId) {
        JsonObject message = new JsonObject();
        message.addProperty("t", "accept");
        message.addProperty("id", toastId);
        return message;
    }

    public static JsonObject kick(String guestKey) {
        JsonObject message = new JsonObject();
        message.addProperty("t", "kick");
        message.addProperty("key", guestKey);
        return message;
    }

    public static String type(JsonObject message) {
        return string(message, "t", 32);
    }

    public static List<ToastItem> toasts(JsonObject message) {
        JsonArray items = array(message, "items");
        if (items == null) return List.of();
        List<ToastItem> result = new ArrayList<>();
        for (JsonElement element : items) {
            if (!element.isJsonObject() || result.size() >= MAX_TOASTS) continue;
            JsonObject item = element.getAsJsonObject();
            String id = string(item, "id", 200);
            String title = string(item, "title", MAX_TEXT);
            if (id == null || title == null) continue;
            String kind = string(item, "kind", 16);
            String body = string(item, "body", MAX_TEXT);
            int count = integer(item, "count", 1);
            result.add(new ToastItem(
                    id,
                    kind == null ? "message" : kind,
                    title,
                    body == null ? "" : body,
                    Math.max(1, count),
                    bool(item, "actionable")));
        }
        return Collections.unmodifiableList(result);
    }

    public static HostState host(JsonObject message) {
        if (!bool(message, "active")) return HostState.NONE;
        String kind = string(message, "kind", 16);
        JsonArray items = array(message, "guests");
        List<Guest> guests = new ArrayList<>();
        if (items != null) {
            for (JsonElement element : items) {
                if (!element.isJsonObject() || guests.size() >= MAX_GUESTS) continue;
                JsonObject item = element.getAsJsonObject();
                String key = string(item, "key", 200);
                String name = string(item, "name", 64);
                if (key == null) continue;
                String transport = string(item, "transport", 16);
                guests.add(new Guest(key, name == null ? "?" : name, transport == null ? "relay" : transport));
            }
        }
        return new HostState(true, kind == null ? "world" : kind, Collections.unmodifiableList(guests));
    }

    public static ConnectRequest connect(JsonObject message) {
        String address = string(message, "address", 255);
        if (address == null) return null;
        String name = string(message, "name", 64);
        return new ConnectRequest(address, name == null ? address : name);
    }

    static String string(JsonObject object, String key, int limit) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return null;
        String text = value.getAsString().replaceAll("\\p{Cntrl}", " ").trim();
        if (text.isEmpty()) return null;
        return text.length() > limit ? text.substring(0, limit) : text;
    }

    static int integer(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return fallback;
        try {
            return value.getAsInt();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    static boolean bool(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
    }

    static JsonArray array(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }
}
