package com.grubielauncher.companion.ui;

import com.grubielauncher.companion.render.Canvas;
import com.grubielauncher.companion.render.Icons;
import com.grubielauncher.companion.render.Text;

import java.util.ArrayList;
import java.util.List;

public final class Ui {
    public enum Style {
        PRIMARY,
        SECONDARY,
        GHOST,
        DANGER
    }

    private record Region(float x, float y, float w, float h, Runnable action) {
    }

    public final Canvas canvas;
    public final float mouseX;
    public final float mouseY;
    private final List<Region> regions = new ArrayList<>();
    private final boolean interactive;

    public Ui(Canvas canvas, float mouseX, float mouseY, boolean interactive) {
        this.canvas = canvas;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.interactive = interactive;
    }

    public boolean hovered(float x, float y, float w, float h) {
        return interactive && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    public void click(float x, float y, float w, float h, Runnable action) {
        if (action != null) regions.add(new Region(x, y, w, h, action));
    }

    public boolean press(float x, float y) {
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region r = regions.get(i);
            if (x >= r.x && x < r.x + r.w && y >= r.y && y < r.y + r.h) {
                r.action.run();
                return true;
            }
        }
        return false;
    }

    public int px(float size) {
        return Math.max(Theme.MIN_TEXT_PX, Math.round(size * canvas.scale));
    }

    public float textWidth(String text, float size, Text.Weight weight) {
        return Text.width(text, px(size), weight) / canvas.scale;
    }

    public float lineHeight(float size) {
        return Text.lineHeight(px(size)) / canvas.scale;
    }

    public void text(String text, float x, float y, float size, Text.Weight weight, int color) {
        Text.draw(canvas.gfx, text, canvas.px(x), canvas.px(y), px(size), weight, color);
    }

    public void textFit(String text, float x, float y, float maxWidth, float size, Text.Weight weight, int color) {
        String fitted = Text.fit(text, maxWidth * canvas.scale, px(size), weight);
        text(fitted, x, y, size, weight, color);
    }

    public void textCentered(String text, float cx, float y, float size, Text.Weight weight, int color) {
        text(text, cx - textWidth(text, size, weight) / 2f, y, size, weight, color);
    }

    public void icon(String name, float x, float y, float size, int color) {
        Icons.draw(canvas, name, x, y, size, color);
    }

    public float button(float x, float y, float w, float h, String label, String icon, Style style, boolean enabled, Runnable action) {
        boolean hover = enabled && hovered(x, y, w, h);
        String key = "btn:" + Math.round(x) + ":" + Math.round(y) + ":" + label;
        float t = Anim.toward(key, hover ? 1f : 0f, Theme.FAST_MS);
        int bg;
        int fg;
        switch (style) {
            case PRIMARY -> {
                bg = Theme.mix(Theme.PRIMARY, Theme.PRIMARY_HOVER, t);
                fg = 0xFFFFFFFF;
            }
            case DANGER -> {
                bg = Theme.mix(0x00EF6B7C, 0x26EF6B7C, t);
                fg = Theme.DANGER;
            }
            case GHOST -> {
                bg = Theme.mix(0x00FFFFFF, 0x14FFFFFF, t);
                fg = Theme.mix(Theme.MUTED, Theme.TEXT, t);
            }
            default -> {
                bg = Theme.mix(Theme.RAISED, Theme.HOVER, t);
                fg = Theme.TEXT;
            }
        }
        if (!enabled) {
            bg = Theme.withAlpha(bg, 0.5f);
            fg = Theme.withAlpha(fg, 0.5f);
        }
        if (style == Style.PRIMARY && enabled) canvas.shadow(x, y, w, h, Theme.RADIUS, 4f, 1.5f, Theme.withAlpha(Theme.PRIMARY, 0.35f + 0.15f * t));
        canvas.round(x, y, w, h, Theme.RADIUS, bg);
        if (style == Style.SECONDARY) canvas.border(x, y, w, h, Theme.RADIUS, 0.5f, Theme.BORDER);
        float size = Theme.TEXT_MD;
        float labelW = label == null ? 0 : textWidth(label, size, Text.Weight.MEDIUM);
        float iconSize = h * 0.46f;
        float gap = label == null || icon == null ? 0 : 3f;
        float contentW = labelW + (icon == null ? 0 : iconSize) + gap;
        float cx = x + (w - contentW) / 2f;
        if (icon != null) {
            icon(icon, cx, y + (h - iconSize) / 2f, iconSize, fg);
            cx += iconSize + gap;
        }
        if (label != null) text(label, cx, y + (h - lineHeight(size)) / 2f, size, Text.Weight.MEDIUM, fg);
        if (enabled) click(x, y, w, h, action);
        return w;
    }

    public float buttonWidth(String label, String icon, float h) {
        float labelW = label == null ? 0 : textWidth(label, Theme.TEXT_MD, Text.Weight.MEDIUM);
        float iconSize = h * 0.46f;
        float gap = label == null || icon == null ? 0 : 3f;
        float padding = label == null ? (h - iconSize) : 12f;
        return labelW + (icon == null ? 0 : iconSize) + gap + padding;
    }

    public void keycap(String key, float x, float y, float h) {
        float size = Theme.TEXT_SM;
        float w = Math.max(h, textWidth(key, size, Text.Weight.MEDIUM) + 4f);
        canvas.round(x, y, w, h, 2.5f, 0x14FFFFFF);
        canvas.border(x, y, w, h, 2.5f, 0.5f, 0x17FFFFFF);
        textCentered(key, x + w / 2f, y + (h - lineHeight(size)) / 2f, size, Text.Weight.MEDIUM, 0xFFD6D7DA);
    }

    public float keycapWidth(String key, float h) {
        return Math.max(h, textWidth(key, Theme.TEXT_SM, Text.Weight.MEDIUM) + 4f);
    }

    public void label(String text, float x, float y) {
        text(text.toUpperCase(), x, y, Theme.TEXT_SM, Text.Weight.MEDIUM, Theme.FAINT);
    }

    public void avatar(String id, String name, float x, float y, float size, int ring) {
        Avatars.draw(this, id, name, x, y, size);
        if (ring != 0) canvas.border(x - 1.5f, y - 1.5f, size + 3f, size + 3f, size * 0.25f + 1.5f, 1f, ring);
    }

    public void statusDot(float cx, float cy, float r, int color, int outline) {
        canvas.circleFill(cx, cy, r + 1f, outline);
        canvas.circleFill(cx, cy, r, color);
    }
}
