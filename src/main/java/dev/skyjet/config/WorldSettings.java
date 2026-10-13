package dev.skyjet.config;

public final class WorldSettings {
    public boolean disableRain = false;
    public boolean disableSnow = false;
    public boolean disableWeatherParticles = false;
    public boolean disableLightningFlash = false;

    /**
     * Master switch. When on, SkyJet owns the sky outright: every vanilla and resource pack sky
     * element below is forced off regardless of its own toggle. This is the default, because a
     * procedural sky with the vanilla sun still burning through it is nobody's idea of working.
     */
    public boolean takeOverSky = true;

    public boolean disableVanillaClouds = true;
    public boolean disableVanillaSun = true;
    public boolean disableVanillaMoon = true;
    public boolean disableVanillaStars = true;
    public boolean disableSunriseGlow = true;
    public boolean disableDarkDisc = true;

    public boolean overrideFogColor = false;
    public int fogColor = 0xFF101828;
    public float fogColorBlend = 1.0f;

    public boolean applyInOverworld = true;
    public boolean applyInNether = false;
    public boolean applyInEnd = true;
    public boolean applyInCustom = true;

    public boolean overrideResourcePackSkies = true;

    public boolean hideClouds() {
        return takeOverSky || disableVanillaClouds;
    }

    public boolean hideSun() {
        return takeOverSky || disableVanillaSun;
    }

    public boolean hideMoon() {
        return takeOverSky || disableVanillaMoon;
    }

    public boolean hideStars() {
        return takeOverSky || disableVanillaStars;
    }

    public boolean hideSunriseGlow() {
        return takeOverSky || disableSunriseGlow;
    }

    public boolean hideDarkDisc() {
        return takeOverSky || disableDarkDisc;
    }

    public boolean replaceBaseSky() {
        return takeOverSky || overrideResourcePackSkies;
    }

    public WorldSettings copy() {
        WorldSettings w = new WorldSettings();
        w.disableRain = disableRain;
        w.disableSnow = disableSnow;
        w.disableWeatherParticles = disableWeatherParticles;
        w.disableLightningFlash = disableLightningFlash;
        w.takeOverSky = takeOverSky;
        w.disableVanillaClouds = disableVanillaClouds;
        w.disableVanillaSun = disableVanillaSun;
        w.disableVanillaMoon = disableVanillaMoon;
        w.disableVanillaStars = disableVanillaStars;
        w.disableSunriseGlow = disableSunriseGlow;
        w.disableDarkDisc = disableDarkDisc;
        w.overrideFogColor = overrideFogColor;
        w.fogColor = fogColor;
        w.fogColorBlend = fogColorBlend;
        w.applyInOverworld = applyInOverworld;
        w.applyInNether = applyInNether;
        w.applyInEnd = applyInEnd;
        w.applyInCustom = applyInCustom;
        w.overrideResourcePackSkies = overrideResourcePackSkies;
        return w;
    }
}
