package com.grubielauncher.companion.mc;

import com.grubielauncher.companion.core.ServerSnapshot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
//? if >=1.17
import net.minecraft.client.multiplayer.resolver.ServerAddress;
//? if >=1.20.2 {
import net.minecraft.advancements.AdvancementHolder;
//?} else
/*import net.minecraft.advancements.Advancement;*/
//? if >=1.21.6
/*import net.minecraft.client.multiplayer.ClientLevel;*/
//? if >=26.2
/*import net.minecraft.server.MinecraftServer;*/

import java.nio.file.Path;

public final class Mc {
    //? if >=26.3 {
    /*public static final int KEY_G = InputConstants.KEY_G;
    public static final int KEY_F7 = InputConstants.KEY_F7;
    *///?} else {
    public static final int KEY_G = 71;
    public static final int KEY_F7 = 296;
    //?}

    private Mc() {
    }

    public static int launcherKey(int glfw) {
        //? if >=26.3 {
        /*String name = glfwKeyName(glfw);
        if (name == null) return -1;
        InputConstants.Key key = InputConstants.getKey(name);
        return key.getType() == InputConstants.Type.MOUSE ? -1 : key.getValue();
        *///?} else
        return glfw;
    }

    static String glfwKeyName(int code) {
        if (code >= 65 && code <= 90) return "key.keyboard." + (char) ('a' + code - 65);
        if (code >= 48 && code <= 57) return "key.keyboard." + (char) ('0' + code - 48);
        if (code >= 290 && code <= 314) return "key.keyboard.f" + (code - 289);
        if (code >= 320 && code <= 329) return "key.keyboard.keypad." + (code - 320);
        switch (code) {
            case 32: return "key.keyboard.space";
            case 39: return "key.keyboard.apostrophe";
            case 44: return "key.keyboard.comma";
            case 45: return "key.keyboard.minus";
            case 46: return "key.keyboard.period";
            case 47: return "key.keyboard.slash";
            case 59: return "key.keyboard.semicolon";
            case 61: return "key.keyboard.equal";
            case 91: return "key.keyboard.left.bracket";
            case 92: return "key.keyboard.backslash";
            case 93: return "key.keyboard.right.bracket";
            case 96: return "key.keyboard.grave.accent";
            case 256: return "key.keyboard.escape";
            case 257: return "key.keyboard.enter";
            case 258: return "key.keyboard.tab";
            case 259: return "key.keyboard.backspace";
            case 260: return "key.keyboard.insert";
            case 261: return "key.keyboard.delete";
            case 262: return "key.keyboard.right";
            case 263: return "key.keyboard.left";
            case 264: return "key.keyboard.down";
            case 265: return "key.keyboard.up";
            case 266: return "key.keyboard.page.up";
            case 267: return "key.keyboard.page.down";
            case 268: return "key.keyboard.home";
            case 269: return "key.keyboard.end";
            case 280: return "key.keyboard.caps.lock";
            case 281: return "key.keyboard.scroll.lock";
            case 282: return "key.keyboard.num.lock";
            case 283: return "key.keyboard.print.screen";
            case 330: return "key.keyboard.keypad.decimal";
            case 331: return "key.keyboard.keypad.divide";
            case 332: return "key.keyboard.keypad.multiply";
            case 333: return "key.keyboard.keypad.subtract";
            case 334: return "key.keyboard.keypad.add";
            case 335: return "key.keyboard.keypad.enter";
            case 340: return "key.keyboard.left.shift";
            case 341: return "key.keyboard.left.control";
            case 342: return "key.keyboard.left.alt";
            case 343: return "key.keyboard.left.win";
            default: return null;
        }
    }

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static Screen screen(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.screen();
        *///?} else
        return mc.screen;
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        //? if >=26.2 {
        /*mc.gui.setScreen(screen);
        *///?} else
        mc.setScreen(screen);
    }

    public static boolean isLoading(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gui.overlay() != null;
        *///?} else
        return mc.getOverlay() != null;
    }

    public static boolean isPause(Screen screen) {
        return screen instanceof PauseScreen;
    }

    public static String minecraftVersion() {
        //? if >=1.21.6 {
        /*return SharedConstants.getCurrentVersion().name();
        *///?} else
        return SharedConstants.getCurrentVersion().getName();
    }

    public static KeyMapping newKey(String name, int key) {
        //? if >=1.21.9 {
        /*return new KeyMapping(name, key, KeyMapping.Category.MULTIPLAYER);
        *///?} else
        return new KeyMapping(name, InputConstants.Type.KEYSYM, key, "key.categories.multiplayer");
    }

    public static int boundKey(KeyMapping mapping) {
        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        return key.getType() == InputConstants.Type.MOUSE ? -1 : key.getValue();
    }

    public static boolean keyDown(Minecraft mc, int key) {
        if (key <= 0) return false;
        //? if >=26.3 {
        /*return InputConstants.isKeyDown(key);
        *///?} elif >=1.21.9 {
        /*return InputConstants.isKeyDown(mc.getWindow(), key);
        *///?} else
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), key);
    }

    public static float guiScale(Minecraft mc) {
        return (float) mc.getWindow().getGuiScale();
    }

    public static float compactUnit(Minecraft mc) {
        float height = guiHeight(mc) * guiScale(mc);
        float unit = Math.round(height / 540f * 2f) / 2f;
        return Math.max(2f, Math.min(3f, unit));
    }

    public static int guiWidth(Minecraft mc) {
        return mc.getWindow().getGuiScaledWidth();
    }

    public static int guiHeight(Minecraft mc) {
        return mc.getWindow().getGuiScaledHeight();
    }

    public static float mouseX(Minecraft mc) {
        return (float) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / Math.max(1, mc.getWindow().getScreenWidth()));
    }

    public static float mouseY(Minecraft mc) {
        return (float) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / Math.max(1, mc.getWindow().getScreenHeight()));
    }

    public static String tr(String key, Object... args) {
        return I18n.get(key, args);
    }

    public static Component literal(String text) {
        //? if >=1.19 {
        return Component.literal(text);
        //?} else
        /*return Component.nullToEmpty(text);*/
    }

    public static Path gameDir(Minecraft mc) {
        return mc.gameDirectory.toPath();
    }

    public static boolean inWorld(Minecraft mc) {
        return mc.level != null && mc.player != null;
    }

    public static boolean isSingleplayer(Minecraft mc) {
        return mc.isLocalServer() && mc.getSingleplayerServer() != null;
    }

    public static String currentServerAddress(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null || mc.isLocalServer()) return null;
        ServerData data = mc.getCurrentServer();
        if (data == null) return null;
        //? if >=1.20.2 {
        if (data.isRealm()) return null;
        //?} else
        /*if (mc.isConnectedToRealms()) return null;*/
        return ServerSnapshot.normalizeAddress(data.ip);
    }

    public static String currentServerName(Minecraft mc) {
        ServerData data = mc.getCurrentServer();
        return data == null ? null : data.name;
    }

    public static String worldName(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return null;
        return server.getWorldData().getLevelName();
    }

    public static String worldFolder(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return null;
        Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        Path name = root.getFileName();
        return name == null ? null : name.toString();
    }

    public static boolean isPublished(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        return server != null && server.isPublished();
    }

    public static int publishedPort(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || !server.isPublished()) return -1;
        return server.getPort();
    }

    public static int openToLan(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mc.gameMode == null) return -1;
        if (server.isPublished()) return server.getPort();
        int port = HttpUtil.getAvailablePort();
        GameType mode = mc.gameMode.getPlayerMode();
        //? if >=1.20.6 {
        boolean cheats = server.getWorldData().isAllowCommands();
        //?} else
        /*boolean cheats = server.getWorldData().getAllowCommands();*/
        //? if >=26.3 {
        /*boolean ok = server.publishServer(MinecraftServer.MultiplayerScope.LAN, cheats, port);
        *///?} elif >=26.2 {
        /*boolean ok = server.publishServer(MinecraftServer.MultiplayerScope.LAN, mode, cheats, port);
        *///?} else
        boolean ok = server.publishServer(mode, cheats, port);
        return ok ? port : -1;
    }

    public static void requestStats(Minecraft mc) {
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
    }

    public static String advancementId(Object key) {
        //? if >=1.20.2 {
        return key instanceof AdvancementHolder holder ? holder.id().toString() : null;
        //?} else
        /*return key instanceof Advancement advancement ? advancement.getId().toString() : null;*/
    }

    public static boolean validAddress(String address) {
        //? if >=1.17 {
        return ServerAddress.isValidAddress(address);
        //?} else
        /*return address != null && !address.isEmpty() && address.length() <= 255;*/
    }

    public static void connect(Minecraft mc, String address, String name) {
        if (!validAddress(address)) return;
        leaveWorld(mc);
        JoinMultiplayerScreen parent = new JoinMultiplayerScreen(new TitleScreen());
        //? if >=1.20.2 {
        ServerData data = new ServerData(name, address, ServerData.Type.OTHER);
        //?} else
        /*ServerData data = new ServerData(name, address, false);*/
        //? if >=1.20.6 {
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(address), data, false, null);
        //?} elif >=1.20 {
        /*ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(address), data, false);
        *///?} elif >=1.17 {
        /*ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(address), data);
        *///?} else
        /*setScreen(mc, new ConnectScreen(parent, mc, data));*/
    }

    public static void leaveWorld(Minecraft mc) {
        if (mc.level == null) return;
        //? if >=1.21.9 {
        /*mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
        *///?} elif >=1.21.6 {
        /*mc.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
        mc.disconnect(new JoinMultiplayerScreen(new TitleScreen()), false);
        *///?} elif >=1.20.2 {
        mc.level.disconnect();
        mc.disconnect();
        //?} else {
        /*mc.level.disconnect();
        mc.clearLevel();
        *///?}
    }
}
