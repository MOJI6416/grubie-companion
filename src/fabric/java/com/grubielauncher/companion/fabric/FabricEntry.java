package com.grubielauncher.companion.fabric;

import com.grubielauncher.companion.Companion;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        start();
    }

    public static Companion start() {
        String version = FabricLoader.getInstance()
                .getModContainer(Companion.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0");
        return Companion.init("fabric", version);
    }
}
