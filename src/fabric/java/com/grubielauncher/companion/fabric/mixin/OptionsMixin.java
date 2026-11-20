package com.grubielauncher.companion.fabric.mixin;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.fabric.FabricEntry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "load", at = @At("HEAD"))
    private void grubie$addKeys(CallbackInfo info) {
        Companion companion = FabricEntry.start();
        if (companion == null) return;
        KeyMapping key = companion.panelKey();
        if (Arrays.asList(keyMappings).contains(key)) return;
        KeyMapping[] next = Arrays.copyOf(keyMappings, keyMappings.length + 1);
        next[keyMappings.length] = key;
        keyMappings = next;
    }
}
