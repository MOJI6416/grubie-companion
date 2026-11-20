package com.grubielauncher.companion.neoforge;

import com.grubielauncher.companion.Companion;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = Companion.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeEntry {
    public NeoForgeEntry(IEventBus modBus, ModContainer container) {
        Companion companion = Companion.init("neoforge", container.getModInfo().getVersion().toString());
        if (companion == null) return;
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(companion.panelKey()));
    }
}
