package com.grubielauncher.companion;

import com.grubielauncher.companion.core.Json;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Settings {
    public enum Option {
        SCREENSHOTS("screenshots"),
        ACHIEVEMENTS("achievements"),
        MESSAGES("messages"),
        VOICE_HUD("voiceHud"),
        PAUSE_ROW("pauseRow"),
        SOUNDS("sounds");

        public final String key;

        Option(String key) {
            this.key = key;
        }
    }

    private static final Map<Option, Boolean> VALUES = new LinkedHashMap<>();
    private static Path file;

    private Settings() {
    }

    private static void load(Path gameDir) {
        file = gameDir.resolve("config").resolve("grubie_companion.json");
        for (Option option : Option.values()) VALUES.put(option, true);
        try {
            if (!Files.isRegularFile(file)) return;
            Json.Obj saved = Json.parseObject(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            if (saved == null) return;
            for (Option option : Option.values()) {
                if (saved.has(option.key)) VALUES.put(option, saved.bool(option.key));
            }
        } catch (Exception e) {
            Companion.LOGGER.warn("Grubie Companion settings could not be read: {}", e.toString());
        }
    }

    public static boolean on(Option option) {
        if (file == null) load(gameDir());
        return VALUES.getOrDefault(option, true);
    }

    public static void toggle(Option option) {
        VALUES.put(option, !on(option));
        save();
    }

    private static Path gameDir() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.gameDirectory != null ? mc.gameDirectory.toPath() : Path.of("").toAbsolutePath();
    }

    private static void save() {
        if (file == null) return;
        Json.Obj out = new Json.Obj();
        for (Option option : Option.values()) out.put(option.key, on(option));
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(temp, out.toString().getBytes(StandardCharsets.UTF_8));
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Companion.LOGGER.warn("Grubie Companion settings could not be saved: {}", e.toString());
        }
    }
}
