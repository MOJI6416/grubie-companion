package com.grubielauncher.companion;

import com.grubielauncher.companion.core.Bridge;
import com.grubielauncher.companion.core.CompanionEnv;
import com.grubielauncher.companion.core.CompanionState;
import com.grubielauncher.companion.core.Json;
import com.grubielauncher.companion.core.Log;
import com.grubielauncher.companion.core.Protocol;
import com.grubielauncher.companion.core.ServerSnapshot;
import com.grubielauncher.companion.core.StatsSchedule;
import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.render.Canvas;
import com.grubielauncher.companion.render.Gfx;
import com.grubielauncher.companion.render.Tex;
import com.grubielauncher.companion.ui.Anim;
import com.grubielauncher.companion.ui.Avatars;
import com.grubielauncher.companion.ui.Panel;
import com.grubielauncher.companion.ui.PanelScreen;
import com.grubielauncher.companion.ui.Theme;
import com.grubielauncher.companion.ui.Toasts;
import com.grubielauncher.companion.ui.Ui;
import com.grubielauncher.companion.ui.VoiceHud;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class Companion {
    public static final String MOD_ID = "grubie_companion";
    public static final Logger LOGGER = LogManager.getLogger("GrubieCompanion");
    private static final List<String> FEATURES = List.of("panel", "toasts", "share", "invite", "join", "voice", "ptt", "screenshot", "stats", "stage", "hang");
    private static final long WORLD_STATS_MS = 45_000L;

    private static boolean initialized;
    private static Companion instance;

    private final CompanionEnv env;
    private final CompanionState state = new CompanionState();
    private final StatsSchedule schedule = new StatsSchedule();
    private final ServerSnapshot snapshots = new ServerSnapshot();
    private final Bridge bridge;
    private final Diagnostics diagnostics;
    private final KeyMapping panelKey;
    private final KeyMapping acceptKey;
    private final AtomicInteger requests = new AtomicInteger();
    private final Map<String, String> pending = new HashMap<>();
    private final Map<String, Long> invited = new HashMap<>();
    private final Map<String, Long> screenshotsSent = new HashMap<>();
    private final Map<String, Tex> thumbs = new HashMap<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Grubie Companion io");
        thread.setDaemon(true);
        return thread;
    });
    private boolean sharePending;
    private boolean pttDown;
    private boolean panelHeld;
    private boolean acceptHeld;
    private long lastWorldStats;
    private long worldCaptureAt = -1;
    private final boolean selftest = "1".equals(System.getenv("GRUBIE_COMPANION_SELFTEST"));
    private long selftestWorldAt = -1;
    private int selftestStep;
    private String worldStatsKey;

    private Companion(CompanionEnv env, String loader, String modVersion) {
        this.env = env;
        this.panelKey = Mc.newKey("key.grubie_companion.panel", Mc.KEY_G);
        this.acceptKey = Mc.newKey("key.grubie_companion.accept", Mc.KEY_F7);
        Json.Obj hello = Protocol.hello(env.token, modVersion, Mc.minecraftVersion(), loader, FEATURES);
        this.diagnostics = new Diagnostics(this::send);
        this.bridge = new Bridge(env.host, env.port, hello, new Bridge.Handler() {
            @Override
            public void onMessage(Json.Obj message) {
                state.apply(message);
            }

            @Override
            public void onConnected(boolean connected) {
                state.setConnected(connected);
                LOGGER.info(connected ? "Connected to Grubie Launcher" : "Lost the connection to Grubie Launcher");
                if (connected) diagnostics.onConnected();
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
        instance.diagnostics.start();
        return instance;
    }

    public static synchronized Companion ensure() {
        if (initialized) return instance;
        return init(detectLoader(), readVersion());
    }

    private static String detectLoader() {
        if (present("net.neoforged.fml.common.Mod")) return "neoforge";
        if (present("net.minecraftforge.fml.common.Mod")) return "forge";
        if (present("org.quiltmc.loader.api.QuiltLoader")) return "quilt";
        return "fabric";
    }

    private static boolean present(String name) {
        try {
            Class.forName(name, false, Companion.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static String readVersion() {
        try (var stream = Companion.class.getResourceAsStream("/assets/grubie_companion/version.txt")) {
            if (stream == null) return "0";
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (java.io.IOException e) {
            return "0";
        }
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

    public KeyMapping acceptKey() {
        return acceptKey;
    }

    public String panelKeyName() {
        return keyName(panelKey);
    }

    public String acceptKeyName() {
        return keyName(acceptKey);
    }

    private static String keyName(KeyMapping mapping) {
        String name = mapping.getTranslatedKeyMessage().getString();
        if (name.startsWith("key.")) {
            String raw = mapping.saveString();
            name = raw.substring(raw.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
        }
        if (name.length() == 1) return name.toUpperCase(Locale.ROOT);
        return name.length() > 6 ? name.substring(0, 6) : name;
    }

    boolean send(Json.Obj message) {
        return bridge.send(message);
    }

    private String request(String kind) {
        int colon = kind.indexOf(':');
        String req = (colon < 0 ? kind : kind.substring(0, colon)) + "-" + requests.incrementAndGet();
        pending.put(req, kind);
        return req;
    }

    public void tick(Minecraft mc) {
        diagnostics.tick(mc);
        for (Protocol.ToastItem toast : state.drainFresh()) Toasts.push(toast);
        Protocol.Avatar avatar;
        while ((avatar = state.takeAvatar()) != null) Avatars.accept(avatar);
        Protocol.Result result;
        while ((result = state.takeResult()) != null) onResult(result);

        Protocol.ConnectRequest connect = Mc.isLoading(mc) ? null : state.takeConnect();
        if (connect != null) {
            LOGGER.info("Grubie Launcher asked to join {}", connect.name());
            Mc.connect(mc, connect.address(), connect.name());
        }

        Screen screen = Mc.screen(mc);
        boolean typing = screen != null && screen.getFocused() instanceof EditBox;
        boolean panelDown = Mc.keyDown(mc, Mc.boundKey(panelKey));
        boolean acceptDown = Mc.keyDown(mc, Mc.boundKey(acceptKey));
        boolean openPanel = panelDown && !panelHeld && screen != null && Mc.isPause(screen);
        boolean acceptInvite = acceptDown && !acceptHeld && screen != null && !typing;
        panelHeld = panelDown;
        acceptHeld = acceptDown;
        while (panelKey.consumeClick()) openPanel = true;
        while (acceptKey.consumeClick()) acceptInvite = true;
        if (openPanel && (screen == null || Mc.isPause(screen))) openPanel(mc, screen);
        if (acceptInvite) {
            String id = Toasts.latestInvite();
            if (id == null) {
                List<Protocol.ToastItem> invites = state.view().invites();
                if (!invites.isEmpty()) id = invites.get(0).id();
            }
            if (id != null) accept(id);
        }
        trackPtt(mc, screen);
        trackStats(mc);
        if (selftest) selftest(mc);
    }

    private void selftest(Minecraft mc) {
        if (!Mc.inWorld(mc) || selftestStep >= 3) return;
        long now = System.currentTimeMillis();
        if (selftestWorldAt < 0) selftestWorldAt = now;
        if (selftestStep == 0 && now - selftestWorldAt > 1_000L) {
            selftestStep = 1;
            Mc.setScreen(mc, new PanelScreen(null, Panel.Mode.FRIENDS, null));
        } else if (selftestStep == 1 && now - selftestWorldAt > 2_000L) {
            selftestStep = 2;
            Mc.setScreen(mc, new PanelScreen(null, Panel.Mode.VOICE, null));
        } else if (selftestStep == 2 && now - selftestWorldAt > 3_000L) {
            selftestStep = 3;
            Mc.setScreen(mc, null);
            send(Protocol.stage("selftest", "panel", -1));
            LOGGER.info("Grubie Companion selftest: panel rendered");
        }
    }

    private void openPanel(Minecraft mc, Screen parent) {
        String screenshot = Toasts.latestScreenshot();
        if (screenshot != null) {
            Toasts.dismiss(screenshot);
            Mc.setScreen(mc, new PanelScreen(parent, Panel.Mode.SCREENSHOT, screenshot));
        } else {
            Mc.setScreen(mc, new PanelScreen(parent, Panel.Mode.FRIENDS, null));
        }
    }

    private void trackPtt(Minecraft mc, Screen screen) {
        Protocol.VoiceState voice = state.view().voice();
        boolean down = false;
        if (voice.active() && voice.ptt() && voice.pttCode() > 0 && (screen == null || Mc.isPause(screen))) {
            down = Mc.keyDown(mc, Mc.launcherKey(voice.pttCode()));
        }
        if (down != pttDown) {
            pttDown = down;
            send(Protocol.ptt(down));
        }
    }

    public boolean transmitting() {
        return pttDown;
    }

    public void renderOverlay(Gfx gfx) {
        Minecraft mc = Mc.mc();
        Anim.frame();
        float unit = Mc.compactUnit(mc);
        Canvas canvas = Canvas.begin(gfx, Mc.guiScale(mc), unit);
        try {
            Ui ui = new Ui(canvas, -1, -1, false);
            Screen screen = Mc.screen(mc);
            if (screen == null && Mc.inWorld(mc) && Settings.on(Settings.Option.VOICE_HUD)) VoiceHud.render(ui, state.view().voice(), pttDown);
            float width = Mc.guiWidth(mc) * Mc.guiScale(mc) / unit;
            Toasts.render(ui, screen instanceof PanelScreen ? width - Panel.occupiedWidth() : width);
        } finally {
            canvas.end();
        }
    }

    public void onPauseScreen() {
        schedule.onPause();
        lastWorldStats = 0;
    }

    public boolean canInvite(Minecraft mc) {
        if (Mc.currentServerAddress(mc) != null) return true;
        return Mc.isSingleplayer(mc) && state.view().host().active();
    }

    public String inviteTargetName(Minecraft mc) {
        String server = Mc.currentServerAddress(mc);
        if (server != null) {
            String name = Mc.currentServerName(mc);
            return name == null ? server : name;
        }
        String world = Mc.worldName(mc);
        return world == null ? "" : world;
    }

    public boolean wasInvited(String friendId) {
        Long at = invited.get(friendId);
        return at != null && System.currentTimeMillis() - at < 120_000L;
    }

    public boolean wasSentScreenshot(String path, String friendId) {
        return screenshotsSent.containsKey(path + "|" + friendId);
    }

    public boolean sharePending() {
        return sharePending;
    }

    public void accept(String toastId) {
        if (send(Protocol.accept(toastId))) {
            state.forgetInvite(toastId);
            Toasts.dismiss(toastId);
        }
    }

    public void decline(String toastId) {
        if (send(Protocol.decline(toastId))) {
            state.forgetInvite(toastId);
            Toasts.dismiss(toastId);
        }
    }

    public void invite(String friendId) {
        Minecraft mc = Mc.mc();
        String server = Mc.currentServerAddress(mc);
        String req = request("invite:" + friendId);
        Json.Obj message = server != null
                ? Protocol.inviteToServer(req, friendId, server, Mc.currentServerName(mc))
                : Protocol.inviteToWorld(req, friendId);
        if (send(message)) invited.put(friendId, System.currentTimeMillis());
        else pending.remove(req);
    }

    public void join(String friendId) {
        String req = request("join:" + friendId);
        if (!send(Protocol.join(req, friendId))) pending.remove(req);
    }

    public void share() {
        Minecraft mc = Mc.mc();
        if (sharePending || !Mc.isSingleplayer(mc)) return;
        int port = Mc.openToLan(mc);
        if (port <= 0) {
            Toasts.local("share-error", "error", Mc.tr("grubie_companion.error.title"), Mc.tr("grubie_companion.error.lan"));
            return;
        }
        String req = request("share");
        if (send(Protocol.shareStart(req, port, "friends", Mc.worldName(mc)))) {
            sharePending = true;
        } else {
            pending.remove(req);
            Toasts.local("share-error", "error", Mc.tr("grubie_companion.error.title"), Mc.tr("grubie_companion.error.offline"));
        }
    }

    public void unshare() {
        String req = request("unshare");
        if (!send(Protocol.shareStop(req))) pending.remove(req);
    }

    public void kick(String guestKey) {
        send(Protocol.kick(guestKey));
    }

    public void voice(String action) {
        send(Protocol.voice(action));
    }

    public void sendScreenshot(String path, String friendId) {
        if (path == null) return;
        String key = path + "|" + friendId;
        String req = request("screenshot:" + key);
        if (send(Protocol.sendScreenshot(req, path, friendId))) screenshotsSent.put(key, System.currentTimeMillis());
        else pending.remove(req);
    }

    public void drawScreenshotThumb(Ui ui, String path, float x, float y, float w, float h) {
        if (path == null) return;
        Tex tex = thumbs.get(path);
        if (tex == null && !thumbs.containsKey(path)) {
            thumbs.put(path, null);
            try {
                Path file = Path.of(path);
                Path root = Mc.gameDir(Mc.mc()).toAbsolutePath().normalize();
                if (file.toAbsolutePath().normalize().startsWith(root) && Files.size(file) < 32L * 1024 * 1024) {
                    tex = Tex.fromPng("screenshot", Files.readAllBytes(file));
                    thumbs.put(path, tex);
                }
            } catch (Exception e) {
                LOGGER.debug("Screenshot preview failed: {}", e.toString());
            }
        }
        if (tex == null) {
            ui.icon("camera", x + w / 2f - 6f, y + h / 2f - 6f, 12f, Theme.FAINT);
            return;
        }
        float ratio = tex.width() / (float) Math.max(1, tex.height());
        float dw = w;
        float dh = w / ratio;
        if (dh > h) {
            dh = h;
            dw = h * ratio;
        }
        ui.canvas.image(tex, x + (w - dw) / 2f, y + (h - dh) / 2f, dw, dh, 0xFFFFFFFF);
    }

    private void onResult(Protocol.Result result) {
        String kind = pending.remove(result.req());
        if (kind == null) return;
        if (kind.equals("share")) sharePending = false;
        if (result.ok()) {
            if (kind.equals("share")) Toasts.local("share-ok", "system", Mc.tr("grubie_companion.play.sharedToast"), Mc.tr("grubie_companion.play.sharedToastBody"));
            return;
        }
        if (kind.startsWith("invite:")) invited.remove(kind.substring("invite:".length()));
        if (kind.startsWith("screenshot:")) screenshotsSent.remove(kind.substring("screenshot:".length()));
        String error = result.error() == null ? "unknown" : result.error();
        String key = "grubie_companion.error." + error;
        String text = Mc.tr(key);
        if (text.equals(key)) text = Mc.tr("grubie_companion.error.unknown");
        Toasts.local("error-" + result.req(), "error", Mc.tr("grubie_companion.error.title"), text);
    }

    public static void clickSound() {
        if (!Settings.on(Settings.Option.SOUNDS)) return;
        try {
            Minecraft mc = Mc.mc();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        } catch (RuntimeException e) {
            LOGGER.debug("Click sound failed: {}", e.toString());
        }
    }

    private void trackStats(Minecraft mc) {
        String server = Mc.currentServerAddress(mc);
        if (server != null) {
            switch (schedule.tick(System.currentTimeMillis(), server)) {
                case REQUEST -> Mc.requestStats(mc);
                case CAPTURE -> capture(mc, "server", server, Mc.currentServerName(mc), true);
                default -> {
                }
            }
            return;
        }
        schedule.tick(System.currentTimeMillis(), null);
        if (!Mc.isSingleplayer(mc) || mc.player == null) {
            worldStatsKey = null;
            return;
        }
        String world = Mc.worldFolder(mc);
        if (world == null) return;
        long now = System.currentTimeMillis();
        if (!world.equals(worldStatsKey)) {
            worldStatsKey = world;
            lastWorldStats = now - WORLD_STATS_MS + 10_000L;
        }
        if (now - lastWorldStats >= WORLD_STATS_MS) {
            lastWorldStats = now;
            worldCaptureAt = now + StatsSchedule.RESPONSE_DELAY_MS;
            Mc.requestStats(mc);
        } else if (worldCaptureAt > 0 && now >= worldCaptureAt) {
            worldCaptureAt = -1;
            capture(mc, "world", world, Mc.worldName(mc), false);
        }
    }

    private void capture(Minecraft mc, String kind, String key, String name, boolean file) {
        if (mc.player == null || mc.getConnection() == null) return;
        var stats = StatsReader.read(mc.player.getStats());
        if (stats.isEmpty()) return;
        var advancements = StatsReader.advancements(mc.getConnection().getAdvancements());
        Json.Obj snapshot = ServerSnapshot.build(
                key,
                name,
                new ServerSnapshot.Player(mc.player.getUUID().toString(), mc.player.getName().getString()),
                Mc.minecraftVersion(),
                stats,
                advancements,
                Instant.now());
        send(Protocol.stats(kind, key, name, snapshot));
        if (!file || env.statsDir == null) return;
        io.execute(() -> {
            try {
                snapshots.write(env.statsDir, snapshot);
            } catch (Exception e) {
                LOGGER.warn("Could not save the server statistics for Grubie Launcher: {}", e.toString());
            }
        });
    }
}
