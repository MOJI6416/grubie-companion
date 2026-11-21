package com.grubielauncher.companion.loader.fabric;

import com.grubielauncher.companion.Companion;
import net.fabricmc.api.ClientModInitializer;

public final class FabricEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Companion.ensure();
    }
}
