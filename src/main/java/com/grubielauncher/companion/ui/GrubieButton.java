package com.grubielauncher.companion.ui;

import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.render.Canvas;
import com.grubielauncher.companion.render.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
//? if >=1.17
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else
/*import com.mojang.blaze3d.vertex.PoseStack;*/
//? if >=1.21.9
/*import net.minecraft.client.input.InputWithModifiers;*/

public final class GrubieButton extends AbstractButton {
    public interface Painter {
        void paint(Ui ui, float x, float y, float w, float h, boolean hovered);
    }

    private final Painter painter;
    private final Runnable action;

    public GrubieButton(int x, int y, int w, int h, String narration, Painter painter, Runnable action) {
        super(x, y, w, h, Mc.literal(narration));
        this.painter = painter;
        this.action = action;
    }

    //? if >=1.21.9 {
    /*@Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }
    *///?} else {
    @Override
    public void onPress() {
        action.run();
    }
    //?}

    //? if >=26.1 {
    /*@Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        paint(new Gfx(graphics), mouseX, mouseY);
    }
    *///?} elif >=1.21.11 {
    /*@Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        paint(new Gfx(graphics), mouseX, mouseY);
    }
    *///?} elif >=1.20 {
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        paint(new Gfx(graphics), mouseX, mouseY);
    }
    //?} elif >=1.19.3 {
    /*@Override
    public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        paint(new Gfx(pose), mouseX, mouseY);
    }
    *///?} else {
    /*@Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        paint(new Gfx(pose), mouseX, mouseY);
    }
    *///?}

    //? if >=1.19.3 {
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
    //?} elif >=1.17 {
    /*@Override
    public void updateNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
    *///?}

    private void paint(Gfx gfx, int mouseX, int mouseY) {
        Canvas canvas = Canvas.begin(gfx, Mc.guiScale(Minecraft.getInstance()));
        try {
            Ui ui = new Ui(canvas, mouseX, mouseY, true);
            //? if >=1.18 {
            boolean hovered = this.isHoveredOrFocused();
            //?} else
            /*boolean hovered = this.isHovered();*/
            painter.paint(ui, x(), y(), this.width, this.height, hovered && this.active);
        } finally {
            canvas.end();
        }
    }

    private int x() {
        //? if >=1.19.3 {
        return this.getX();
        //?} else
        /*return this.x;*/
    }

    private int y() {
        //? if >=1.19.3 {
        return this.getY();
        //?} else
        /*return this.y;*/
    }
}
