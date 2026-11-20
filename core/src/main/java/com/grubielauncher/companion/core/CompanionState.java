package com.grubielauncher.companion.core;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CompanionState {
    public record View(boolean connected, List<Protocol.ToastItem> invites, Protocol.HostState host, int revision) {
    }

    private final Object lock = new Object();
    private final Map<String, Integer> shown = new HashMap<>();
    private final List<Protocol.ToastItem> fresh = new ArrayList<>();
    private List<Protocol.ToastItem> toasts = List.of();
    private Protocol.HostState host = Protocol.HostState.NONE;
    private Protocol.ConnectRequest connect;
    private boolean connected;
    private int revision;

    public void apply(JsonObject message) {
        String type = Protocol.type(message);
        if (type == null) return;
        synchronized (lock) {
            switch (type) {
                case "toasts" -> applyToasts(Protocol.toasts(message));
                case "host" -> host = Protocol.host(message);
                case "connect" -> {
                    Protocol.ConnectRequest request = Protocol.connect(message);
                    if (request != null) connect = request;
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
                host = Protocol.HostState.NONE;
                connect = null;
                fresh.clear();
                shown.clear();
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

    public Protocol.ConnectRequest takeConnect() {
        synchronized (lock) {
            Protocol.ConnectRequest request = connect;
            connect = null;
            return request;
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
            return new View(connected, List.copyOf(invites), host, revision);
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
