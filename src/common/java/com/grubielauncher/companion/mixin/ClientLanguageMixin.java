package com.grubielauncher.companion.mixin;

import com.grubielauncher.companion.Translations;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {
    @Inject(method = "loadFrom", at = @At("RETURN"))
    private static void grubie$addTranslations(
            ResourceManager manager,
            List<String> languages,
            boolean rightToLeft,
            CallbackInfoReturnable<ClientLanguage> info) {
        ClientLanguage language = info.getReturnValue();
        if (language == null) return;
        ClientLanguageAccessor accessor = (ClientLanguageAccessor) (Object) language;
        Map<String, String> merged = Translations.merge(accessor.grubie$storage(), languages);
        if (merged != null) accessor.grubie$setStorage(merged);
    }
}
