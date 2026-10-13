package dev.skyjet.mixin;

import dev.skyjet.render.SkyJetRuntime;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setSkyFlashTime", at = @At("HEAD"), cancellable = true)
    private void skyjet$blockLightningFlash(int time, CallbackInfo ci) {
        if (time > 0 && SkyJetRuntime.enabled() && SkyJetRuntime.world().disableLightningFlash) {
            ci.cancel();
        }
    }
}
