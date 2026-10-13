package dev.skyjet.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.skyjet.render.SkyJetRenderer;
import dev.skyjet.render.SkyJetRuntime;
import dev.skyjet.render.SkyState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26 {
import net.minecraft.client.renderer.state.level.SkyRenderState;
//?} else {
/*import net.minecraft.client.renderer.state.SkyRenderState;
*///?}

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    //? if >=26.2 {
    @Shadow
    @Final
    private RenderTarget renderTarget;
    //?}

    @Unique
    private RenderTarget skyjet$target() {
        //? if >=26.2 {
        return this.renderTarget;
        //?} else {
        /*return Minecraft.getInstance().getMainRenderTarget();
        *///?}
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void skyjet$captureState(ClientLevel level, float partialTick, Camera camera,
                                     SkyRenderState state, CallbackInfo ci) {
        if (state.skybox == null || state.skybox == DimensionType.Skybox.NONE) return;
        // Vanilla stores the celestial angle in radians; SkyJet works in 0..1 day fractions.
        float dayFraction = (float) (state.sunAngle / (Math.PI * 2.0));
        dayFraction -= Math.floor(dayFraction);
        SkyState.captureVanilla(dayFraction, state.starBrightness, state.rainBrightness,
                level.getThunderLevel(partialTick), state.skyColor);
    }

    @Unique
    private void skyjet$drawSky() {
        RenderTarget target = skyjet$target();
        if (target == null) return;
        SkyJetRenderer.renderWorldSky(target.getColorTextureView(), target.getDepthTextureView());
    }

    /** When "replace base sky" is on we take over completely instead of painting over the vanilla disc. */
    @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
    private void skyjet$replaceSkyDisc(int skyColor, CallbackInfo ci) {
        if (!SkyJetRuntime.shouldRender()) return;
        if (!SkyJetRuntime.world().replaceBaseSky()) return;
        ci.cancel();
        skyjet$drawSky();
    }

    @Inject(method = "renderSkyDisc", at = @At("TAIL"))
    private void skyjet$renderSkyDisc(int skyColor, CallbackInfo ci) {
        if (!SkyJetRuntime.shouldRender()) return;
        if (SkyJetRuntime.world().replaceBaseSky()) return;
        skyjet$drawSky();
    }

    @Inject(method = "renderEndSky", at = @At("HEAD"), cancellable = true)
    private void skyjet$renderEndSky(CallbackInfo ci) {
        if (!SkyJetRuntime.shouldRender()) return;
        RenderTarget target = skyjet$target();
        if (target == null) return;
        ci.cancel();
        SkyJetRenderer.renderWorldSky(target.getColorTextureView(), target.getDepthTextureView());
    }

    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void skyjet$renderStars(float brightness, PoseStack poseStack, CallbackInfo ci) {
        if (SkyJetRuntime.shouldRender() && SkyJetRuntime.world().hideStars()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
    private void skyjet$renderSun(float brightness, PoseStack poseStack, CallbackInfo ci) {
        if (SkyJetRuntime.shouldRender() && SkyJetRuntime.world().hideSun()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
    private void skyjet$renderMoon(MoonPhase phase, float brightness, PoseStack poseStack, CallbackInfo ci) {
        if (SkyJetRuntime.shouldRender() && SkyJetRuntime.world().hideMoon()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
    private void skyjet$renderSunrise(PoseStack poseStack, float sunAngle, int color, CallbackInfo ci) {
        if (SkyJetRuntime.shouldRender() && SkyJetRuntime.world().hideSunriseGlow()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
    private void skyjet$renderDarkDisc(CallbackInfo ci) {
        if (SkyJetRuntime.shouldRender() && SkyJetRuntime.world().hideDarkDisc()) {
            ci.cancel();
        }
    }
}
