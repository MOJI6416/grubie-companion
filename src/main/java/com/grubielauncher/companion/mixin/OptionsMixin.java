package com.grubielauncher.companion.mixin;

import com.grubielauncher.companion.Companion;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "load", at = @At("HEAD"))
    private void grubie$addKeys(CallbackInfo info) {
        Companion companion = Companion.ensure();
        if (companion == null) return;
        List<KeyMapping> current = new ArrayList<>(Arrays.asList(keyMappings));
        boolean changed = false;
        for (KeyMapping key : new KeyMapping[]{companion.panelKey(), companion.acceptKey()}) {
            if (!current.contains(key)) {
                current.add(key);
                changed = true;
            }
        }
        if (changed) keyMappings = current.toArray(new KeyMapping[0]);
    }
}
