package dev.skyjet.render;

import dev.skyjet.config.AnimationSettings;
import dev.skyjet.config.SkyConfig;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Per-frame scalar state handed to the sky shader. Values are captured from the vanilla sky render
 * state (so they stay correct across dimensions and time-of-day packets) and then reshaped by the
 * active animation settings.
 */
public final class SkyState {
    public float camX;
    public float camY;
    public float camZ;
    public float time;
    public float sunX;
    public float sunY = 1.0f;
    public float sunZ;
    public float dayFraction;
    public float rain;
    public float thunder;
    public float starVisibility = 1.0f;
    public float starBoost = 1.0f;
    public float effectiveQuality = 3.0f;
    /** Day time in ticks, which is the clock sky pack fade stops and rotations are written in. */
    public float worldTicks;

    // Raw values captured from the vanilla extraction pass.
    public static volatile float vanillaSunAngle;
    public static volatile float vanillaStarBrightness;
    public static volatile float vanillaRain;
    public static volatile float vanillaThunder;
    public static volatile int vanillaSkyColor = 0xFF78A7FF;
    public static volatile boolean vanillaStateValid;

    private static long startNanos = System.nanoTime();
    private static double accumulated;
    private static long lastNanos = System.nanoTime();

    // Auto quality. A smoothed frame time drives the detail level down when the sky is expensive
    // and lets it climb back once there is headroom, so a heavy preset costs frames instead of
    // making the game unplayable.
    private static double smoothedFrameSeconds = 1.0 / 60.0;
    private static float qualityScale = 1.0f;

    // The system zone offset is cached because resolving it is far more work than reading a clock,
    // and it only ever changes at a daylight saving boundary.
    private static int zoneOffsetSeconds = Integer.MIN_VALUE;
    private static long zoneCheckedAtMillis;

    public static void resetClock() {
        startNanos = System.nanoTime();
        lastNanos = startNanos;
        accumulated = 0.0;
        smoothedFrameSeconds = 1.0 / 60.0;
        qualityScale = 1.0f;
    }

    public static void captureVanilla(float sunAngle, float starBrightness, float rain, float thunder,
                                      int skyColor) {
        vanillaSunAngle = sunAngle;
        vanillaStarBrightness = starBrightness;
        vanillaRain = rain;
        vanillaThunder = thunder;
        vanillaSkyColor = skyColor;
        vanillaStateValid = true;
    }

    private static double advanceClock(float speed, boolean paused) {
        long now = System.nanoTime();
        double delta = (now - lastNanos) / 1.0e9;
        lastNanos = now;
        if (delta < 0.0 || delta > 1.0) delta = 0.0;
        if (delta > 0.0) {
            smoothedFrameSeconds += (delta - smoothedFrameSeconds) * 0.08;
        }
        if (!paused) {
            accumulated += delta * speed;
        }
        return accumulated;
    }

    /**
     * The wall clock of the machine running the game, as a 0..1 day fraction in SkyJet's
     * convention: 0 is local noon, 0.25 local sunset, 0.5 local midnight.
     */
    public static float localClockDayFraction(float offsetHours, float scale) {
        long millis = System.currentTimeMillis();
        if (zoneOffsetSeconds == Integer.MIN_VALUE || millis - zoneCheckedAtMillis > 60_000L) {
            zoneCheckedAtMillis = millis;
            try {
                zoneOffsetSeconds = ZoneId.systemDefault().getRules()
                        .getOffset(Instant.ofEpochMilli(millis)).getTotalSeconds();
            } catch (Exception e) {
                zoneOffsetSeconds = 0;
            }
        }
        double localSeconds = millis / 1000.0 + zoneOffsetSeconds;
        double hours = (localSeconds / 3600.0 + offsetHours) * Math.max(0.0f, scale);
        double frac = hours / 24.0 - 0.5;
        frac -= Math.floor(frac);
        return (float) frac;
    }

    private static float resolveQuality(SkyConfig cfg) {
        int target = SkyConfig.clamp(cfg.quality, 1, 5);
        if (!cfg.autoQuality) {
            qualityScale = 1.0f;
            return target;
        }
        // 22ms and 13ms bracket a 45-75 fps band. Outside it the detail level walks by a small
        // step per frame, which keeps the sky from visibly popping between levels.
        if (smoothedFrameSeconds > 0.022) {
            qualityScale = Math.max(0.34f, qualityScale - 0.01f);
        } else if (smoothedFrameSeconds < 0.013) {
            qualityScale = Math.min(1.0f, qualityScale + 0.004f);
        }
        return Math.max(1.0f, Math.round(target * qualityScale));
    }

    public void update(SkyConfig cfg, double camX, double camY, double camZ, boolean paused) {
        AnimationSettings anim = cfg.animation;
        this.camX = (float) camX;
        this.camY = (float) camY;
        this.camZ = (float) camZ;

        float dayFrac;
        switch (anim.timeMode) {
            case AnimationSettings.TIME_FIXED -> dayFrac = anim.fixedTime;
            case AnimationSettings.TIME_REALTIME ->
                    dayFrac = localClockDayFraction(anim.realTimeOffsetHours, anim.realTimeScale);
            default -> dayFrac = vanillaStateValid ? vanillaSunAngle : 0.0f;
        }
        this.dayFraction = dayFrac;

        // Freezing the time of day must not freeze the clouds and stars as well; the animation
        // speed slider is what stops motion. The Layers tab's master speed scales all of it.
        float speed = anim.speed * Math.max(0.0f, cfg.layers.animationSpeed);
        this.time = (float) advanceClock(speed, paused && anim.pauseInMenus);
        // Sky pack layers fade and rotate on the day clock, which follows whichever time source
        // is active rather than the world's own, so an imported pack obeys the Animation tab.
        this.worldTicks = toTicks(dayFrac);
        this.effectiveQuality = resolveQuality(cfg);

        float sunFrac = anim.sunFollowsWorld ? dayFrac : (this.time * 0.01f);
        float angle = (float) ((sunFrac * 2.0 * Math.PI) + anim.sunAzimuthOffset);
        float tilt = anim.sunTilt;
        float sx = (float) -Math.sin(angle);
        float sy = (float) Math.cos(angle);
        float sz = 0.0f;
        if (tilt != 0.0f) {
            float ct = (float) Math.cos(tilt);
            float st = (float) Math.sin(tilt);
            float nx = sx * ct - sz * st;
            sz = sx * st + sz * ct;
            sx = nx;
        }
        float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0e-5f) len = 1.0f;
        this.sunX = sx / len;
        this.sunY = sy / len;
        this.sunZ = sz / len;

        this.rain = anim.weatherReactive ? vanillaRain * anim.rainDim : 0.0f;
        this.thunder = anim.weatherReactive ? vanillaThunder : 0.0f;

        if (anim.dayNightBlend) {
            float start = Math.min(anim.starFadeStart, anim.starFadeEnd);
            float end = Math.max(anim.starFadeStart, anim.starFadeEnd);
            float sunHeight = this.sunY * 0.5f + 0.5f;
            float t = clamp01((sunHeight - start) / Math.max(1.0e-4f, end - start));
            this.starVisibility = 1.0f - t;
        } else {
            this.starVisibility = 1.0f;
        }
        this.starVisibility *= (1.0f - 0.75f * this.rain);
        this.starBoost = anim.nightBoost;
    }

    public void updateForPreview(SkyConfig cfg, float previewTime, float previewDayFraction) {
        this.camX = 0.0f;
        this.camY = 80.0f;
        this.camZ = 0.0f;
        this.time = previewTime;
        this.dayFraction = previewDayFraction;
        // The preview always renders at the configured level, so what it shows is what the sky
        // looks like before auto quality steps in.
        this.effectiveQuality = SkyConfig.clamp(cfg.quality, 1, 5);
        this.worldTicks = toTicks(previewDayFraction);
        float angle = (float) (previewDayFraction * 2.0 * Math.PI + cfg.animation.sunAzimuthOffset);
        this.sunX = (float) -Math.sin(angle);
        this.sunY = (float) Math.cos(angle);
        this.sunZ = 0.0f;
        this.rain = 0.0f;
        this.thunder = 0.0f;
        if (cfg.animation.dayNightBlend) {
            float start = Math.min(cfg.animation.starFadeStart, cfg.animation.starFadeEnd);
            float end = Math.max(cfg.animation.starFadeStart, cfg.animation.starFadeEnd);
            float sunHeight = this.sunY * 0.5f + 0.5f;
            this.starVisibility = 1.0f - clamp01((sunHeight - start) / Math.max(1.0e-4f, end - start));
        } else {
            this.starVisibility = 1.0f;
        }
        this.starBoost = cfg.animation.nightBoost;
    }

    /**
     * SkyJet counts the day from noon, Minecraft counts it from sunrise. Sky pack fade stops are
     * written in Minecraft ticks, so they get the Minecraft clock.
     */
    public static float toTicks(float dayFraction) {
        float ticks = (dayFraction * 24000.0f + 6000.0f) % 24000.0f;
        return ticks < 0.0f ? ticks + 24000.0f : ticks;
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    public static float elapsedSeconds() {
        return (float) ((System.nanoTime() - startNanos) / 1.0e9);
    }
}
