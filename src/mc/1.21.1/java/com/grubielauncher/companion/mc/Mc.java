package com.grubielauncher.companion.mc;

import com.grubielauncher.companion.core.ServerSnapshot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import org.lwjgl.glfw.GLFW;

public final class Mc {
    private static final SystemToast.SystemToastId INVITE = new SystemToast.SystemToastId(15_000L);
    private static final SystemToast.SystemToastId MESSAGE = new SystemToast.SystemToastId(8_000L);

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
        SystemToast.SystemToastId id = "message".equals(kind) ? MESSAGE : INVITE;
        mc.getToasts().addToast(SystemToast.multiline(mc, id, title, body));
    }

    public static String currentServerAddress(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null || mc.isLocalServer()) return null;
        ServerData data = mc.getCurrentServer();
        if (data == null || data.isRealm()) return null;
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
        return key instanceof AdvancementHolder holder ? holder.id().toString() : null;
    }

    public static void connect(Minecraft mc, String address, String name) {
        if (!ServerAddress.isValidAddress(address)) return;
        if (mc.level != null) {
            boolean local = mc.isLocalServer();
            mc.level.disconnect();
            if (local) {
                mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
            } else {
                mc.disconnect();
            }
        }
        JoinMultiplayerScreen parent = new JoinMultiplayerScreen(new TitleScreen());
        ServerData data = new ServerData(name, address, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(address), data, false, null);
    }
}
