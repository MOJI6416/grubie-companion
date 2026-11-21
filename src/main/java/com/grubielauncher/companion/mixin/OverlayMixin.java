package com.grubielauncher.companion.mixin;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.render.Gfx;
import com.grubielauncher.companion.ui.PanelScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
/*import net.minecraft.client.gui.components.toasts.ToastManager;
*///?} else
import net.minecraft.client.gui.components.toasts.ToastComponent;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else
/*import com.mojang.blaze3d.vertex.PoseStack;*/

//? if >=1.21.2 {
/*@Mixin(ToastManager.class)
*///?} else
@Mixin(ToastComponent.class)
public abstract class OverlayMixin {
    //? if >=26.1 {
    /*@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void grubie$underPanel(GuiGraphicsExtractor graphics, CallbackInfo info) {
    *///?} elif >=1.20 {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void grubie$underPanel(GuiGraphics graphics, CallbackInfo info) {
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void grubie$underPanel(PoseStack graphics, CallbackInfo info) {
    *///?}
        if (!(Mc.screen(Mc.mc()) instanceof PanelScreen)) return;
        grubie$draw(new Gfx(graphics));
        info.cancel();
    }

    //? if >=26.1 {
    /*@Inject(method = "extractRenderState", at = @At("TAIL"))
    private void grubie$overlay(GuiGraphicsExtractor graphics, CallbackInfo info) {
    *///?} elif >=1.20 {
    @Inject(method = "render", at = @At("TAIL"))
    private void grubie$overlay(GuiGraphics graphics, CallbackInfo info) {
    //?} else {
    /*@Inject(method = "render", at = @At("TAIL"))
    private void grubie$overlay(PoseStack graphics, CallbackInfo info) {
    *///?}
        grubie$draw(new Gfx(graphics));
    }

    private static void grubie$draw(Gfx gfx) {
        Companion companion = Companion.get();
        if (companion == null) return;
        try {
            companion.renderOverlay(gfx);
        } catch (RuntimeException e) {
            Companion.LOGGER.warn("Grubie Companion overlay failed", e);
        }
    }
}
