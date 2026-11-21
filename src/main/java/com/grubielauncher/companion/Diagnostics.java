package com.grubielauncher.companion;

import com.grubielauncher.companion.core.Json;
import com.grubielauncher.companion.core.Protocol;
import com.grubielauncher.companion.mc.Mc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class Diagnostics {
    private static final long STARTUP_LIMIT_MS = 90_000L;
    private static final long FREEZE_LIMIT_MS = 15_000L;
    private static final int MAX_FRAMES = 30;

    private final Predicate<Json.Obj> send;
    private final long started = System.currentTimeMillis();
    private final Thread initThread = Thread.currentThread();
    private volatile long lastTick;
    private volatile Thread gameThread;
    private volatile boolean hangReported;
    private volatile Json.Obj pendingHang;
    private String stage;

    public Diagnostics(Predicate<Json.Obj> send) {
        this.send = send;
    }

    public void start() {
        Thread watchdog = new Thread(this::watch, "Grubie Companion watchdog");
        watchdog.setDaemon(true);
        watchdog.setPriority(Thread.MIN_PRIORITY);
        watchdog.start();
        setStage("init", null);
    }

    public void onConnected() {
        if (stage != null) send.test(Protocol.stage(stage, null, -1));
        Json.Obj hang = pendingHang;
        if (hang != null && send.test(hang)) pendingHang = null;
    }

    public void tick(Minecraft mc) {
        lastTick = System.currentTimeMillis();
        gameThread = Thread.currentThread();
        if (hangReported) {
            hangReported = false;
            send.test(Protocol.stage("resumed", null, -1));
        }
        String next = stageOf(mc);
        if (!next.equals(stage)) setStage(next, null);
    }

    private void setStage(String next, String detail) {
        stage = next;
        send.test(Protocol.stage(next, detail, -1));
    }

    private static String stageOf(Minecraft mc) {
        if (Mc.isLoading(mc)) return "resources";
        Screen screen = Mc.screen(mc);
        if (Mc.inWorld(mc)) return Mc.isSingleplayer(mc) ? "world" : "server";
        if (screen instanceof TitleScreen) return "menu";
        if (screen != null) {
            String name = screen.getClass().getSimpleName();
            if (name.contains("Connect") || name.contains("LevelLoading") || name.contains("ReceivingLevel") || name.contains("Progress") || name.contains("GenericMessage") || name.contains("GenericDirt")) return "joining";
        }
        return "menu";
    }

    private void watch() {
        while (true) {
            try {
                Thread.sleep(2_000L);
            } catch (InterruptedException e) {
                return;
            }
            long now = System.currentTimeMillis();
            long last = lastTick;
            long since = last == 0 ? now - started : now - last;
            long limit = last == 0 ? STARTUP_LIMIT_MS : FREEZE_LIMIT_MS;
            if (since < limit || hangReported) continue;
            Thread thread = gameThread != null ? gameThread : mainThread();
            if (thread == null || !thread.isAlive()) continue;
            hangReported = true;
            Json.Obj report = report(thread, since);
            if (!send.test(report)) pendingHang = report;
            Companion.LOGGER.warn("Grubie Companion: the game has not ticked for {} s, stage {}", since / 1000, stage);
        }
    }

    private Thread mainThread() {
        Thread fallback = null;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            String name = thread.getName();
            if ("Render thread".equals(name)) return thread;
            if ("main".equals(name)) fallback = thread;
        }
        return fallback != null ? fallback : initThread;
    }

    private Json.Obj report(Thread thread, long since) {
        StackTraceElement[] trace = thread.getStackTrace();
        List<String> frames = new ArrayList<>();
        Set<String> sources = new LinkedHashSet<>();
        ClassLoader loader = thread.getContextClassLoader();
        for (StackTraceElement element : trace) {
            if (frames.size() >= MAX_FRAMES) break;
            frames.add(element.toString());
            String source = sourceOf(element.getClassName(), loader);
            if (source != null) sources.add(source);
        }
        Json.Obj message = Protocol.hang(since / 1000, thread.getName(), frames, new ArrayList<>(sources));
        message.put("stage", stage);
        return message;
    }

    private static String sourceOf(String className, ClassLoader loader) {
        if (className.startsWith("java.") || className.startsWith("jdk.") || className.startsWith("sun.")) return null;
        try {
            Class<?> type = Class.forName(className, false, loader == null ? Diagnostics.class.getClassLoader() : loader);
            CodeSource code = type.getProtectionDomain() == null ? null : type.getProtectionDomain().getCodeSource();
            URL url = code == null ? null : code.getLocation();
            if (url == null) return null;
            String path = url.toString();
            int hash = path.indexOf('#');
            if (hash > 0) path = path.substring(0, hash);
            int bang = path.indexOf("!/");
            if (bang > 0) path = path.substring(0, bang);
            while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            name = name.replace("%2B", "+").replace("%20", " ");
            if (name.isEmpty()) return null;
            return name.length() > 120 ? name.substring(0, 120) : name;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
