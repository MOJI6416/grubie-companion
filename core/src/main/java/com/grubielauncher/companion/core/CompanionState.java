package com.grubielauncher.companion.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CompanionState {
    public record View(
            boolean connected,
            int protocol,
            String selfName,
            List<Protocol.ToastItem> invites,
            Protocol.FriendsState friends,
            Protocol.HostState host,
            Protocol.VoiceState voice,
            int revision) {
    }

    private static final int MAX_PENDING_AVATARS = 64;
    private static final int MAX_PENDING_RESULTS = 32;

    private final Object lock = new Object();
    private final Map<String, Integer> shown = new HashMap<>();
    private final List<Protocol.ToastItem> fresh = new ArrayList<>();
    private final ArrayDeque<Protocol.Avatar> avatars = new ArrayDeque<>();
    private final ArrayDeque<Protocol.Result> results = new ArrayDeque<>();
    private List<Protocol.ToastItem> toasts = List.of();
    private Protocol.FriendsState friends = Protocol.FriendsState.NONE;
    private Protocol.HostState host = Protocol.HostState.NONE;
    private Protocol.VoiceState voice = Protocol.VoiceState.NONE;
    private Protocol.ConnectRequest connect;
    private volatile Protocol.ConnectRequest place;
    private Protocol.Welcome welcome;
    private boolean connected;
    private int revision;

    public void apply(Json.Obj message) {
        String type = Protocol.type(message);
        if (type == null) return;
        synchronized (lock) {
            switch (type) {
                case "welcome" -> welcome = Protocol.welcome(message);
                case "toasts" -> applyToasts(Protocol.toasts(message));
                case "friends" -> friends = Protocol.friends(message);
                case "host" -> host = Protocol.host(message);
                case "voice" -> voice = Protocol.voice(message);
                case "connect" -> {
                    Protocol.ConnectRequest request = Protocol.connect(message);
                    if (request != null) {
                        connect = request;
                        place = request;
                    }
                }
                case "place" -> {
                    Protocol.ConnectRequest request = Protocol.connect(message);
                    if (request != null) place = request;
                }
                case "avatar" -> {
                    Protocol.Avatar avatar = Protocol.avatar(message);
                    if (avatar != null) {
                        if (avatars.size() >= MAX_PENDING_AVATARS) avatars.pollFirst();
                        avatars.addLast(avatar);
                    }
                    return;
                }
                case "result" -> {
                    Protocol.Result result = Protocol.result(message);
                    if (result != null) {
                        if (results.size() >= MAX_PENDING_RESULTS) results.pollFirst();
                        results.addLast(result);
                    }
                    return;
                }
                default -> {
                    return;
                }
            }
            revision++;
        }
    }

    public void setConnected(boolean value) {
        synchronized (lock) {
            if (connected == value) return;
            connected = value;
            if (!value) {
                toasts = List.of();
                friends = Protocol.FriendsState.NONE;
                host = Protocol.HostState.NONE;
                voice = Protocol.VoiceState.NONE;
                connect = null;
                welcome = null;
                fresh.clear();
                shown.clear();
                results.clear();
            }
            revision++;
        }
    }

    public List<Protocol.ToastItem> drainFresh() {
        synchronized (lock) {
            if (fresh.isEmpty()) return List.of();
            List<Protocol.ToastItem> result = List.copyOf(fresh);
            fresh.clear();
            return result;
        }
    }

    public Protocol.ConnectRequest place() {
        return place;
    }

    public Protocol.ConnectRequest takeConnect() {
        synchronized (lock) {
            Protocol.ConnectRequest request = connect;
            connect = null;
            return request;
        }
    }

    public Protocol.Avatar takeAvatar() {
        synchronized (lock) {
            return avatars.pollFirst();
        }
    }

    public Protocol.Result takeResult() {
        synchronized (lock) {
            return results.pollFirst();
        }
    }

    public void forgetInvite(String id) {
        synchronized (lock) {
            List<Protocol.ToastItem> next = new ArrayList<>(toasts);
            if (next.removeIf(toast -> toast.id().equals(id))) {
                toasts = List.copyOf(next);
                revision++;
            }
        }
    }

    public View view() {
        synchronized (lock) {
            List<Protocol.ToastItem> invites = new ArrayList<>();
            for (Protocol.ToastItem toast : toasts) {
                if (toast.actionable()) invites.add(toast);
            }
            return new View(
                    connected,
                    welcome == null ? 0 : welcome.protocol(),
                    welcome == null ? null : welcome.selfName(),
                    List.copyOf(invites),
                    friends,
                    host,
                    voice,
                    revision);
        }
    }

    public int revision() {
        synchronized (lock) {
            return revision;
        }
    }

    private void applyToasts(List<Protocol.ToastItem> next) {
        Map<String, Integer> nextShown = new HashMap<>();
        for (Protocol.ToastItem toast : next) {
            Integer before = shown.get(toast.id());
            if (before == null || toast.count() > before) fresh.add(toast);
            nextShown.put(toast.id(), toast.count());
        }
        shown.clear();
        shown.putAll(nextShown);
        toasts = next;
    }
}
