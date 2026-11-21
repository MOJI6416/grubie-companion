package com.grubielauncher.companion.mixin;

import com.grubielauncher.companion.Companion;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void grubie$tick(CallbackInfo info) {
        Companion companion = Companion.get();
        if (companion == null) return;
        try {
            companion.tick((Minecraft) (Object) this);
        } catch (RuntimeException e) {
            Companion.LOGGER.warn("Grubie Companion tick failed", e);
        }
    }
}
