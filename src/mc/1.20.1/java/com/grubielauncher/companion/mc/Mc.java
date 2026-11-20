package com.grubielauncher.companion.mc;

import com.grubielauncher.companion.core.ServerSnapshot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import org.lwjgl.glfw.GLFW;

public final class Mc {
    private Mc() {
    }

    public static Screen screen(Minecraft mc) {
        return mc.screen;
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        mc.setScreen(screen);
    }

    public static boolean isLoading(Minecraft mc) {
        return mc.getOverlay() != null;
    }

    public static String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    public static KeyMapping newPanelKey() {
        return new KeyMapping("key.grubie_companion.panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.multiplayer");
    }

    public static void showToast(Minecraft mc, String kind, Component title, Component body) {
        SystemToast.SystemToastIds id = "message".equals(kind)
                ? SystemToast.SystemToastIds.PERIODIC_NOTIFICATION
                : SystemToast.SystemToastIds.UNSECURE_SERVER_WARNING;
        mc.getToasts().addToast(SystemToast.multiline(mc, id, title, body));
    }

    public static String currentServerAddress(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null || mc.isLocalServer() || mc.isConnectedToRealms()) return null;
        ServerData data = mc.getCurrentServer();
        if (data == null) return null;
        return ServerSnapshot.normalizeAddress(data.ip);
    }

    public static String currentServerName(Minecraft mc) {
        ServerData data = mc.getCurrentServer();
        return data == null ? null : data.name;
    }

    public static void requestStats(Minecraft mc) {
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
    }

    public static String advancementId(Object key) {
        return key instanceof Advancement advancement ? advancement.getId().toString() : null;
    }

    public static void connect(Minecraft mc, String address, String name) {
        if (!ServerAddress.isValidAddress(address)) return;
        if (mc.level != null) {
            boolean local = mc.isLocalServer();
            mc.level.disconnect();
            if (local) {
                mc.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
            } else {
                mc.clearLevel();
            }
        }
        JoinMultiplayerScreen parent = new JoinMultiplayerScreen(new TitleScreen());
        ServerData data = new ServerData(name, address, false);
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(address), data, false);
    }
}
