package dev.skyjet.mixin;

import dev.skyjet.config.WorldSettings;
import dev.skyjet.render.SkyJetRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Unique
    private static void skyjet$tint(Vector4f color) {
        if (!SkyJetRuntime.shouldRender()) return;
        WorldSettings w = SkyJetRuntime.world();
        if (!w.overrideFogColor) return;
        float blend = Math.max(0.0f, Math.min(1.0f, w.fogColorBlend));
        float r = ((w.fogColor >> 16) & 0xFF) / 255.0f;
        float g = ((w.fogColor >> 8) & 0xFF) / 255.0f;
        float b = (w.fogColor & 0xFF) / 255.0f;
        color.x += (r - color.x) * blend;
        color.y += (g - color.y) * blend;
        color.z += (b - color.z) * blend;
    }

    //? if >=26 {
    @Inject(method = "computeFogColor", at = @At("TAIL"))
    private void skyjet$computeFogColor(Camera camera, float partialTick, ClientLevel level,
                                        int renderDistance, float darken, Vector4f out, CallbackInfo ci) {
        skyjet$tint(out);
    }
    //?} else {
    /*@Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private void skyjet$computeFogColor(Camera camera, float partialTick, ClientLevel level,
                                        int renderDistance, float darken, CallbackInfoReturnable<Vector4f> cir) {
        Vector4f value = cir.getReturnValue();
        if (value != null) {
            skyjet$tint(value);
            cir.setReturnValue(value);
        }
    }
    *///?}
}
