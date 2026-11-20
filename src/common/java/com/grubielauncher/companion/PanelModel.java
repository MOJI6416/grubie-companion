package com.grubielauncher.companion;

import com.grubielauncher.companion.core.CompanionState;
import com.grubielauncher.companion.core.Protocol;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class PanelModel {
    public static final int MAX_INVITES = 3;
    public static final int MAX_GUESTS = 6;
    public static final int MUTED = 0xFFA0A0A0;
    public static final int HEADING = 0xFFFFFFFF;
    public static final int TEXT = 0xFFE0E0E0;

    public enum Action {
        ACCEPT,
        KICK
    }

    public record Row(Component text, int color, boolean heading, Action action, String target) {
    }

    private PanelModel() {
    }

    public static List<Row> rows(CompanionState.View view) {
        List<Row> rows = new ArrayList<>();
        if (!view.connected()) {
            rows.add(new Row(Component.translatable("grubie_companion.panel.offline"), MUTED, false, null, null));
            return rows;
        }

        rows.add(new Row(Component.translatable("grubie_companion.panel.invites"), HEADING, true, null, null));
        if (view.invites().isEmpty()) {
            rows.add(new Row(Component.translatable("grubie_companion.panel.noInvites"), MUTED, false, null, null));
        } else {
            for (Protocol.ToastItem invite : view.invites().subList(0, Math.min(MAX_INVITES, view.invites().size()))) {
                Component text = invite.body().isEmpty()
                        ? Component.literal(invite.title())
                        : Component.literal(invite.title() + " · " + invite.body());
                rows.add(new Row(text, TEXT, false, Action.ACCEPT, invite.id()));
            }
        }

        Protocol.HostState host = view.host();
        rows.add(new Row(Component.translatable("grubie_companion.panel.host"), HEADING, true, null, null));
        if (!host.active()) {
            rows.add(new Row(Component.translatable("grubie_companion.panel.notHosting"), MUTED, false, null, null));
            return rows;
        }
        if (host.guests().isEmpty()) {
            rows.add(new Row(Component.translatable("grubie_companion.panel.noGuests"), MUTED, false, null, null));
            return rows;
        }
        rows.add(new Row(Component.translatable("grubie_companion.panel.guests", host.guests().size()), MUTED, false, null, null));
        for (Protocol.Guest guest : host.guests().subList(0, Math.min(MAX_GUESTS, host.guests().size()))) {
            Component text = Component.literal(guest.name() + " · ")
                    .append(Component.translatable("grubie_companion.transport." + transport(guest.transport())));
            rows.add(new Row(text, TEXT, false, Action.KICK, guest.key()));
        }
        if (host.guests().size() > MAX_GUESTS) {
            rows.add(new Row(Component.translatable("grubie_companion.panel.more", host.guests().size() - MAX_GUESTS), MUTED, false, null, null));
        }
        return rows;
    }

    public static Component actionLabel(Action action) {
        return Component.translatable(action == Action.ACCEPT ? "grubie_companion.panel.accept" : "grubie_companion.panel.kick");
    }

    public static void run(Action action, String target) {
        Companion companion = Companion.get();
        if (companion == null || target == null) return;
        if (action == Action.ACCEPT) companion.accept(target);
        else companion.kick(target);
    }

    private static String transport(String value) {
        return switch (value) {
            case "direct", "turn" -> value;
            default -> "relay";
        };
    }
}
