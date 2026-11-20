package com.grubielauncher.companion.core;

import java.util.Objects;

public final class StatsSchedule {
    public static final long PERIOD_MS = 5 * 60_000L;
    public static final long FIRST_REQUEST_MS = 30_000L;
    public static final long PAUSE_GAP_MS = 20_000L;
    public static final long RESPONSE_DELAY_MS = 3_000L;
    public static final long REBUILD_MS = 60_000L;

    public enum Action {
        NONE,
        REQUEST,
        CAPTURE
    }

    private String server;
    private long lastRequest;
    private long lastCapture;
    private long captureAt = -1;
    private boolean pauseRequested;

    public Action tick(long now, String serverKey) {
        if (!Objects.equals(server, serverKey)) {
            server = serverKey;
            captureAt = -1;
            pauseRequested = false;
            lastRequest = now - PERIOD_MS + FIRST_REQUEST_MS;
            lastCapture = now;
        }
        if (server == null) return Action.NONE;

        boolean pauseDue = pauseRequested && now - lastRequest >= PAUSE_GAP_MS;
        if (pauseDue || now - lastRequest >= PERIOD_MS) {
            pauseRequested = false;
            lastRequest = now;
            captureAt = now + RESPONSE_DELAY_MS;
            return Action.REQUEST;
        }

        if ((captureAt >= 0 && now >= captureAt) || now - lastCapture >= REBUILD_MS) {
            captureAt = -1;
            lastCapture = now;
            return Action.CAPTURE;
        }
        return Action.NONE;
    }

    public void onPause() {
        if (server != null) pauseRequested = true;
    }

    public String server() {
        return server;
    }
}
