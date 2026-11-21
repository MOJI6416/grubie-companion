package com.grubielauncher.companion.ui;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.Settings;
import com.grubielauncher.companion.core.Protocol;
import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.render.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Toasts {
    private static final int MAX_VISIBLE = 4;
    private static final float WIDTH = 136f;
    private static final float MARGIN = 6f;
    private static final float PAD = 6f;

    private static final class Entry {
        final String id;
        final String kind;
        final String title;
        final String body;
        final String meta;
        final String avatar;
        final boolean actionable;
        final long created;
        final long duration;
        long leaving = -1;

        Entry(String id, String kind, String title, String body, String meta, String avatar, boolean actionable, long duration) {
            this.id = id;
            this.kind = kind;
            this.title = title;
            this.body = body;
            this.meta = meta;
            this.avatar = avatar;
            this.actionable = actionable;
            this.created = Anim.now();
            this.duration = duration;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private Toasts() {
    }

    public static void push(Protocol.ToastItem item) {
        if ("screenshot".equals(item.kind()) && !Settings.on(Settings.Option.SCREENSHOTS)) return;
        if ("achievement".equals(item.kind()) && !Settings.on(Settings.Option.ACHIEVEMENTS)) return;
        if ("message".equals(item.kind()) && !Settings.on(Settings.Option.MESSAGES)) return;
        long duration = switch (item.kind()) {
            case "invite", "call" -> 15_000L;
            case "screenshot" -> 10_000L;
            case "achievement" -> 8_000L;
            default -> 7_000L;
        };
        if ("screenshot".equals(item.kind())) {
            add(new Entry(item.id(), item.kind(), Mc.tr("grubie_companion.toast.screenshotSaved"), item.title(), null, null, false, duration));
            return;
        }
        add(new Entry(item.id(), item.kind(), item.title(), item.body(), item.meta(), item.avatar(), item.actionable(), duration));
    }

    public static void local(String id, String kind, String title, String body) {
        add(new Entry(id, kind, title, body, null, null, false, 6_000L));
    }

    public static void dismiss(String id) {
        for (Entry entry : ENTRIES) {
            if (entry.id.equals(id) && entry.leaving < 0) entry.leaving = Anim.now();
        }
    }

    public static String latestInvite() {
        for (int i = ENTRIES.size() - 1; i >= 0; i--) {
            Entry entry = ENTRIES.get(i);
            if (entry.actionable && entry.leaving < 0) return entry.id;
        }
        return null;
    }

    public static String latestScreenshot() {
        for (int i = ENTRIES.size() - 1; i >= 0; i--) {
            Entry entry = ENTRIES.get(i);
            if ("screenshot".equals(entry.kind) && entry.leaving < 0) return entry.id;
        }
        return null;
    }

    public static void clear() {
        ENTRIES.clear();
    }

    private static void add(Entry entry) {
        ENTRIES.removeIf(existing -> existing.id.equals(entry.id));
        ENTRIES.add(entry);
        int alive = 0;
        for (int i = ENTRIES.size() - 1; i >= 0; i--) {
            Entry e = ENTRIES.get(i);
            if (e.leaving >= 0) continue;
            alive++;
            if (alive > MAX_VISIBLE) e.leaving = Anim.now();
        }
    }

    public static void render(Ui ui, float screenWidth) {
        long now = Anim.now();
        Iterator<Entry> iterator = ENTRIES.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.leaving < 0 && now - entry.created > entry.duration) entry.leaving = now;
            if (entry.leaving >= 0 && now - entry.leaving > Theme.SLOW_MS) iterator.remove();
        }
        float y = MARGIN;
        for (int i = ENTRIES.size() - 1; i >= 0; i--) {
            Entry entry = ENTRIES.get(i);
            float enter = Anim.easeOut(Anim.progress(entry.created, Theme.SLOW_MS));
            float exit = entry.leaving < 0 ? 0f : Anim.easeOut(Anim.progress(entry.leaving, Theme.SLOW_MS));
            float visibility = enter * (1f - exit);
            float height = height(ui, entry);
            float x = screenWidth - WIDTH - MARGIN + (1f - enter) * 24f + exit * 12f;
            ui.canvas.alpha(visibility);
            draw(ui, entry, x, y, height, now);
            ui.canvas.alpha(1f);
            y += (height + 4f) * (entry.leaving < 0 ? 1f : 1f - exit);
        }
    }

    private static float height(Ui ui, Entry entry) {
        float h = PAD * 2 + ui.lineHeight(Theme.TEXT_MD);
        if (entry.body != null && !entry.body.isEmpty()) h += ui.lineHeight(Theme.TEXT_SM) + 1f;
        if (entry.meta != null && !entry.meta.isEmpty()) h += ui.lineHeight(Theme.TEXT_SM) + 2f;
        if (entry.actionable || "screenshot".equals(entry.kind)) h += 13f;
        return Math.max(h, PAD * 2 + 16f);
    }

    private static void draw(Ui ui, Entry entry, float x, float y, float h, long now) {
        boolean invite = entry.actionable;
        ui.canvas.shadow(x, y, WIDTH, h, Theme.RADIUS_LG, 10f, 3f, Theme.SHADOW);
        ui.canvas.round(x, y, WIDTH, h, Theme.RADIUS_LG, Theme.PANEL);
        ui.canvas.border(x, y, WIDTH, h, Theme.RADIUS_LG, 0.5f, invite ? 0x737C5CFF : Theme.BORDER);
        float iconSize = 16f;
        float ix = x + PAD;
        float iy = y + PAD;
        switch (entry.kind) {
            case "achievement" -> badge(ui, ix, iy, iconSize, "trophy", Theme.WARNING);
            case "screenshot" -> badge(ui, ix, iy, iconSize, "camera", Theme.INFO);
            case "system" -> badge(ui, ix, iy, iconSize, "radio", Theme.PRIMARY);
            case "error" -> badge(ui, ix, iy, iconSize, "alert", Theme.DANGER);
            default -> ui.avatar(entry.avatar, entry.title, ix, iy, iconSize, 0);
        }
        float tx = ix + iconSize + 5f;
        float tw = x + WIDTH - PAD - tx;
        float ty = y + PAD;
        ui.textFit(entry.title, tx, ty, tw, Theme.TEXT_MD, Text.Weight.SEMIBOLD, Theme.TEXT);
        ty += ui.lineHeight(Theme.TEXT_MD) + 1f;
        if (entry.body != null && !entry.body.isEmpty()) {
            ui.textFit(entry.body, tx, ty, tw, Theme.TEXT_SM, Text.Weight.REGULAR, 0xFFC9CBCF);
            ty += ui.lineHeight(Theme.TEXT_SM);
        }
        if (entry.meta != null && !entry.meta.isEmpty()) {
            ty += 2f;
            ui.textFit(entry.meta, tx, ty, tw, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.FAINT);
            ty += ui.lineHeight(Theme.TEXT_SM);
        }
        if (invite) {
            ty += 3f;
            Companion companion = Companion.get();
            hint(ui, tx, ty, companion == null ? "F7" : companion.acceptKeyName(), Mc.tr("grubie_companion.toast.accept"), true);
            float progress = 1f - Math.min(1f, (now - entry.created) / (float) entry.duration);
            ui.canvas.round(x + PAD, y + h - 3f, WIDTH - PAD * 2, 1.25f, 0.6f, 0x14FFFFFF);
            ui.canvas.round(x + PAD, y + h - 3f, (WIDTH - PAD * 2) * progress, 1.25f, 0.6f, Theme.PRIMARY);
        } else if ("screenshot".equals(entry.kind)) {
            ty += 3f;
            Companion companion = Companion.get();
            hint(ui, tx, ty, companion == null ? "G" : companion.panelKeyName(), Mc.tr("grubie_companion.toast.sendScreenshot"), false);
        }
    }

    private static void hint(Ui ui, float x, float y, String key, String label, boolean primary) {
        float h = 9f;
        float labelW = ui.textWidth(label, Theme.TEXT_SM, Text.Weight.MEDIUM);
        float keyW = ui.keycapWidth(key, h - 3f);
        float w = labelW + keyW + 10f;
        ui.canvas.round(x, y, w, h, 3f, primary ? Theme.PRIMARY : Theme.RAISED);
        ui.text(label, x + 4f, y + (h - ui.lineHeight(Theme.TEXT_SM)) / 2f, Theme.TEXT_SM, Text.Weight.MEDIUM, 0xFFFFFFFF);
        ui.keycap(key, x + w - keyW - 2f, y + 1.5f, h - 3f);
    }

    private static void badge(Ui ui, float x, float y, float size, String icon, int color) {
        ui.canvas.round(x, y, size, size, Theme.RADIUS, Theme.withAlpha(color, 0.16f));
        ui.icon(icon, x + size * 0.22f, y + size * 0.22f, size * 0.56f, color);
    }
}
