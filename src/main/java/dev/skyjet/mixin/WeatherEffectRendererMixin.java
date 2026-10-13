package dev.skyjet.mixin;

import dev.skyjet.render.SkyJetRuntime;
import dev.skyjet.config.WorldSettings;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26 {
import net.minecraft.client.renderer.state.level.WeatherRenderState;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.WeatherRenderState;
*///?}

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
    @Unique
    private static void skyjet$filter(WeatherRenderState state, CallbackInfo ci) {
        if (!SkyJetRuntime.enabled()) return;
        WorldSettings w = SkyJetRuntime.world();
        if (w.disableRain) state.rainColumns.clear();
        if (w.disableSnow) state.snowColumns.clear();
        if (state.rainColumns.isEmpty() && state.snowColumns.isEmpty()) {
            ci.cancel();
        }
    }

    //? if >=26 {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void skyjet$render(Vec3 cameraPos, WeatherRenderState state, CallbackInfo ci) {
        skyjet$filter(state, ci);
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void skyjet$render(MultiBufferSource bufferSource, Vec3 cameraPos, WeatherRenderState state, CallbackInfo ci) {
        skyjet$filter(state, ci);
    }
    *///?}

    //? if <26.2 {
    /*@Inject(method = "tickRainParticles", at = @At("HEAD"), cancellable = true, require = 0)
    private void skyjet$tickRainParticles(net.minecraft.client.multiplayer.ClientLevel level,
                                          net.minecraft.client.Camera camera, int ticks,
                                          net.minecraft.server.level.ParticleStatus particleStatus,
                                          int renderDistance, CallbackInfo ci) {
        if (SkyJetRuntime.enabled() && SkyJetRuntime.world().disableWeatherParticles) {
            ci.cancel();
        }
    }
    *///?}
}
