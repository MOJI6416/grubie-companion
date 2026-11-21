package com.grubielauncher.companion.render;

import java.util.HashMap;
import java.util.Map;

public final class Canvas {
    private static final Map<Integer, Tex> CIRCLES = new HashMap<>();
    private static final Map<Long, Tex> RINGS = new HashMap<>();
    private static final Map<Long, Tex> SHADOWS = new HashMap<>();

    public final Gfx gfx;
    public final float scale;

    private Canvas(Gfx gfx, float scale) {
        this.gfx = gfx;
        this.scale = scale;
    }

    public static Canvas begin(Gfx gfx, float guiScale) {
        return begin(gfx, guiScale, guiScale);
    }

    public static Canvas begin(Gfx gfx, float guiScale, float unit) {
        gfx.push();
        gfx.lift();
        gfx.scale(1f / Math.max(1f, guiScale));
        return new Canvas(gfx, Math.max(1f, unit));
    }

    public void end() {
        gfx.alpha(1f);
        gfx.pop();
    }

    public int px(float units) {
        return Math.round(units * scale);
    }

    public void alpha(float value) {
        gfx.alpha(value);
    }

    public void rect(float x, float y, float w, float h, int argb) {
        int x0 = px(x);
        int y0 = px(y);
        gfx.fill(x0, y0, px(x + w), px(y + h), argb);
    }

    public void round(float x, float y, float w, float h, float radius, int argb) {
        int x0 = px(x);
        int y0 = px(y);
        int x1 = px(x + w);
        int y1 = px(y + h);
        roundPx(x0, y0, x1 - x0, y1 - y0, px(radius), argb);
    }

    public void roundPx(int x, int y, int w, int h, int r, int argb) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            gfx.fill(x, y, x + w, y + h, argb);
            return;
        }
        Tex circle = circle(r);
        gfx.blit(circle, x, y, r, r, 0, 0, r, r, argb);
        gfx.blit(circle, x + w - r, y, r, r, r, 0, r, r, argb);
        gfx.blit(circle, x, y + h - r, r, r, 0, r, r, r, argb);
        gfx.blit(circle, x + w - r, y + h - r, r, r, r, r, r, r, argb);
        gfx.fill(x + r, y, x + w - r, y + r, argb);
        gfx.fill(x, y + r, x + w, y + h - r, argb);
        gfx.fill(x + r, y + h - r, x + w - r, y + h, argb);
    }

    public void border(float x, float y, float w, float h, float radius, float thickness, int argb) {
        int x0 = px(x);
        int y0 = px(y);
        int ww = px(x + w) - x0;
        int hh = px(y + h) - y0;
        int t = Math.max(1, Math.round(thickness * scale));
        int r = Math.max(t, Math.min(px(radius), Math.min(ww, hh) / 2));
        Tex ring = ring(r, t);
        gfx.blit(ring, x0, y0, r, r, 0, 0, r, r, argb);
        gfx.blit(ring, x0 + ww - r, y0, r, r, r, 0, r, r, argb);
        gfx.blit(ring, x0, y0 + hh - r, r, r, 0, r, r, r, argb);
        gfx.blit(ring, x0 + ww - r, y0 + hh - r, r, r, r, r, r, r, argb);
        gfx.fill(x0 + r, y0, x0 + ww - r, y0 + t, argb);
        gfx.fill(x0 + r, y0 + hh - t, x0 + ww - r, y0 + hh, argb);
        gfx.fill(x0, y0 + r, x0 + t, y0 + hh - r, argb);
        gfx.fill(x0 + ww - t, y0 + r, x0 + ww, y0 + hh - r, argb);
    }

    public void circleFill(float cx, float cy, float radius, int argb) {
        int r = Math.max(1, px(radius));
        int x = px(cx) - r;
        int y = px(cy) - r;
        gfx.blit(circle(r), x, y, r * 2, r * 2, 0, 0, r * 2, r * 2, argb);
    }

    public void shadow(float x, float y, float w, float h, float radius, float blur, float dy, int argb) {
        int b = Math.max(1, px(blur));
        int r = Math.max(0, px(radius));
        int x0 = px(x) - b;
        int y0 = px(y + dy) - b;
        int ww = px(x + w) - px(x) + 2 * b;
        int hh = px(y + h) - px(y) + 2 * b;
        int c = r + b;
        if (ww < 2 * c || hh < 2 * c) return;
        Tex tex = shadowTex(r, b);
        gfx.blit(tex, x0, y0, c, c, 0, 0, c, c, argb);
        gfx.blit(tex, x0 + ww - c, y0, c, c, c, 0, c, c, argb);
        gfx.blit(tex, x0, y0 + hh - c, c, c, 0, c, c, c, argb);
        gfx.blit(tex, x0 + ww - c, y0 + hh - c, c, c, c, c, c, c, argb);
        gfx.blit(tex, x0 + c, y0, ww - 2 * c, c, c, 0, 1, c, argb);
        gfx.blit(tex, x0 + c, y0 + hh - c, ww - 2 * c, c, c, c, 1, c, argb);
        gfx.blit(tex, x0, y0 + c, c, hh - 2 * c, 0, c, c, 1, argb);
        gfx.blit(tex, x0 + ww - c, y0 + c, c, hh - 2 * c, c, c, c, 1, argb);
        gfx.blit(tex, x0 + c, y0 + c, ww - 2 * c, hh - 2 * c, c, c, 1, 1, argb);
    }

    public void image(Tex tex, float x, float y, float w, float h, int argb) {
        if (tex == null) return;
        int x0 = px(x);
        int y0 = px(y);
        gfx.blit(tex, x0, y0, px(x + w) - x0, px(y + h) - y0, 0, 0, tex.width(), tex.height(), argb);
    }

    public static void reset() {
        CIRCLES.values().forEach(Tex::close);
        RINGS.values().forEach(Tex::close);
        SHADOWS.values().forEach(Tex::close);
        CIRCLES.clear();
        RINGS.clear();
        SHADOWS.clear();
    }

    private static Tex circle(int r) {
        return CIRCLES.computeIfAbsent(r, key -> {
            int d = key * 2;
            Tex tex = Tex.create("circle" + key, d, d);
            for (int y = 0; y < d; y++) {
                for (int x = 0; x < d; x++) {
                    double dist = Math.hypot(x + 0.5 - key, y + 0.5 - key);
                    double a = clamp(key - dist + 0.5);
                    tex.set(x, y, alpha(a));
                }
            }
            tex.upload();
            return tex;
        });
    }

    private static Tex ring(int r, int t) {
        long key = ((long) r << 16) | t;
        return RINGS.computeIfAbsent(key, ignored -> {
            int d = r * 2;
            Tex tex = Tex.create("ring" + r + "_" + t, d, d);
            for (int y = 0; y < d; y++) {
                for (int x = 0; x < d; x++) {
                    double dist = Math.hypot(x + 0.5 - r, y + 0.5 - r);
                    double outer = clamp(r - dist + 0.5);
                    double inner = clamp(r - t - dist + 0.5);
                    tex.set(x, y, alpha(outer - inner));
                }
            }
            tex.upload();
            return tex;
        });
    }

    private static Tex shadowTex(int r, int b) {
        long key = ((long) r << 16) | b;
        return SHADOWS.computeIfAbsent(key, ignored -> {
            int c = r + b;
            int d = c * 2;
            Tex tex = Tex.create("shadow" + r + "_" + b, d, d);
            for (int y = 0; y < d; y++) {
                for (int x = 0; x < d; x++) {
                    double px = Math.max(0, Math.abs(x + 0.5 - c) - 0.0);
                    double py = Math.max(0, Math.abs(y + 0.5 - c) - 0.0);
                    double dist = Math.hypot(px, py) - r;
                    double t = clamp((dist + b * 0.15) / (b * 1.15));
                    double a = 1 - t * t * (3 - 2 * t);
                    tex.set(x, y, alpha(a * a));
                }
            }
            tex.upload();
            return tex;
        });
    }

    private static double clamp(double value) {
        return value < 0 ? 0 : value > 1 ? 1 : value;
    }

    private static int alpha(double a) {
        int value = (int) Math.round(clamp(a) * 255);
        return (value << 24) | 0xffffff;
    }
}
