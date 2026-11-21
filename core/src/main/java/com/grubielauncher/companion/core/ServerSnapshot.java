package com.grubielauncher.companion.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class ServerSnapshot {
    public static final int FORMAT = 1;

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

    public static Json.Obj build(
            String address,
            String serverName,
            Player player,
            String minecraft,
            Map<String, Map<String, Integer>> stats,
            Map<String, Advancement> advancements,
            Instant time) {
        Json.Obj root = new Json.Obj();
        root.put("format", FORMAT);
        root.put("server", address);
        root.put("serverName", serverName);
        root.put("player", new Json.Obj().put("uuid", player.uuid()).put("name", player.name()));
        root.put("minecraft", minecraft);
        root.put("time", time.toString());
        root.put("stats", statsJson(stats));
        root.put("advancements", advancementsJson(advancements));
        return root;
    }

    public static Map<String, Object> statsJson(Map<String, Map<String, Integer>> stats) {
        Map<String, Object> result = new LinkedHashMap<>();
        new TreeMap<>(stats).forEach((type, values) -> {
            Map<String, Object> category = new LinkedHashMap<>();
            new TreeMap<>(values).forEach((key, value) -> {
                if (value != null && value != 0) category.put(key, value);
            });
            if (!category.isEmpty()) result.put(type, category);
        });
        return result;
    }

    public static Map<String, Object> advancementsJson(Map<String, Advancement> advancements) {
        Map<String, Object> result = new LinkedHashMap<>();
        new TreeMap<>(advancements).forEach((id, progress) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("done", progress.done());
            List<Object> criteria = new ArrayList<>(progress.criteria());
            criteria.sort((a, b) -> ((String) a).compareTo((String) b));
            entry.put("criteria", criteria);
            result.put(id, entry);
        });
        return result;
    }

    public synchronized boolean write(Path directory, Json.Obj snapshot) throws IOException {
        String address = snapshot.string("server", 255);
        Json.Obj player = snapshot.object("player");
        String uuid = player == null ? null : player.string("uuid", 64);
        if (address == null || uuid == null) return false;
        String name = fileName(address, uuid);

        Map<String, Object> comparable = new LinkedHashMap<>(snapshot.map());
        comparable.remove("time");
        String signature = Json.write(comparable);
        if (signature.equals(lastWritten.get(name))) return false;

        Files.createDirectories(directory);
        Path target = directory.resolve(name);
        Path temporary = directory.resolve(name + "." + Long.toHexString(System.nanoTime()) + ".tmp");
        try {
            Files.write(temporary, snapshot.toString().getBytes(StandardCharsets.UTF_8));
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
