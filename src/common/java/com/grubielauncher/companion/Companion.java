package com.grubielauncher.companion;

import com.google.gson.JsonObject;
import com.grubielauncher.companion.core.Bridge;
import com.grubielauncher.companion.core.CompanionEnv;
import com.grubielauncher.companion.core.CompanionState;
import com.grubielauncher.companion.core.Log;
import com.grubielauncher.companion.core.Protocol;
import com.grubielauncher.companion.core.ServerSnapshot;
import com.grubielauncher.companion.core.StatsSchedule;
import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.mc.PanelScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Companion {
    public static final String MOD_ID = "grubie_companion";
    public static final Logger LOGGER = LoggerFactory.getLogger("GrubieCompanion");

    private static boolean initialized;
    private static Companion instance;

    private final CompanionEnv env;
    private final CompanionState state = new CompanionState();
    private final StatsSchedule schedule = new StatsSchedule();
    private final ServerSnapshot snapshots = new ServerSnapshot();
    private final Bridge bridge;
    private final KeyMapping panelKey;
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Grubie Companion io");
        thread.setDaemon(true);
        return thread;
    });

    private Companion(CompanionEnv env, String loader, String modVersion) {
        this.env = env;
        this.panelKey = Mc.newPanelKey();
        JsonObject hello = Protocol.hello(env.token, modVersion, Mc.minecraftVersion(), loader);
        this.bridge = new Bridge(env.host, env.port, hello, new Bridge.Handler() {
            @Override
            public void onMessage(JsonObject message) {
                state.apply(message);
            }

            @Override
            public void onConnected(boolean connected) {
                state.setConnected(connected);
                LOGGER.info(connected ? "Connected to Grubie Launcher" : "Lost the connection to Grubie Launcher");
            }
        });
    }

    public static synchronized Companion init(String loader, String modVersion) {
        if (initialized) return instance;
        initialized = true;
        Log.setSink((level, message) -> {
            switch (level) {
                case "warn" -> LOGGER.warn(message);
                case "info" -> LOGGER.info(message);
                default -> LOGGER.debug(message);
            }
        });
        CompanionEnv env = CompanionEnv.fromSystem();
        if (env == null) {
            LOGGER.info("Started without Grubie Launcher, staying silent");
            return null;
        }
        instance = new Companion(env, loader, modVersion);
        instance.bridge.start();
        return instance;
    }

    public static Companion get() {
        return instance;
    }

    public CompanionState state() {
        return state;
    }

    public KeyMapping panelKey() {
        return panelKey;
    }

    public void tick(Minecraft mc) {
        for (Protocol.ToastItem toast : state.drainFresh()) {
            Component body = Component.literal(toast.body());
            if (toast.actionable()) {
                body = Component.translatable("grubie_companion.toast.answer", toast.body(), panelKey.getTranslatedKeyMessage());
            }
            Mc.showToast(mc, toast.kind(), Component.literal(toast.title()), body);
        }

        Protocol.ConnectRequest connect = Mc.isLoading(mc) ? null : state.takeConnect();
        if (connect != null) {
            LOGGER.info("Grubie Launcher asked to join {}", connect.name());
            Mc.connect(mc, connect.address(), connect.name());
        }

        while (panelKey.consumeClick()) {
            if (Mc.screen(mc) == null) Mc.setScreen(mc, new PanelScreen(null));
        }

        trackStats(mc);
    }

    public void onPauseScreen() {
        schedule.onPause();
    }

    public void accept(String toastId) {
        if (bridge.send(Protocol.accept(toastId))) state.forgetInvite(toastId);
    }

    public void kick(String guestKey) {
        bridge.send(Protocol.kick(guestKey));
    }

    private void trackStats(Minecraft mc) {
        if (env.statsDir == null) return;
        String server = Mc.currentServerAddress(mc);
        switch (schedule.tick(System.currentTimeMillis(), server)) {
            case REQUEST -> Mc.requestStats(mc);
            case CAPTURE -> capture(mc, server);
            default -> {
            }
        }
    }

    private void capture(Minecraft mc, String server) {
        if (server == null || mc.player == null || mc.getConnection() == null) return;
        var stats = StatsReader.read(mc.player.getStats());
        if (stats.isEmpty()) return;
        var advancements = StatsReader.advancements(mc.getConnection().getAdvancements());
        JsonObject snapshot = ServerSnapshot.build(
                server,
                Mc.currentServerName(mc),
                new ServerSnapshot.Player(mc.player.getUUID().toString(), mc.player.getName().getString()),
                Mc.minecraftVersion(),
                stats,
                advancements,
                Instant.now());
        io.execute(() -> {
            try {
                snapshots.write(env.statsDir, snapshot);
            } catch (Exception e) {
                LOGGER.warn("Could not save the server statistics for Grubie Launcher: {}", e.toString());
            }
        });
    }
}
