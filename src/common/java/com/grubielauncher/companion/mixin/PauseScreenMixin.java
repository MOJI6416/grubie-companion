package com.grubielauncher.companion.mixin;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.mc.Mc;
import com.grubielauncher.companion.mc.PanelScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    @Shadow
    @Final
    private boolean showPauseMenu;

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void grubie$init(CallbackInfo info) {
        Companion companion = Companion.get();
        if (companion == null || !this.showPauseMenu) return;
        companion.onPauseScreen();
        Screen self = this;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("grubie_companion.pause.button"),
                        button -> Mc.setScreen(this.minecraft, new PanelScreen(self)))
                .bounds(4, 4, 72, 20)
                .build());
    }
}
