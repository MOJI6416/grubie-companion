package com.grubielauncher.companion.ui;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.Settings;
import com.grubielauncher.companion.core.CompanionState;
import com.grubielauncher.companion.core.Protocol;
import com.grubielauncher.companion.core.ServerSnapshot;
import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.render.Text;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class Panel {
    public enum Mode {
        FRIENDS,
        PLAY,
        VOICE,
        INVITE,
        SCREENSHOT,
        SETTINGS
    }

    private static final float WIDTH = 156f;
    private static final float MARGIN = 6f;
    private static final float PAD = 6f;
    private static final float FOOTER = 22f;
    private static final float MIN_CONTENT = 44f;
    private static final float ROW = 20f;

    private Mode mode;
    private final String target;
    private final Runnable close;
    private float scroll;
    private float maxScroll;
    private float x0;
    private float y0;
    private float h0;
    private float content = MIN_CONTENT;
    private boolean offlineOpen;

    public Panel(Mode mode, String target, Runnable close) {
        this.mode = mode == null ? Mode.FRIENDS : mode;
        this.offlineOpen = this.mode == Mode.SCREENSHOT;
        this.target = target;
        this.close = close;
    }

    public static float occupiedWidth() {
        return WIDTH + MARGIN;
    }

    public boolean contains(float x, float y) {
        return x >= x0 && x < x0 + WIDTH && y >= y0 && y < y0 + h0;
    }

    public void scroll(float delta) {
        scroll = Math.max(0f, Math.min(maxScroll, scroll - delta * 14f));
    }

    public void tick() {
    }

    public void render(Ui ui, float width, float height, float open) {
        Companion companion = Companion.get();
        CompanionState.View view = companion == null ? null : companion.state().view();
        Minecraft mc = Mc.mc();

        ui.canvas.alpha(open);
        ui.canvas.rect(0, 0, width, height, Theme.withAlpha(Theme.BACKDROP, 0.2f));
        ui.canvas.alpha(1f);

        boolean tabbed = mode == Mode.FRIENDS || mode == Mode.PLAY || mode == Mode.VOICE;
        float chrome = PAD + 14f + 6f + (tabbed ? 15f + 7f : 0f) + FOOTER + 4f;
        x0 = width - WIDTH - MARGIN + (1f - open) * (WIDTH + MARGIN * 2);
        y0 = MARGIN;
        h0 = Math.min(height - MARGIN * 2, chrome + Math.max(MIN_CONTENT, content));
        float x = x0;
        float y = y0;

        ui.canvas.shadow(x, y, WIDTH, h0, Theme.RADIUS_LG, 14f, 4f, Theme.SHADOW);
        ui.canvas.round(x, y, WIDTH, h0, Theme.RADIUS_LG, Theme.PANEL);
        ui.canvas.border(x, y, WIDTH, h0, Theme.RADIUS_LG, 0.5f, Theme.BORDER);

        float cy = header(ui, x, y, view);
        if (tabbed) cy = tabs(ui, x + PAD, cy, view);

        float footerH = FOOTER;
        float contentTop = cy;
        float contentBottom = y + h0 - footerH;
        float contentY = contentTop - scroll;
        float end;
        if (mode == Mode.SETTINGS) {
            end = settings(ui, x + PAD, contentY, new Clip(contentTop, contentBottom));
        } else if (view == null || !view.connected()) {
            end = empty(ui, x + PAD, contentY + 18f, "alert", Mc.tr("grubie_companion.panel.offline"), Mc.tr("grubie_companion.panel.offlineHint"));
        } else {
            Clip clip = new Clip(contentTop, contentBottom);
            end = switch (mode) {
                case FRIENDS -> friends(ui, x + PAD, contentY, view, mc, clip, false);
                case INVITE -> friends(ui, x + PAD, contentY, view, mc, clip, true);
                case PLAY -> play(ui, x + PAD, contentY, view, mc, clip);
                case VOICE -> voice(ui, x + PAD, contentY, view, clip);
                case SCREENSHOT -> screenshot(ui, x + PAD, contentY, view, clip);
                case SETTINGS -> contentY;
            };
        }
        content = end + scroll - contentTop;
        maxScroll = Math.max(0f, end + scroll - contentBottom + 4f);
        footer(ui, x, y + h0 - footerH, footerH, mc);
    }

    private record Clip(float top, float bottom) {
        boolean visible(float y, float h) {
            return y >= top - 0.5f && y + h <= bottom + 0.5f;
        }
    }

    private float header(Ui ui, float x, float y, CompanionState.View view) {
        float top = y + PAD;
        float bw = 14f;
        String key = Companion.get() == null ? "G" : Companion.get().panelKeyName();
        float keyX = x + WIDTH - PAD - bw - ui.keycapWidth(key, 8f) - 3f;
        if (mode == Mode.INVITE || mode == Mode.SCREENSHOT || mode == Mode.SETTINGS) {
            ui.button(x + PAD, top, bw, bw, null, "chevron-left", Ui.Style.GHOST, true, () -> {
                mode = Mode.FRIENDS;
                scroll = 0f;
            });
            String title = Mc.tr(mode == Mode.INVITE ? "grubie_companion.panel.inviteTitle" : mode == Mode.SETTINGS ? "grubie_companion.settings.title" : "grubie_companion.panel.screenshotTitle");
            ui.textFit(title, x + PAD + bw + 4f, top + 2.5f, WIDTH - PAD * 2 - bw * 2 - 8f, Theme.TEXT_LG, Text.Weight.SEMIBOLD, Theme.TEXT);
        } else {
            float logo = 14f;
            ui.canvas.round(x + PAD, top, logo, logo, Theme.RADIUS, Theme.PRIMARY);
            ui.textCentered("G", x + PAD + logo / 2f, top + (logo - ui.lineHeight(Theme.TEXT_LG)) / 2f, Theme.TEXT_LG, Text.Weight.SEMIBOLD, 0xFFFFFFFF);
            float tx = x + PAD + logo + 5f;
            ui.text("Grubie", tx, top - 0.5f, Theme.TEXT_LG, Text.Weight.SEMIBOLD, Theme.TEXT);
            String sub = view == null || !view.connected()
                    ? Mc.tr("grubie_companion.panel.noLauncher")
                    : (view.selfName() == null ? "" : view.selfName() + " · ") + Mc.tr("grubie_companion.panel.online");
            ui.textFit(sub, tx, top + ui.lineHeight(Theme.TEXT_LG) - 0.5f, keyX - bw - 6f - tx, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
            ui.button(keyX - bw - 3f, top, bw, bw, null, "sliders", Ui.Style.GHOST, true, () -> {
                mode = Mode.SETTINGS;
                scroll = 0f;
            });
        }
        ui.button(x + WIDTH - PAD - bw, top, bw, bw, null, "x", Ui.Style.GHOST, true, close);
        ui.keycap(key, keyX, top + 3f, 8f);
        return top + 14f + 6f;
    }

    private float settings(Ui ui, float x, float y, Clip clip) {
        float w = WIDTH - PAD * 2;
        float h = 17f;
        for (Settings.Option option : Settings.Option.values()) {
            if (clip.visible(y, h)) {
                boolean on = Settings.on(option);
                boolean hover = ui.hovered(x, y, w, h);
                if (hover) ui.canvas.round(x - 2f, y, w + 4f, h, Theme.RADIUS_SM, Theme.HOVER);
                ui.textFit(Mc.tr("grubie_companion.settings." + option.key), x + 2f, y + (h - ui.lineHeight(Theme.TEXT_SM)) / 2f, w - 24f, Theme.TEXT_SM, Text.Weight.MEDIUM, Theme.TEXT);
                float sw = 14f;
                float sh = 8f;
                float sx = x + w - sw - 2f;
                float sy = y + (h - sh) / 2f;
                float knob = Anim.easeOut(Anim.toward("settings:" + option.key, on ? 1f : 0f, Theme.FAST_MS));
                ui.canvas.round(sx, sy, sw, sh, sh / 2f, on ? Theme.PRIMARY : 0x33FFFFFF);
                ui.canvas.circleFill(sx + sh / 2f + knob * (sw - sh), sy + sh / 2f, sh / 2f - 1f, 0xFFFFFFFF);
                ui.click(x, y, w, h, () -> Settings.toggle(option));
            }
            y += h + 1f;
        }
        y += 4f;
        for (String line : wrap(ui, Mc.tr("grubie_companion.settings.hint"), w - 4f)) {
            if (clip.visible(y, 8f)) ui.text(line, x + 2f, y, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.FAINT);
            y += ui.lineHeight(Theme.TEXT_SM) + 1f;
        }
        return y + 2f;
    }

    private float tabs(Ui ui, float x, float y, CompanionState.View view) {
        float w = WIDTH - PAD * 2;
        float h = 15f;
        ui.canvas.round(x, y, w, h, Theme.RADIUS, Theme.SURFACE);
        ui.canvas.border(x, y, w, h, Theme.RADIUS, 0.5f, 0x0FFFFFFF);
        Mode[] tabs = {Mode.FRIENDS, Mode.PLAY, Mode.VOICE};
        String[] labels = {Mc.tr("grubie_companion.panel.tab.friends"), Mc.tr("grubie_companion.panel.tab.play"), Mc.tr("grubie_companion.panel.tab.voice")};
        float tw = (w - 3f) / 3f;
        int index = mode.ordinal();
        float slide = Anim.toward("panel:tab", index, Theme.NORMAL_MS * 2);
        ui.canvas.round(x + 1.5f + tw * slide, y + 1.5f, tw, h - 3f, Theme.RADIUS_SM, Theme.HOVER);
        for (int i = 0; i < tabs.length; i++) {
            Mode tab = tabs[i];
            float tx = x + 1.5f + tw * i;
            boolean active = mode == tab;
            boolean hover = ui.hovered(tx, y, tw, h);
            int color = active ? Theme.TEXT : hover ? 0xFFD6D7DA : Theme.MUTED;
            String label = labels[i];
            float lw = ui.textWidth(label, Theme.TEXT_SM, Text.Weight.MEDIUM);
            float dot = tab == Mode.VOICE && view != null && view.voice().active() ? 4f : 0f;
            float lx = tx + (tw - lw - dot) / 2f;
            if (dot > 0) {
                ui.canvas.circleFill(lx + 1.25f, y + h / 2f, 1.25f, Theme.SUCCESS);
                lx += dot;
            }
            ui.text(label, lx, y + (h - ui.lineHeight(Theme.TEXT_SM)) / 2f, Theme.TEXT_SM, Text.Weight.MEDIUM, color);
            ui.click(tx, y, tw, h, () -> {
                mode = tab;
                scroll = 0f;
            });
        }
        return y + h + 7f;
    }

    private float friends(Ui ui, float x, float y, CompanionState.View view, Minecraft mc, Clip clip, boolean picking) {
        float w = WIDTH - PAD * 2;
        Companion companion = Companion.get();
        boolean canInvite = companion != null && companion.canInvite(mc);
        if (!picking && !view.invites().isEmpty()) {
            if (clip.visible(y, 8f)) ui.label(Mc.tr("grubie_companion.panel.invites") + " · " + view.invites().size(), x, y);
            y += 9f;
            for (Protocol.ToastItem invite : view.invites().subList(0, Math.min(3, view.invites().size()))) {
                float h = 34f;
                if (clip.visible(y, h)) inviteCard(ui, x, y, w, h, invite);
                y += h + 4f;
            }
            y += 3f;
        }
        if (mode == Mode.INVITE && clip.visible(y, 8f)) {
            String where = companion == null ? "" : companion.inviteTargetName(mc);
            ui.textFit(Mc.tr("grubie_companion.panel.inviteTo", where), x, y, w, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
        }
        if (mode == Mode.INVITE) y += 11f;
        List<Protocol.Friend> online = view.friends().online();
        if (clip.visible(y, 8f)) ui.label(Mc.tr("grubie_companion.panel.onlineCount", online.size()), x, y);
        y += 10f;
        if (online.isEmpty()) {
            if (clip.visible(y, 10f)) ui.text(Mc.tr("grubie_companion.panel.nobodyOnline"), x + 2f, y, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.FAINT);
            y += 12f;
        }
        for (Protocol.Friend friend : online) {
            if (clip.visible(y, ROW)) friendRow(ui, x, y, w, friend, canInvite, picking);
            y += ROW + 1f;
        }
        if (view.friends().offline() > 0) {
            y += 2f;
            float rowH = 11f;
            if (clip.visible(y, rowH)) {
                boolean hover = ui.hovered(x - 2f, y - 1.5f, w + 4f, rowH);
                int color = hover ? Theme.MUTED : Theme.FAINT;
                ui.icon(offlineOpen ? "chevron-down" : "chevron-right", x + 1f, y, 7f, color);
                ui.text(Mc.tr("grubie_companion.panel.offlineCount", view.friends().offline()), x + 10f, y, Theme.TEXT_SM, Text.Weight.REGULAR, color);
                ui.click(x - 2f, y - 1.5f, w + 4f, rowH, () -> offlineOpen = !offlineOpen);
            }
            y += 12f;
            if (offlineOpen) {
                List<Protocol.Friend> offline = view.friends().offlineList();
                if (offline.isEmpty()) {
                    if (clip.visible(y, 10f)) ui.text(Mc.tr("grubie_companion.panel.offlineEmpty"), x + 10f, y, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.FAINT);
                    y += 12f;
                }
                float row = 15f;
                for (Protocol.Friend friend : offline) {
                    if (mode == Mode.SCREENSHOT) {
                        if (clip.visible(y, ROW)) friendRow(ui, x, y, w, friend, false, true);
                        y += ROW + 1f;
                        continue;
                    }
                    if (clip.visible(y, row)) {
                        float size = 11f;
                        ui.canvas.alpha(0.55f);
                        ui.avatar(friend.avatar(), friend.name(), x + 2f, y + (row - size) / 2f, size, 0);
                        ui.canvas.alpha(1f);
                        ui.textFit(friend.name(), x + size + 7f, y + (row - ui.lineHeight(Theme.TEXT_SM)) / 2f, w - size - 9f, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.FAINT);
                    }
                    y += row;
                }
            }
        }
        return y;
    }

    private void inviteCard(Ui ui, float x, float y, float w, float h, Protocol.ToastItem invite) {
        ui.canvas.round(x, y, w, h, Theme.RADIUS, Theme.SURFACE);
        ui.canvas.round(x, y, w, h, Theme.RADIUS, 0x147C5CFF);
        ui.canvas.border(x, y, w, h, Theme.RADIUS, 0.5f, 0x597C5CFF);
        ui.avatar(invite.avatar(), invite.title(), x + 5f, y + 5f, 13f, 0);
        float tx = x + 22f;
        ui.textFit(invite.title(), tx, y + 4f, w - 26f, Theme.TEXT_MD, Text.Weight.MEDIUM, Theme.TEXT);
        ui.textFit(invite.body(), tx, y + 4f + ui.lineHeight(Theme.TEXT_MD), w - 26f, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
        float by = y + h - 14f;
        String accept = Mc.tr("grubie_companion.panel.accept");
        float aw = ui.buttonWidth(accept, null, 11f);
        ui.button(x + 5f, by, aw, 11f, accept, null, Ui.Style.PRIMARY, true, () -> Companion.get().accept(invite.id()));
        String decline = Mc.tr("grubie_companion.panel.decline");
        ui.button(x + 8f + aw, by, ui.buttonWidth(decline, null, 11f), 11f, decline, null, Ui.Style.GHOST, true, () -> Companion.get().decline(invite.id()));
        String key = Companion.get() == null ? "F7" : Companion.get().acceptKeyName();
        ui.keycap(key, x + w - 5f - ui.keycapWidth(key, 8f), by + 1.5f, 8f);
    }

    private void friendRow(Ui ui, float x, float y, float w, Protocol.Friend friend, boolean canInvite, boolean picking) {
        boolean hover = ui.hovered(x - 2f, y, w + 4f, ROW);
        float t = Anim.toward("row:" + friend.id(), hover ? 1f : 0f, Theme.FAST_MS);
        if (t > 0.01f) ui.canvas.round(x - 2f, y, w + 4f, ROW, Theme.RADIUS, Theme.withAlpha(Theme.RAISED, t));
        float size = 13f;
        ui.avatar(friend.avatar(), friend.name(), x + 1f, y + (ROW - size) / 2f, size, 0);
        int dot = switch (friend.status()) {
            case "away" -> Theme.WARNING;
            case "busy" -> Theme.DANGER;
            case "offline" -> 0xFF6B6E75;
            default -> Theme.SUCCESS;
        };
        ui.statusDot(x + 1f + size - 0.5f, y + (ROW + size) / 2f - 0.5f, 1.75f, dot, 0xFF16171A);
        float tx = x + size + 6f;
        Companion companion = Companion.get();
        boolean invited = companion != null && companion.wasInvited(friend.id());
        String action = null;
        String icon = null;
        Runnable run = null;
        if (mode == Mode.SCREENSHOT) {
            boolean sent = companion != null && companion.wasSentScreenshot(target, friend.id());
            if (sent) {
                action = Mc.tr("grubie_companion.panel.sent");
            } else {
                action = Mc.tr("grubie_companion.panel.send");
                icon = "send";
                run = () -> Companion.get().sendScreenshot(target, friend.id());
            }
        } else if (canInvite && !invited) {
            action = Mc.tr("grubie_companion.panel.invite");
            icon = "user-plus";
            run = () -> Companion.get().invite(friend.id());
        } else if (invited) {
            action = Mc.tr("grubie_companion.panel.invited");
        } else if (friend.joinable() && !picking) {
            action = Mc.tr("grubie_companion.panel.join");
            run = () -> Companion.get().join(friend.id());
        }
        float actionW = 0f;
        if (action != null && run == null) {
            float lw = ui.textWidth(action, Theme.TEXT_SM, Text.Weight.MEDIUM);
            ui.icon("check", x + w - lw - 9f, y + (ROW - 6f) / 2f, 6f, Theme.SUCCESS);
            ui.text(action, x + w - lw - 2f, y + (ROW - ui.lineHeight(Theme.TEXT_SM)) / 2f, Theme.TEXT_SM, Text.Weight.MEDIUM, Theme.SUCCESS);
            actionW = lw + 10f;
        } else if (action != null) {
            float bh = 12f;
            float bw = ui.buttonWidth(action, icon, bh);
            if (hover || picking || mode == Mode.SCREENSHOT) {
                ui.button(x + w - bw, y + (ROW - bh) / 2f, bw, bh, action, icon, Ui.Style.SECONDARY, true, run);
            } else {
                ui.icon(icon == null ? "chevron-right" : icon, x + w - 9f, y + (ROW - 7f) / 2f, 7f, Theme.FAINT);
            }
            actionW = bw + 3f;
        }
        float textW = w - (tx - x) - actionW - 2f;
        ui.textFit(friend.name(), tx, y + 3f, textW, Theme.TEXT_MD, Text.Weight.MEDIUM, Theme.TEXT);
        String activity = friend.activity() == null || friend.activity().isEmpty()
                ? Mc.tr("grubie_companion.status." + friend.status())
                : friend.activity();
        ui.textFit(activity, tx, y + 3f + ui.lineHeight(Theme.TEXT_MD), textW, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
    }

    private float play(Ui ui, float x, float y, CompanionState.View view, Minecraft mc, Clip clip) {
        float w = WIDTH - PAD * 2;
        Companion companion = Companion.get();
        Protocol.HostState host = view.host();
        if (Mc.isSingleplayer(mc)) {
            if (!host.active()) {
                float h = 56f;
                if (clip.visible(y, h)) {
                    ui.canvas.round(x, y, w, h, Theme.RADIUS, Theme.SURFACE);
                    ui.canvas.round(x + 6f, y + 6f, 16f, 16f, Theme.RADIUS, Theme.PRIMARY_SOFT);
                    ui.icon("radio", x + 9.5f, y + 9.5f, 9f, 0xFFB3A0FF);
                    ui.textFit(Mc.tr("grubie_companion.play.openTitle"), x + 27f, y + 6f, w - 32f, Theme.TEXT_MD, Text.Weight.SEMIBOLD, Theme.TEXT);
                    ui.textFit(Mc.tr("grubie_companion.play.openHint"), x + 27f, y + 6f + ui.lineHeight(Theme.TEXT_MD), w - 32f, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
                    boolean busy = companion != null && companion.sharePending();
                    String label = Mc.tr(busy ? "grubie_companion.play.opening" : "grubie_companion.play.open");
                    ui.button(x + 6f, y + h - 20f, w - 12f, 14f, label, "radio", Ui.Style.PRIMARY, !busy, () -> Companion.get().share());
                }
                return y + h + 4f;
            }
            return hosting(ui, x, y, w, host, clip);
        }
        if (Mc.currentServerAddress(mc) != null) {
            float h = 40f;
            if (clip.visible(y, h)) {
                ui.canvas.round(x, y, w, h, Theme.RADIUS, Theme.SURFACE);
                ui.icon("globe", x + 6f, y + 6f, 9f, Theme.MUTED);
                String name = Mc.currentServerName(mc);
                ui.textFit(name == null ? Mc.currentServerAddress(mc) : name, x + 19f, y + 5.5f, w - 24f, Theme.TEXT_MD, Text.Weight.SEMIBOLD, Theme.TEXT);
                ui.textFit(Mc.currentServerAddress(mc), x + 19f, y + 5.5f + ui.lineHeight(Theme.TEXT_MD), w - 24f, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
                ui.button(x + 6f, y + h - 18f, w - 12f, 13f, Mc.tr("grubie_companion.play.inviteFriends"), "user-plus", Ui.Style.PRIMARY, true, () -> {
                    mode = Mode.INVITE;
                    scroll = 0f;
                });
            }
            return y + h + 4f;
        }
        return empty(ui, x, y + 14f, "gamepad", Mc.tr("grubie_companion.play.noWorld"), Mc.tr("grubie_companion.play.noWorldHint"));
    }

    private float hosting(Ui ui, float x, float y, float w, Protocol.HostState host, Clip clip) {
        String world = Mc.worldName(Mc.mc());
        String title = host.title() != null ? host.title() : world != null ? world : Mc.tr("grubie_companion.play.yourWorld");
        if (clip.visible(y, 22f)) {
            ui.textFit(Mc.tr("grubie_companion.play.sharedTitle", title), x, y, w, Theme.TEXT_LG, Text.Weight.SEMIBOLD, Theme.TEXT);
            ui.canvas.circleFill(x + 1.5f, y + ui.lineHeight(Theme.TEXT_LG) + 3.5f, 1.5f, Theme.SUCCESS);
            String visibility = Mc.tr("grubie_companion.play.visibility." + (host.visibility() == null ? "friends" : host.visibility()));
            ui.textFit(visibility, x + 5f, y + ui.lineHeight(Theme.TEXT_LG) + 0.5f, w - 5f, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
        }
        y += 24f;
        int max = host.max();
        if (clip.visible(y, 8f)) ui.label(max > 0 ? Mc.tr("grubie_companion.play.guestsOf", host.guests().size(), max) : Mc.tr("grubie_companion.play.guests", host.guests().size()), x, y);
        y += 10f;
        if (host.guests().isEmpty() && clip.visible(y, 10f)) {
            ui.text(Mc.tr("grubie_companion.play.noGuests"), x + 2f, y, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.FAINT);
        }
        if (host.guests().isEmpty()) y += 12f;
        for (Protocol.Guest guest : host.guests()) {
            float h = 22f;
            if (clip.visible(y, h)) {
                ui.canvas.round(x, y, w, h, Theme.RADIUS, Theme.SURFACE);
                ui.avatar(guest.avatar(), guest.name(), x + 4f, y + 4.5f, 13f, 0);
                ui.textFit(guest.name(), x + 21f, y + 3f, w - 70f, Theme.TEXT_MD, Text.Weight.MEDIUM, Theme.TEXT);
                boolean direct = "direct".equals(guest.transport());
                String transport = Mc.tr("grubie_companion.transport." + (direct ? "direct" : "turn".equals(guest.transport()) ? "turn" : "relay"));
                String line = guest.ping() >= 0 ? transport + " · " + guest.ping() + " ms" : transport;
                ui.icon(direct ? "signal" : "server", x + 21f, y + 4f + ui.lineHeight(Theme.TEXT_MD), 5.5f, direct ? Theme.SUCCESS : Theme.MUTED);
                ui.textFit(line, x + 28f, y + 3f + ui.lineHeight(Theme.TEXT_MD), w - 77f, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
                String kick = Mc.tr("grubie_companion.panel.kick");
                float bw = ui.buttonWidth(kick, null, 12f);
                ui.button(x + w - bw - 4f, y + 5f, bw, 12f, kick, null, Ui.Style.DANGER, true, () -> Companion.get().kick(guest.key()));
            }
            y += h + 3f;
        }
        for (String invited : host.invited()) {
            if (clip.visible(y, 14f)) {
                ui.canvas.border(x, y, w, 14f, Theme.RADIUS, 0.5f, 0x1FFFFFFF);
                ui.textFit(Mc.tr("grubie_companion.play.invitedWaiting", invited), x + 5f, y + (14f - ui.lineHeight(Theme.TEXT_SM)) / 2f, w - 10f, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
            }
            y += 16f;
        }
        y += 4f;
        if (clip.visible(y, 14f)) {
            String invite = Mc.tr("grubie_companion.play.inviteFriend");
            float iw = ui.buttonWidth(invite, "user-plus", 14f);
            ui.button(x, y, iw, 14f, invite, "user-plus", Ui.Style.PRIMARY, true, () -> {
                mode = Mode.INVITE;
                scroll = 0f;
            });
            String stop = Mc.tr("grubie_companion.play.stop");
            float sw = ui.buttonWidth(stop, null, 14f);
            ui.button(x + w - sw, y, sw, 14f, stop, null, Ui.Style.DANGER, true, () -> Companion.get().unshare());
        }
        return y + 18f;
    }

    private float voice(Ui ui, float x, float y, CompanionState.View view, Clip clip) {
        float w = WIDTH - PAD * 2;
        Protocol.VoiceState voice = view.voice();
        if (!voice.active()) {
            return empty(ui, x, y + 14f, "mic", Mc.tr("grubie_companion.voice.none"), Mc.tr("grubie_companion.voice.noneHint"));
        }
        if (clip.visible(y, 12f)) {
            ui.canvas.circleFill(x + 2f, y + ui.lineHeight(Theme.TEXT_LG) / 2f, 2f, Theme.SUCCESS);
            ui.textFit(voice.channel() == null ? Mc.tr("grubie_companion.voice.channel") : voice.channel(), x + 7f, y, w - 7f, Theme.TEXT_LG, Text.Weight.SEMIBOLD, Theme.TEXT);
        }
        y += 14f;
        float bw = (w - 4f) / 2f;
        if (clip.visible(y, 14f)) {
            ui.button(x, y, bw, 14f, Mc.tr(voice.muted() ? "grubie_companion.voice.unmute" : "grubie_companion.voice.mute"), voice.muted() ? "mic-off" : "mic", voice.muted() ? Ui.Style.DANGER : Ui.Style.SECONDARY, true, () -> Companion.get().voice(voice.muted() ? "unmute" : "mute"));
            ui.button(x + bw + 4f, y, bw, 14f, Mc.tr(voice.deafened() ? "grubie_companion.voice.undeafen" : "grubie_companion.voice.deafen"), voice.deafened() ? "headphones-off" : "headphones", voice.deafened() ? Ui.Style.DANGER : Ui.Style.SECONDARY, true, () -> Companion.get().voice(voice.deafened() ? "undeafen" : "deafen"));
        }
        y += 18f;
        if (voice.ptt() && clip.visible(y, 10f)) {
            String key = voice.pttKey() == null ? "?" : voice.pttKey();
            String hint = Mc.tr("grubie_companion.voice.pttHint");
            ui.text(hint, x, y, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
            ui.keycap(key, x + ui.textWidth(hint, Theme.TEXT_SM, Text.Weight.REGULAR) + 3f, y - 1f, 8f);
        }
        if (voice.ptt()) y += 12f;
        if (clip.visible(y, 8f)) ui.label(Mc.tr("grubie_companion.voice.members", voice.members().size()), x, y);
        y += 10f;
        for (Protocol.VoiceMember member : voice.members()) {
            if (clip.visible(y, ROW)) {
                float speaking = Anim.toward("panelvoice:" + member.id(), member.speaking() ? 1f : 0f, Theme.FAST_MS);
                ui.avatar(member.avatar(), member.name(), x + 1f, y + 3.5f, 13f, 0);
                if (speaking > 0.01f) ui.canvas.border(x - 0.5f, y + 2f, 16f, 16f, 4.5f, 1f, Theme.withAlpha(Theme.SUCCESS, speaking));
                ui.textFit(member.name(), x + 19f, y + (ROW - ui.lineHeight(Theme.TEXT_MD)) / 2f, w - 32f, Theme.TEXT_MD, Text.Weight.MEDIUM, member.muted() ? Theme.MUTED : Theme.TEXT);
                if (member.muted()) ui.icon("mic-off", x + w - 9f, y + (ROW - 7f) / 2f, 7f, Theme.DANGER);
            }
            y += ROW + 1f;
        }
        return y;
    }

    private float screenshot(Ui ui, float x, float y, CompanionState.View view, Clip clip) {
        float w = WIDTH - PAD * 2;
        Companion companion = Companion.get();
        float th = w * 9f / 16f;
        if (clip.visible(y, th)) {
            ui.canvas.round(x, y, w, th, Theme.RADIUS, Theme.SURFACE);
            if (companion != null) companion.drawScreenshotThumb(ui, target, x, y, w, th);
        }
        y += th + 6f;
        Clip inner = clip;
        return friends(ui, x, y, view, Mc.mc(), inner, true);
    }

    private float empty(Ui ui, float x, float y, String icon, String title, String hint) {
        float w = WIDTH - PAD * 2;
        float size = 20f;
        ui.canvas.round(x + (w - size - 8f) / 2f, y, size + 8f, size + 8f, Theme.RADIUS_LG, Theme.SURFACE);
        ui.icon(icon, x + (w - size) / 2f + 2f, y + 6f, size - 4f, Theme.MUTED);
        y += size + 14f;
        ui.textCentered(Text.fit(title, w * ui.canvas.scale, ui.px(Theme.TEXT_MD), Text.Weight.SEMIBOLD), x + w / 2f, y, Theme.TEXT_MD, Text.Weight.SEMIBOLD, Theme.TEXT);
        y += ui.lineHeight(Theme.TEXT_MD) + 2f;
        for (String line : wrap(ui, hint, w - 10f)) {
            ui.textCentered(line, x + w / 2f, y, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.MUTED);
            y += ui.lineHeight(Theme.TEXT_SM);
        }
        return y;
    }

    private void footer(Ui ui, float x, float y, float h, Minecraft mc) {
        ui.canvas.rect(x + 0.5f, y, WIDTH - 1f, 0.5f, 0x12FFFFFF);
        String icon;
        String caption;
        String value;
        String server = Mc.currentServerAddress(mc);
        if (Mc.isSingleplayer(mc)) {
            icon = "gamepad";
            caption = Mc.tr("grubie_companion.footer.world");
            value = Mc.worldName(mc);
        } else if (server != null) {
            Companion companion = Companion.get();
            Protocol.ConnectRequest place = companion == null ? null : companion.state().place();
            boolean known = place != null && server.equals(ServerSnapshot.normalizeAddress(place.address()));
            boolean world = known ? place.world() : server.endsWith(".join.grubielauncher.com");
            String name = known ? place.name() : Mc.currentServerName(mc);
            boolean named = name != null && !name.isBlank() && !name.equals(server) && !name.equals(place == null ? null : place.address()) && !name.equals(Mc.tr("selectServer.defaultName"));
            icon = world ? "gamepad" : "globe";
            caption = Mc.tr(world ? "grubie_companion.footer.world" : "grubie_companion.footer.server");
            value = named ? name : server;
        } else {
            icon = "gamepad";
            caption = Mc.tr("grubie_companion.footer.menu");
            value = "";
        }
        ui.icon(icon, x + PAD, y + (h - 8f) / 2f, 8f, Theme.MUTED);
        ui.text(caption, x + PAD + 12f, y + 5f, Theme.TEXT_SM, Text.Weight.REGULAR, Theme.FAINT);
        if (value != null && !value.isEmpty()) {
            ui.textFit(value, x + PAD + 12f, y + 5f + ui.lineHeight(Theme.TEXT_SM), WIDTH - PAD * 2 - 14f, Theme.TEXT_SM, Text.Weight.MEDIUM, Theme.TEXT);
        }
    }

    private static List<String> wrap(Ui ui, String text, float width) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        if (text == null) return lines;
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (ui.textWidth(candidate, Theme.TEXT_SM, Text.Weight.REGULAR) > width && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }
}
