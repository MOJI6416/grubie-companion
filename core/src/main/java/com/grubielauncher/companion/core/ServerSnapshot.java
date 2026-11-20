package com.grubielauncher.companion.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class ServerSnapshot {
    public static final int FORMAT = 1;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public record Advancement(boolean done, List<String> criteria) {
    }

    public record Player(String uuid, String name) {
    }

    private final Map<String, String> lastWritten = new HashMap<>();

    public static String normalizeAddress(String address) {
        if (address == null) return null;
        String value = address.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith(":25565")) value = value.substring(0, value.length() - 6);
        while (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        return value.isEmpty() ? null : value;
    }

    public static String fileName(String normalizedAddress, String uuid) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest((normalizedAddress + "\n" + uuid.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex + ".json";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static JsonObject build(
            String address,
            String serverName,
            Player player,
            String minecraft,
            Map<String, Map<String, Integer>> stats,
            Map<String, Advancement> advancements,
            Instant time) {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("server", address);
        if (serverName != null) root.addProperty("serverName", serverName);
        JsonObject playerJson = new JsonObject();
        playerJson.addProperty("uuid", player.uuid());
        playerJson.addProperty("name", player.name());
        root.add("player", playerJson);
        root.addProperty("minecraft", minecraft);
        root.addProperty("time", time.toString());

        JsonObject statsJson = new JsonObject();
        new TreeMap<>(stats).forEach((type, values) -> {
            JsonObject category = new JsonObject();
            new TreeMap<>(values).forEach((key, value) -> {
                if (value != null && value != 0) category.addProperty(key, value);
            });
            if (category.size() > 0) statsJson.add(type, category);
        });
        root.add("stats", statsJson);

        JsonObject advancementsJson = new JsonObject();
        new TreeMap<>(advancements).forEach((id, progress) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("done", progress.done());
            JsonArray criteria = new JsonArray();
            progress.criteria().stream().sorted().forEach(criteria::add);
            entry.add("criteria", criteria);
            advancementsJson.add(id, entry);
        });
        root.add("advancements", advancementsJson);
        return root;
    }

    public synchronized boolean write(Path directory, JsonObject snapshot) throws IOException {
        String address = snapshot.get("server").getAsString();
        String uuid = snapshot.getAsJsonObject("player").get("uuid").getAsString();
        String name = fileName(address, uuid);

        JsonObject comparable = snapshot.deepCopy();
        comparable.remove("time");
        String signature = GSON.toJson(comparable);
        if (signature.equals(lastWritten.get(name))) return false;

        Files.createDirectories(directory);
        Path target = directory.resolve(name);
        Path temporary = directory.resolve(name + "." + Long.toHexString(System.nanoTime()) + ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(snapshot), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        lastWritten.put(name, signature);
        return true;
    }
}
