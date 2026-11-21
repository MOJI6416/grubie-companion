package com.grubielauncher.companion.render;

import net.minecraft.resources.ResourceLocation;

public final class Ids {
    public static final String NAMESPACE = "grubie_companion";

    private Ids() {
    }

    public static ResourceLocation of(String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
        //?} else
        /*return new ResourceLocation(NAMESPACE, path);*/
    }
}
