package com.grubielauncher.companion.loader.neoforge;

import com.grubielauncher.companion.Companion;
import net.neoforged.fml.common.Mod;

@Mod(Companion.MOD_ID)
public final class NeoForgeEntry {
    public NeoForgeEntry() {
        Companion.ensure();
    }
}
