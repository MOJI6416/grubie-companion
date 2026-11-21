package com.grubielauncher.companion.ui;

import com.grubielauncher.companion.core.Protocol;

public final class VoiceHud {
    private static final int MAX_MEMBERS = 8;

    private VoiceHud() {
    }

    public static void render(Ui ui, Protocol.VoiceState voice, boolean transmitting) {
        if (!voice.active() || voice.members().isEmpty()) return;
        float size = 12f;
        float gap = 4f;
        int count = Math.min(MAX_MEMBERS, voice.members().size());
        float w = count * size + (count - 1) * gap + 6f;
        float x = 6f;
        float y = 6f;
        float shown = Anim.toward("voicehud", 1f, Theme.NORMAL_MS);
        ui.canvas.alpha(shown);
        ui.canvas.round(x, y, w, size + 6f, Theme.RADIUS_LG, 0xB3161719);
        ui.canvas.border(x, y, w, size + 6f, Theme.RADIUS_LG, 0.5f, Theme.BORDER);
        float cx = x + 3f;
        for (int i = 0; i < count; i++) {
            Protocol.VoiceMember member = voice.members().get(i);
            float speaking = Anim.toward("voice:" + member.id(), member.speaking() ? 1f : 0f, Theme.FAST_MS);
            ui.canvas.alpha(shown * (member.muted() ? 0.45f : 0.6f + 0.4f * speaking));
            ui.avatar(member.avatar(), member.name(), cx, y + 3f, size, 0);
            ui.canvas.alpha(shown);
            if (speaking > 0.01f) ui.canvas.border(cx - 1.25f, y + 1.75f, size + 2.5f, size + 2.5f, 4f, 1f, Theme.withAlpha(Theme.SUCCESS, speaking));
            if (member.muted()) {
                ui.canvas.circleFill(cx + size - 1f, y + size + 2f, 3f, 0xFF16171A);
                ui.icon("mic-off", cx + size - 3.5f, y + size - 0.5f, 5f, Theme.DANGER);
            }
            cx += size + gap;
        }
        if (voice.muted()) {
            ui.canvas.round(x + w + 3f, y, size + 6f, size + 6f, Theme.RADIUS_LG, 0xB3161719);
            ui.icon("mic-off", x + w + 6.5f, y + 3.5f, size - 1f, Theme.DANGER);
        } else if (voice.ptt() && transmitting) {
            ui.canvas.round(x + w + 3f, y, size + 6f, size + 6f, Theme.RADIUS_LG, Theme.withAlpha(Theme.SUCCESS, 0.25f));
            ui.icon("mic", x + w + 6.5f, y + 3.5f, size - 1f, Theme.SUCCESS);
        }
        ui.canvas.alpha(1f);
    }
}
