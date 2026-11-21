package com.grubielauncher.companion.mixin;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.Settings;
import com.grubielauncher.companion.ui.PauseRow;
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
        if (!Settings.on(Settings.Option.PAUSE_ROW)) return;
        try {
            //? if >=1.17 {
            PauseRow.build(this, this.width, this.height, this.children(), button -> this.addRenderableWidget(button));
            //?} else
            /*PauseRow.build(this, this.width, this.height, this.children(), button -> this.addButton(button));*/
        } catch (RuntimeException e) {
            Companion.LOGGER.warn("Grubie Companion pause buttons failed", e);
        }
    }
}
