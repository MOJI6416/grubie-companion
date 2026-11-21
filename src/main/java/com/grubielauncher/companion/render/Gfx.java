package com.grubielauncher.companion.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
*///?} elif >=1.21.6 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
*///?} elif >=1.21.2 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
*///?} elif >=1.20 {
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
*///?}

public final class Gfx {
    //? if >=26.1 {
    /*public final GuiGraphicsExtractor g;
    *///?} elif >=1.20 {
    public final GuiGraphics g;
    //?} else
    /*public final PoseStack g;*/

    private float alpha = 1f;

    //? if >=26.1 {
    /*public Gfx(GuiGraphicsExtractor g) {
    *///?} elif >=1.20 {
    public Gfx(GuiGraphics g) {
    //?} else
    /*public Gfx(PoseStack g) {*/
        this.g = g;
    }

    public void alpha(float value) {
        alpha = Math.max(0f, Math.min(1f, value));
    }

    public float alpha() {
        return alpha;
    }

    private int tint(int argb) {
        if (alpha >= 1f) return argb;
        int a = Math.round(((argb >>> 24) & 0xff) * alpha);
        return (a << 24) | (argb & 0xffffff);
    }

    public void fill(int x0, int y0, int x1, int y1, int argb) {
        if (x1 <= x0 || y1 <= y0) return;
        int color = tint(argb);
        if ((color >>> 24) == 0) return;
        //? if >=1.20 {
        g.fill(x0, y0, x1, y1, color);
        //?} else
        /*GuiComponent.fill(g, x0, y0, x1, y1, color);*/
    }

    public void blit(Tex tex, int x, int y, int w, int h, int u, int v, int uw, int vh, int argb) {
        if (w <= 0 || h <= 0 || tex == null) return;
        int color = tint(argb);
        if ((color >>> 24) == 0) return;
        //? if >=1.21.6 {
        /*g.blit(RenderPipelines.GUI_TEXTURED, tex.id(), x, y, u, v, w, h, uw, vh, tex.width(), tex.height(), color);
        *///?} elif >=1.21.2 {
        /*g.blit(RenderType::guiTextured, tex.id(), x, y, u, v, w, h, uw, vh, tex.width(), tex.height(), color);
        *///?} elif >=1.20 {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(((color >> 16) & 0xff) / 255f, ((color >> 8) & 0xff) / 255f, (color & 0xff) / 255f, ((color >>> 24) & 0xff) / 255f);
        g.blit(tex.id(), x, y, w, h, u, v, uw, vh, tex.width(), tex.height());
        g.setColor(1f, 1f, 1f, 1f);
        //?} elif >=1.17 {
        /*RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, tex.id());
        RenderSystem.setShaderColor(((color >> 16) & 0xff) / 255f, ((color >> 8) & 0xff) / 255f, (color & 0xff) / 255f, ((color >>> 24) & 0xff) / 255f);
        GuiComponent.blit(g, x, y, w, h, u, v, uw, vh, tex.width(), tex.height());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        *///?} else {
        /*RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Minecraft.getInstance().getTextureManager().bind(tex.id());
        RenderSystem.color4f(((color >> 16) & 0xff) / 255f, ((color >> 8) & 0xff) / 255f, (color & 0xff) / 255f, ((color >>> 24) & 0xff) / 255f);
        GuiComponent.blit(g, x, y, w, h, u, v, uw, vh, tex.width(), tex.height());
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        *///?}
    }

    public void push() {
        //? if >=1.21.6 {
        /*g.pose().pushMatrix();
        *///?} elif >=1.20 {
        g.pose().pushPose();
        //?} else
        /*g.pushPose();*/
    }

    public void pop() {
        //? if >=1.21.6 {
        /*g.pose().popMatrix();
        *///?} elif >=1.20 {
        g.pose().popPose();
        //?} else
        /*g.popPose();*/
    }

    public void translate(float x, float y) {
        //? if >=1.21.6 {
        /*g.pose().translate(x, y);
        *///?} elif >=1.20 {
        g.pose().translate(x, y, 0f);
        //?} else
        /*g.translate(x, y, 0);*/
    }

    public void scale(float s) {
        //? if >=1.21.6 {
        /*g.pose().scale(s, s);
        *///?} elif >=1.20 {
        g.pose().scale(s, s, 1f);
        //?} else
        /*g.scale(s, s, 1f);*/
    }

    public void lift() {
        //? if >=1.20 && <1.21.6 {
        g.pose().translate(0f, 0f, 1500f);
        //?} elif <1.20
        /*g.translate(0, 0, 980);*/
    }

    public void vanillaText(String text, int x, int y, int argb, boolean shadow) {
        int color = tint(argb);
        if ((color >>> 24) < 8) return;
        Font font = Minecraft.getInstance().font;
        //? if >=26.1 {
        /*g.text(font, text, x, y, color, shadow);
        *///?} elif >=1.20 {
        g.drawString(font, text, x, y, color, shadow);
        //?} else {
        /*if (shadow) font.drawShadow(g, text, x, y, color);
        else font.draw(g, text, x, y, color);
        *///?}
    }

    public static int vanillaWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }
}
