package dev.skyjet.mixin;

import dev.skyjet.render.SkyJetRuntime;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Unique
    private static boolean skyjet$hidden() {
        return SkyJetRuntime.enabled() && SkyJetRuntime.world().hideClouds();
    }

    //? if >=26 {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void skyjet$render(int color, CloudStatus status, float height, int renderDistance,
                               Vec3 cameraPos, long ticks, float partialTick, CallbackInfo ci) {
        if (skyjet$hidden()) ci.cancel();
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void skyjet$render(int color, CloudStatus status, float height,
                               Vec3 cameraPos, long ticks, float partialTick, CallbackInfo ci) {
        if (skyjet$hidden()) ci.cancel();
    }
    *///?}
}
