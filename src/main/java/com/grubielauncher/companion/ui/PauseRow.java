package com.grubielauncher.companion.ui;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.core.CompanionState;
import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.render.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PauseRow {
    private static final int HEIGHT = 20;
    private static final int GAP = 4;

    private PauseRow() {
    }

    public static void build(Screen screen, int width, int height, List<? extends GuiEventListener> children, Consumer<GrubieButton> add) {
        Companion companion = Companion.get();
        if (companion == null) return;
        Minecraft mc = Mc.mc();
        CompanionState.View view = companion.state().view();
        boolean singleplayer = Mc.isSingleplayer(mc);
        boolean hosting = view.host().active();
        boolean canShare = singleplayer && !hosting && view.connected();
        boolean canInvite = companion.canInvite(mc) && view.connected();

        int bottom = 0;
        int left = width;
        int right = 0;
        for (GuiEventListener child : children) {
            if (!(child instanceof AbstractWidget widget) || child instanceof GrubieButton) continue;
            bottom = Math.max(bottom, y(widget) + widget.getHeight());
            left = Math.min(left, x(widget));
            right = Math.max(right, x(widget) + widget.getWidth());
        }
        if (bottom == 0) {
            bottom = height / 2;
            left = width / 2 - 102;
            right = width / 2 + 102;
        }

        List<int[]> sizes = new ArrayList<>();
        List<GrubieButton.Painter> painters = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        List<String> names = new ArrayList<>();
        if (canShare) {
            String label = Mc.tr("grubie_companion.pause.share");
            sizes.add(new int[]{124});
            painters.add(painter(label, "radio", true, companion::sharePending));
            actions.add(() -> companion.share());
            names.add(label);
        }
        if (canInvite || hosting) {
            String label = Mc.tr("grubie_companion.pause.invite");
            sizes.add(new int[]{canShare ? 104 : 140});
            painters.add(painter(label, "user-plus", !canShare, () -> false));
            actions.add(() -> Mc.setScreen(mc, new PanelScreen(screen, Panel.Mode.INVITE, null)));
            names.add(label);
        }
        String key = companion.panelKeyName();
        sizes.add(new int[]{28});
        painters.add(keyPainter(key));
        actions.add(() -> Mc.setScreen(mc, new PanelScreen(screen, Panel.Mode.FRIENDS, null)));
        names.add("Grubie");

        int total = (sizes.size() - 1) * GAP;
        for (int[] size : sizes) total += size[0];
        int y = bottom + 18;
        if (y + HEIGHT > height - 4) y = Math.max(4, height - HEIGHT - 4);
        int x = width / 2 - total / 2;
        for (int i = 0; i < sizes.size(); i++) {
            int w = sizes.get(i)[0];
            GrubieButton button = new GrubieButton(x, y, w, HEIGHT, names.get(i), painters.get(i), actions.get(i));
            add.accept(button);
            if (i == 0) {
                int labelY = y - 10;
                int labelLeft = x;
                int labelRight = x + total;
                GrubieButton divider = new GrubieButton(labelLeft, labelY, labelRight - labelLeft, 8, "", divider(), () -> {
                });
                divider.active = false;
                add.accept(divider);
            }
            x += w + GAP;
        }
    }

    private static GrubieButton.Painter divider() {
        return (ui, x, y, w, h, hovered) -> {
            String label = "GRUBIE";
            float lw = ui.textWidth(label, Theme.TEXT_SM, Text.Weight.MEDIUM);
            float cy = y + h / 2f;
            ui.canvas.rect(x, cy, (w - lw) / 2f - 5f, 0.5f, 0x24FFFFFF);
            ui.canvas.rect(x + (w + lw) / 2f + 5f, cy, (w - lw) / 2f - 5f, 0.5f, 0x24FFFFFF);
            ui.textCentered(label, x + w / 2f, y + (h - ui.lineHeight(Theme.TEXT_SM)) / 2f, Theme.TEXT_SM, Text.Weight.MEDIUM, 0x8CFFFFFF);
        };
    }

    private interface Busy {
        boolean busy();
    }

    private static GrubieButton.Painter painter(String label, String icon, boolean primary, Busy busy) {
        return (ui, x, y, w, h, hovered) -> {
            float t = Anim.toward("pause:" + label, hovered ? 1f : 0f, Theme.FAST_MS);
            boolean working = busy.busy();
            int bg = primary ? Theme.mix(Theme.PRIMARY, Theme.PRIMARY_HOVER, t) : Theme.mix(0xF51C1D20, 0xF52A2B30, t);
            ui.canvas.shadow(x, y, w, h, Theme.RADIUS, 6f, 2f, primary ? Theme.withAlpha(Theme.PRIMARY, 0.4f) : 0x66000000);
            ui.canvas.round(x, y, w, h, Theme.RADIUS, working ? Theme.withAlpha(bg, 0.6f) : bg);
            if (!primary) ui.canvas.border(x, y, w, h, Theme.RADIUS, 0.5f, 0x1AFFFFFF);
            String text = working ? Mc.tr("grubie_companion.play.opening") : label;
            float iconSize = 8f;
            float lw = ui.textWidth(text, Theme.TEXT_MD, Text.Weight.MEDIUM);
            float cx = x + (w - lw - iconSize - 4f) / 2f;
            ui.icon(icon, cx, y + (h - iconSize) / 2f, iconSize, 0xFFFFFFFF);
            ui.text(text, cx + iconSize + 4f, y + (h - ui.lineHeight(Theme.TEXT_MD)) / 2f, Theme.TEXT_MD, Text.Weight.MEDIUM, 0xFFFFFFFF);
        };
    }

    private static GrubieButton.Painter keyPainter(String key) {
        return (ui, x, y, w, h, hovered) -> {
            float t = Anim.toward("pause:key", hovered ? 1f : 0f, Theme.FAST_MS);
            ui.canvas.shadow(x, y, w, h, Theme.RADIUS, 6f, 2f, 0x66000000);
            ui.canvas.round(x, y, w, h, Theme.RADIUS, Theme.mix(0xF51C1D20, 0xF52A2B30, t));
            ui.canvas.border(x, y, w, h, Theme.RADIUS, 0.5f, 0x1AFFFFFF);
            float kw = ui.keycapWidth(key, 9f);
            ui.keycap(key, x + (w - kw) / 2f, y + (h - 9f) / 2f, 9f);
        };
    }

    private static int x(AbstractWidget widget) {
        //? if >=1.19.3 {
        return widget.getX();
        //?} else
        /*return widget.x;*/
    }

    private static int y(AbstractWidget widget) {
        //? if >=1.19.3 {
        return widget.getY();
        //?} else
        /*return widget.y;*/
    }
}
