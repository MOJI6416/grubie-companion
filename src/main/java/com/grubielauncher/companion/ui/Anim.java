package com.grubielauncher.companion.ui;

import java.util.HashMap;
import java.util.Map;

public final class Anim {
    private static final Map<String, float[]> VALUES = new HashMap<>();
    private static long lastFrame = System.nanoTime();
    private static float dt = 0f;

    private Anim() {
    }

    public static void frame() {
        long now = System.nanoTime();
        if (now - lastFrame < 2_000_000L) return;
        dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
    }

    public static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    public static float toward(String key, float target, long durationMs) {
        float[] value = VALUES.computeIfAbsent(key, ignored -> new float[]{target});
        float step = durationMs <= 0 ? 1f : dt * 1000f / durationMs;
        if (value[0] < target) value[0] = Math.min(target, value[0] + step);
        else if (value[0] > target) value[0] = Math.max(target, value[0] - step);
        return value[0];
    }

    public static void set(String key, float value) {
        VALUES.computeIfAbsent(key, ignored -> new float[1])[0] = value;
    }

    public static void forget(String prefix) {
        VALUES.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public static float easeOut(float t) {
        t = clamp(t);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    public static float easeInOut(float t) {
        t = clamp(t);
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    public static float clamp(float t) {
        return t < 0f ? 0f : t > 1f ? 1f : t;
    }

    public static float progress(long start, long durationMs) {
        return clamp((now() - start) / (float) Math.max(1, durationMs));
    }
}
