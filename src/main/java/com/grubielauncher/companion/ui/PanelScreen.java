package com.grubielauncher.companion.ui;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.render.Canvas;
import com.grubielauncher.companion.render.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else
/*import com.mojang.blaze3d.vertex.PoseStack;*/
//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

public final class PanelScreen extends Screen {
    private final Screen parent;
    private final Panel panel;
    private Ui ui;
    private float toUnits = 1f;
    private boolean closing;
    private long closeStart;

    public PanelScreen(Screen parent, Panel.Mode mode, String target) {
        super(Mc.literal("Grubie"));
        this.parent = parent;
        this.panel = new Panel(mode, target, this::onClose);
        Anim.set("panel:open", 0f);
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        draw(new Gfx(graphics), mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }
    *///?} elif >=1.20 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        draw(new Gfx(graphics), mouseX, mouseY);
    }
    //?} else {
    /*@Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        draw(new Gfx(pose), mouseX, mouseY);
    }
    *///?}

    //? if >=1.20.2 && <26.1 {
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
    //?}

    private void draw(Gfx gfx, int mouseX, int mouseY) {
        Anim.frame();
        Minecraft mc = Minecraft.getInstance();
        float unit = Mc.compactUnit(mc);
        toUnits = Mc.guiScale(mc) / unit;
        Canvas canvas = Canvas.begin(gfx, Mc.guiScale(mc), unit);
        try {
            ui = new Ui(canvas, mouseX * toUnits, mouseY * toUnits, !closing);
            float open = closing ? 1f - Anim.easeOut(Anim.progress(closeStart, Theme.NORMAL_MS)) : Anim.easeOut(Anim.toward("panel:open", 1f, Theme.SLOW_MS));
            panel.render(ui, this.width * toUnits, this.height * toUnits, open);
        } finally {
            canvas.end();
        }
        if (closing && Anim.now() - closeStart >= Theme.NORMAL_MS) {
            Anim.set("panel:open", 0f);
            Mc.setScreen(mc, parent);
        }
    }

    private boolean click(double x, double y, int button) {
        if (button != Mc.MOUSE_LEFT || ui == null || closing) return false;
        float ux = (float) x * toUnits;
        float uy = (float) y * toUnits;
        if (ui.press(ux, uy)) {
            Companion.clickSound();
            return true;
        }
        if (!panel.contains(ux, uy)) {
            onClose();
            return true;
        }
        return true;
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return click(event.x(), event.y(), event.button());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        Companion companion = Companion.get();
        if (companion != null && companion.panelKey().matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return click(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        Companion companion = Companion.get();
        if (companion != null && companion.panelKey().matches(key, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
    //?}

    //? if >=1.20.2 {
    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        panel.scroll((float) dy);
        return true;
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double x, double y, double delta) {
        panel.scroll((float) delta);
        return true;
    }
    *///?}

    @Override
    public void tick() {
        panel.tick();
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        closeStart = Anim.now();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
