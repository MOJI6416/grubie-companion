package com.grubielauncher.companion.ui;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.core.Protocol;
import com.grubielauncher.companion.render.Tex;
import com.grubielauncher.companion.render.Text;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Avatars {
    private static final int SIZE = 64;
    private static final int MAX = 96;
    private static final int[] COLORS = {0xFF7C5CFF, 0xFF34C38A, 0xFF5FB0EF, 0xFFF0B43C, 0xFFEF6B7C, 0xFF3FBFBF, 0xFFD97AE0};
    private static final Map<String, Tex> TEXTURES = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Tex> eldest) {
            if (size() <= MAX) return false;
            eldest.getValue().close();
            return true;
        }
    };

    private Avatars() {
    }

    public static void accept(Protocol.Avatar avatar) {
        Tex source = null;
        try {
            source = Tex.fromPng("avatar_src", avatar.png());
            Tex rounded = Tex.create("avatar", SIZE, SIZE);
            int sw = source.width();
            int sh = source.height();
            float radius = SIZE * 0.22f;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int argb = source.get(Math.min(sw - 1, x * sw / SIZE), Math.min(sh - 1, y * sh / SIZE));
                    float coverage = corner(x + 0.5f, y + 0.5f, radius);
                    int a = Math.round(((argb >>> 24) & 0xff) * coverage);
                    rounded.set(x, y, (a << 24) | (argb & 0xffffff));
                }
            }
            rounded.upload();
            Tex previous = TEXTURES.put(avatar.id(), rounded);
            if (previous != null) previous.close();
        } catch (Exception e) {
            Companion.LOGGER.debug("Avatar {} was not loaded: {}", avatar.id(), e.toString());
        } finally {
            if (source != null) source.close();
        }
    }

    public static void draw(Ui ui, String id, String name, float x, float y, float size) {
        Tex tex = id == null ? null : TEXTURES.get(id);
        if (tex != null) {
            ui.canvas.image(tex, x, y, size, size, 0xFFFFFFFF);
            return;
        }
        String seed = name == null || name.isEmpty() ? "?" : name;
        int color = COLORS[Math.floorMod(seed.hashCode(), COLORS.length)];
        ui.canvas.round(x, y, size, size, size * 0.22f, Theme.mix(color, 0xFF16171A, 0.55f));
        String letter = seed.substring(0, seed.offsetByCodePoints(0, 1)).toUpperCase();
        float textSize = size * 0.5f;
        float lh = ui.lineHeight(textSize);
        ui.textCentered(letter, x + size / 2f, y + (size - lh) / 2f, textSize, Text.Weight.SEMIBOLD, Theme.mix(color, 0xFFFFFFFF, 0.55f));
    }

    public static void reset() {
        TEXTURES.values().forEach(Tex::close);
        TEXTURES.clear();
    }

    private static float corner(float x, float y, float r) {
        float cx = x < r ? r : x > SIZE - r ? SIZE - r : x;
        float cy = y < r ? r : y > SIZE - r ? SIZE - r : y;
        float d = (float) Math.hypot(x - cx, y - cy);
        return Math.max(0f, Math.min(1f, r - d + 0.5f));
    }
}
