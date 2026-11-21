package com.grubielauncher.companion.mixin;

import com.grubielauncher.companion.Translations;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if <1.19.4
/*import net.minecraft.client.resources.language.LanguageInfo;*/

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {
    //? if >=1.19.4 {
    @Inject(method = "loadFrom", at = @At("RETURN"))
    private static void grubie$addTranslations(ResourceManager manager, List<String> languages, boolean rightToLeft, CallbackInfoReturnable<ClientLanguage> info) {
        grubie$merge(info.getReturnValue(), new ArrayList<>(languages));
    }
    //?} else {
    /*@Inject(method = "loadFrom", at = @At("RETURN"))
    private static void grubie$addTranslations(ResourceManager manager, List<LanguageInfo> languages, CallbackInfoReturnable<ClientLanguage> info) {
        List<String> codes = new ArrayList<>();
        for (LanguageInfo language : languages) codes.add(language.getCode());
        grubie$merge(info.getReturnValue(), codes);
    }
    *///?}

    private static void grubie$merge(ClientLanguage language, List<String> codes) {
        if (language == null) return;
        ClientLanguageAccessor accessor = (ClientLanguageAccessor) (Object) language;
        Map<String, String> merged = Translations.merge(accessor.grubie$storage(), codes);
        if (merged != null) accessor.grubie$setStorage(merged);
    }
}
