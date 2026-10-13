package dev.skyjet.config;

public final class AnimationSettings {
    public static final int TIME_WORLD = 0;
    public static final int TIME_REALTIME = 1;
    public static final int TIME_FIXED = 2;

    /**
     * Named for what they actually track. "World Time" on its own read as ambiguous next to a
     * real-world clock, so both are spelled out.
     */
    public static final String[] TIME_MODE_NAMES = {
            "Minecraft world time", "Real time (your computer's clock)", "Frozen at a set time"
    };

    /**
     * Local clock is the default: the sky follows the wall clock of the machine the game is
     * running on, so midnight in the room is midnight in the sky.
     */
    public int timeMode = TIME_REALTIME;
    public float speed = 1.0f;
    public float fixedTime = 0.0f;
    public boolean pauseInMenus = false;

    /** Shifts the local clock, in hours, for players who want the sky to run ahead or behind. */
    public float realTimeOffsetHours = 0.0f;
    /** 1 is a true 24 hour day. Higher values compress the local day into less real time. */
    public float realTimeScale = 1.0f;

    public boolean dayNightBlend = true;
    public float starFadeStart = 0.30f;
    public float starFadeEnd = 0.62f;
    public float nightBoost = 1.0f;

    public boolean sunFollowsWorld = true;
    public float sunAzimuthOffset = 0.0f;
    public float sunTilt = 0.0f;

    public boolean weatherReactive = true;
    public float rainDim = 0.55f;

    public AnimationSettings copy() {
        AnimationSettings a = new AnimationSettings();
        a.timeMode = timeMode;
        a.speed = speed;
        a.fixedTime = fixedTime;
        a.pauseInMenus = pauseInMenus;
        a.realTimeOffsetHours = realTimeOffsetHours;
        a.realTimeScale = realTimeScale;
        a.dayNightBlend = dayNightBlend;
        a.starFadeStart = starFadeStart;
        a.starFadeEnd = starFadeEnd;
        a.nightBoost = nightBoost;
        a.sunFollowsWorld = sunFollowsWorld;
        a.sunAzimuthOffset = sunAzimuthOffset;
        a.sunTilt = sunTilt;
        a.weatherReactive = weatherReactive;
        a.rainDim = rainDim;
        return a;
    }
}
