package com.grubielauncher.companion.loader.forge;

import com.grubielauncher.companion.Companion;
import net.minecraftforge.fml.common.Mod;

@Mod(Companion.MOD_ID)
public final class ForgeEntry {
    public ForgeEntry() {
        Companion.ensure();
    }
}
