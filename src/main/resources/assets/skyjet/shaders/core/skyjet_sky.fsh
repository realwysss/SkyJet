#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

#define MAX_GRAD  8
#define MAX_STAR  6
#define MAX_NEB   4
#define MAX_AUR   3
#define MAX_BODY  6
#define MAX_SKY   6
#define MAX_CLOUD 3
#define STACK     9

#define PI  3.14159265359
#define TAU 6.28318530718

// Layer categories, matching dev.skyjet.config.LayerStack.
#define CAT_GRAD    0
#define CAT_SKY     1
#define CAT_ATMOS   2
#define CAT_NEB     3
#define CAT_STAR    4
#define CAT_BODY    5
#define CAT_SUNMOON 6
#define CAT_AUR     7
#define CAT_CLOUD   8

layout(std140) uniform SkyJetData {
    vec4 CamPos;
    vec4 SunDir;
    vec4 MoonDir;
    vec4 TimeInfo;
    vec4 Master;
    vec4 Counts;
    vec4 Counts2;
    vec4 Counts3;
    vec4 Atmos;
    vec4 AtmosTint;
    vec4 SunColor;
    vec4 MoonColor;
    vec4 GroundCol;
    vec4 HorizonCfg;
    vec4 FogCfg;
    vec4 MarkCfg;
    vec4 MasterTint;

    vec4 SkyC[MAX_SKY];
    vec4 SkyP[MAX_SKY];
    vec4 SkyQ[MAX_SKY];
    vec4 SkyR[MAX_SKY];
    vec4 SkyA[MAX_SKY];
    vec4 SkyF[MAX_SKY];
    vec4 SkyW[MAX_SKY];

    vec4 GradA[MAX_GRAD];
    vec4 GradB[MAX_GRAD];
    vec4 GradP[MAX_GRAD];
    vec4 GradQ[MAX_GRAD];

    vec4 StarC[MAX_STAR];
    vec4 StarC2[MAX_STAR];
    vec4 StarP[MAX_STAR];
    vec4 StarQ[MAX_STAR];
    vec4 StarR[MAX_STAR];
    vec4 StarS[MAX_STAR];
    vec4 StarT[MAX_STAR];

    vec4 NebC[MAX_NEB];
    vec4 NebC2[MAX_NEB];
    vec4 NebP[MAX_NEB];
    vec4 NebQ[MAX_NEB];

    vec4 AurC[MAX_AUR];
    vec4 AurC2[MAX_AUR];
    vec4 AurP[MAX_AUR];
    vec4 AurQ[MAX_AUR];
    vec4 AurR[MAX_AUR];

    vec4 BodyC[MAX_BODY];
    vec4 BodyC2[MAX_BODY];
    vec4 BodyP[MAX_BODY];
    vec4 BodyQ[MAX_BODY];
    vec4 BodyR[MAX_BODY];
    vec4 BodyS[MAX_BODY];

    vec4 CloudC[MAX_CLOUD];
    vec4 CloudC2[MAX_CLOUD];
    vec4 CloudP[MAX_CLOUD];
    vec4 CloudQ[MAX_CLOUD];
    vec4 CloudR[MAX_CLOUD];

    vec4 StackA[STACK];
    vec4 StackT[STACK];

    // Appended, not inserted next to AurR, so the borealis parameters did not shift every
    // array below the aurora. Order here must match the index constants in SkyUniforms.
    vec4 AurS[MAX_AUR];
    vec4 AurT[MAX_AUR];
    vec4 AurU[MAX_AUR];
    vec4 AurV[MAX_AUR];
    vec4 AurW[MAX_AUR];
};

uniform sampler2D NoiseSampler;
uniform sampler2D WatermarkSampler;
uniform sampler2D Skybox0Sampler;
uniform sampler2D Skybox1Sampler;
uniform sampler2D Skybox2Sampler;
uniform sampler2D Skybox3Sampler;
uniform sampler2D Skybox4Sampler;
uniform sampler2D Skybox5Sampler;

in vec3 rayDir;

out vec4 fragColor;

// ---------------------------------------------------------------- accessors

float sjTime()     { return CamPos.w; }
float sjRain()     { return TimeInfo.y; }
float sjThunder()  { return TimeInfo.z; }
float sjStarVis()  { return TimeInfo.w; }
int   sjQuality()  { return int(Counts2.w); }
float sjAA()       { return Counts3.y; }
float sjTicks()    { return Counts3.w; }

/** Angular size of one screen pixel, captured in main before any per-pixel branching. */
float gPixel = 1e-5;
/**
 * Extra octaves earned by zooming in. A fixed octave count just magnifies the same noise when the
 * field of view narrows, which is why a zoomed sky went soft and blocky.
 */
int gLod = 0;

/**
 * smoothstep whose edge is widened to at least one pixel using screen space derivatives.
 * Only safe in uniform control flow, so every call site sits above the per-pixel early outs.
 */
float aastepD(float edge, float v, float softness) {
    float w = softness;
    if (sjAA() > 0.5) {
        w = max(w, fwidth(v) * 0.5);
    }
    return smoothstep(edge - w, edge + w, v);
}

/** Same, with the pixel width supplied analytically for code that runs after an early out. */
float aastepW(float edge, float v, float softness, float halfWidth) {
    float w = sjAA() > 0.5 ? max(softness, halfWidth) : softness;
    return smoothstep(edge - w, edge + w, v);
}

vec3 celestialAxis(float tilt) {
    return normalize(vec3(sin(tilt), 0.0, cos(tilt)));
}

vec3 rotateAxis(vec3 v, vec3 axis, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return v * c + cross(axis, v) * s + axis * dot(axis, v) * (1.0 - c);
}

vec3 rotateY(vec3 v, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
}

vec3 rotateX(vec3 v, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
}

vec3 rotateZ(vec3 v, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

/**
 * Interleaved gradient noise. Neighbouring pixels get well spread values rather than independent
 * random ones, so using it to offset a short sample ladder reads as fine ordered dither instead
 * of the salt-and-pepper grain plain white noise leaves behind.
 */
float ign(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

// ------------------------------------------------------------------- noise

// Value noise read from a tiling lookup texture. One bilinear fetch replaces the eight hashes a
// lattice evaluation needed, which is what made nebula-heavy skies expensive.
#define NOISE_SIZE 256.0

float vnoise2(vec2 x) {
    vec2 p = floor(x);
    vec2 f = x - p;
    f = f * f * (3.0 - 2.0 * f);
    return texture(NoiseSampler, (p + f + 0.5) / NOISE_SIZE).b;
}

float vnoise3(vec3 x) {
    vec3 p = floor(x);
    vec3 f = x - p;
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = p.xy + vec2(37.0, 17.0) * p.z + f.xy;
    vec2 rg = texture(NoiseSampler, (uv + 0.5) / NOISE_SIZE).rg;
    return mix(rg.x, rg.y, f.z);
}

/**
 * Two independent smooth 3D fields from a single fetch. Domain warping used to cost two whole fbm
 * evaluations; this collapses it to one tap.
 *
 * <p>Only values that interpolate across the z slice are returned. The raw channels do not: the
 * lookup shifts by a whole (37, 17) texels each time the z slice changes, so anything read
 * straight out of the texture jumps at every integer z. Feeding such a value into a domain warp
 * drew a hard circular seam across the sky wherever a slice boundary crossed the view sphere.
 */
vec2 vnoise3x2(vec3 x) {
    vec3 p = floor(x);
    vec3 f = x - p;
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = p.xy + vec2(37.0, 17.0) * p.z + f.xy;
    vec4 t = texture(NoiseSampler, (uv + 0.5) / NOISE_SIZE);
    return vec2(mix(t.r, t.g, f.z), mix(t.b, t.a, f.z));
}

float fbm2(vec2 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;
        sum += vnoise2(p) * amp;
        norm += amp;
        p = p * 2.02 + vec2(11.7, 5.3);
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

/** fbm2 whose octaves drift on a circle, so cloud shapes churn instead of only sliding. */
float fbm2Evolving(vec2 p, int octaves, float evolve) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;
        float a = evolve + float(i) * 1.7;
        sum += vnoise2(p + vec2(cos(a), sin(a)) * 0.35) * amp;
        norm += amp;
        p = p * 2.02 + vec2(11.7, 5.3);
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

float fbm3(vec3 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 9; i++) {
        if (i >= octaves) break;
        sum += vnoise3(p) * amp;
        norm += amp;
        p = p * 2.03 + vec3(7.3, 2.9, 13.1);
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

float ridged3(vec3 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = 0; i < 9; i++) {
        if (i >= octaves) break;
        float n = 1.0 - abs(vnoise3(p) * 2.0 - 1.0);
        sum += n * n * amp;
        norm += amp;
        p = p * 2.07 + vec3(3.1, 9.7, 4.3);
        amp *= 0.5;
    }
    return sum / max(norm, 1e-4);
}

/** Blend modes, ordered to match SkyboxLayer.BLEND_NAMES. */
vec3 blendLayer(vec3 base, vec3 top, float alpha, int mode) {
    vec3 result;
    if (mode == 1) {
        result = base + top;
    } else if (mode == 2) {
        result = base * top;
    } else if (mode == 3) {
        result = 1.0 - (1.0 - base) * (1.0 - top);
    } else if (mode == 4) {
        result = mix(2.0 * base * top, 1.0 - 2.0 * (1.0 - base) * (1.0 - top), step(0.5, base));
    } else if (mode == 5) {
        result = mix(base, sqrt(max(base, 0.0)), top);
    } else if (mode == 6) {
        result = max(base, top);
    } else if (mode == 7) {
        result = max(base - top, vec3(0.0));
    } else if (mode == 8) {
        result = min(base / max(vec3(1.0) - top, vec3(0.004)), vec3(4.0));
    } else if (mode == 9) {
        result = 1.0 - min((vec3(1.0) - base) / max(top, vec3(0.004)), vec3(4.0));
    } else {
        // Normal, replace and alpha all resolve to a straight alpha composite.
        result = top;
    }
    return mix(base, result, alpha);
}

// --------------------------------------------------------- textured skybox

/**
 * Maps a direction onto the 3x2 cube sheet both sky pack formats use.
 *
 *   col 0        col 1       col 2
 *   BOTTOM(-Y)   TOP(+Y)     SOUTH(+Z)
 *   WEST(-X)     NORTH(-Z)   EAST(+X)
 *
 * The layout and each face's orientation were read off a shipping pack's own cube geometry, so an
 * OptiFine sky texture binds with no repacking at all. The uv is inset by half a texel so
 * bilinear filtering cannot bleed across a face seam.
 */
vec2 cubeSheetUv(vec3 d, vec2 texel) {
    vec3 a = abs(d);
    vec2 uv;
    vec2 cell;
    if (a.y >= a.x && a.y >= a.z) {
        float inv = 1.0 / max(a.y, 1e-6);
        if (d.y < 0.0) {
            uv = vec2(d.x * inv * 0.5 + 0.5, d.z * inv * 0.5 + 0.5);
            cell = vec2(0.0, 0.0);
        } else {
            uv = vec2(d.x * inv * 0.5 + 0.5, 0.5 - d.z * inv * 0.5);
            cell = vec2(1.0, 0.0);
        }
    } else if (a.z >= a.x) {
        float inv = 1.0 / max(a.z, 1e-6);
        if (d.z > 0.0) {
            uv = vec2(0.5 - d.x * inv * 0.5, 0.5 - d.y * inv * 0.5);
            cell = vec2(2.0, 0.0);
        } else {
            uv = vec2(d.x * inv * 0.5 + 0.5, 0.5 - d.y * inv * 0.5);
            cell = vec2(1.0, 1.0);
        }
    } else {
        float inv = 1.0 / max(a.x, 1e-6);
        if (d.x < 0.0) {
            uv = vec2(0.5 - d.z * inv * 0.5, 0.5 - d.y * inv * 0.5);
            cell = vec2(0.0, 1.0);
        } else {
            uv = vec2(d.z * inv * 0.5 + 0.5, 0.5 - d.y * inv * 0.5);
            cell = vec2(2.0, 1.0);
        }
    }

    vec2 cellSize = vec2(1.0 / 3.0, 0.5);
    vec2 inset = texel / cellSize;
    uv = clamp(uv, inset, 1.0 - inset);
    return cell * cellSize + uv * cellSize;
}

vec2 panoramaUv(vec3 d) {
    return vec2(atan(d.z, d.x) / TAU + 0.5, acos(clamp(d.y, -1.0, 1.0)) / PI);
}

/** Zenith-centred polar map, for overlay art authored as a single overhead disc. */
vec2 domeUv(vec3 d) {
    float r = acos(clamp(d.y, -1.0, 1.0)) / PI;
    float a = atan(d.z, d.x);
    return vec2(0.5 + cos(a) * r, 0.5 + sin(a) * r);
}

/**
 * Time of day fade, in ticks, matching the four stop form both pack formats use. The stops wrap
 * around midnight, so a night layer whose fade-in starts at 22000 and ends at 23500 works without
 * any special casing.
 */
float skyFade(vec4 f, float ticks) {
    float t = mod(ticks, 24000.0);
    float inSpan = mod(f.y - f.x + 24000.0, 24000.0);
    float outSpan = mod(f.w - f.z + 24000.0, 24000.0);
    float sinceIn = mod(t - f.x + 24000.0, 24000.0);
    float sinceOut = mod(t - f.z + 24000.0, 24000.0);
    float onSpan = mod(f.z - f.y + 24000.0, 24000.0);

    if (sinceIn < inSpan) return inSpan < 1.0 ? 1.0 : sinceIn / inSpan;
    if (mod(t - f.y + 24000.0, 24000.0) < onSpan) return 1.0;
    if (sinceOut < outSpan) return outSpan < 1.0 ? 0.0 : 1.0 - sinceOut / outSpan;
    return 0.0;
}

vec4 sampleSkybox(sampler2D tex, vec3 rd, int i) {
    vec4 C = SkyC[i];
    vec4 P = SkyP[i];
    vec4 Q = SkyQ[i];
    vec4 R = SkyR[i];
    vec4 A = SkyA[i];

    float alpha = C.a;
    if (A.w > 0.5) {
        alpha *= skyFade(SkyF[i], sjTicks());
    }
    vec4 W = SkyW[i];
    float weather = mix(W.x, W.y, clamp(sjRain(), 0.0, 1.0));
    weather = mix(weather, W.z, clamp(sjThunder(), 0.0, 1.0));
    alpha *= weather;
    alpha *= mix(1.0, sjStarVis(), Q.z);
    if (alpha <= 0.002) return vec4(0.0);

    vec3 d = rd;
    if (abs(P.y) > 0.0001) d = rotateY(d, P.y);
    if (abs(P.z) > 0.0001) d = rotateX(d, P.z);
    if (abs(P.w) > 0.0001) d = rotateZ(d, P.w);
    if (R.z > 0.5) {
        vec3 axis = A.xyz;
        float len = length(axis);
        axis = len < 1e-4 ? vec3(0.0, 0.0, 1.0) : axis / len;
        d = rotateAxis(d, axis, sjTicks() / 24000.0 * TAU * R.w);
    }

    int mode = int(P.x);
    vec4 texel;
    if (mode == 1 || mode == 2) {
        texel = texture(tex, cubeSheetUv(d, R.xy));
    } else if (mode == 3) {
        vec2 uv = domeUv(d);
        // The dome only covers the upper hemisphere; below it the layer contributes nothing.
        texel = texture(tex, clamp(uv, R.xy, 1.0 - R.xy)) * step(-0.02, d.y);
    } else {
        texel = texture(tex, panoramaUv(d));
    }

    vec3 col = texel.rgb * C.rgb * Q.y;
    alpha *= texel.a;
    if (Q.w > 0.5) {
        // Overlay art is usually authored on black rather than with an alpha channel.
        alpha *= clamp(max(max(texel.r, texel.g), texel.b) * 1.6, 0.0, 1.0);
    }
    return vec4(col, clamp(alpha, 0.0, 1.0));
}

/** Composites the skybox stack, returning its colour and how much of the sky it covers. */
vec4 allSkyboxes(vec3 rd) {
    int count = int(Counts3.x);
    vec3 col = vec3(0.0);
    float cover = 0.0;
    vec4 s;
    if (count > 0) {
        s = sampleSkybox(Skybox0Sampler, rd, 0);
        col = blendLayer(col, s.rgb, s.a, int(SkyQ[0].x));
        cover = cover + s.a * (1.0 - cover);
    }
    if (count > 1) {
        s = sampleSkybox(Skybox1Sampler, rd, 1);
        col = blendLayer(col, s.rgb, s.a, int(SkyQ[1].x));
        cover = cover + s.a * (1.0 - cover);
    }
    if (count > 2) {
        s = sampleSkybox(Skybox2Sampler, rd, 2);
        col = blendLayer(col, s.rgb, s.a, int(SkyQ[2].x));
        cover = cover + s.a * (1.0 - cover);
    }
    if (count > 3) {
        s = sampleSkybox(Skybox3Sampler, rd, 3);
        col = blendLayer(col, s.rgb, s.a, int(SkyQ[3].x));
        cover = cover + s.a * (1.0 - cover);
    }
    if (count > 4) {
        s = sampleSkybox(Skybox4Sampler, rd, 4);
        col = blendLayer(col, s.rgb, s.a, int(SkyQ[4].x));
        cover = cover + s.a * (1.0 - cover);
    }
    if (count > 5) {
        s = sampleSkybox(Skybox5Sampler, rd, 5);
        col = blendLayer(col, s.rgb, s.a, int(SkyQ[5].x));
        cover = cover + s.a * (1.0 - cover);
    }
    return vec4(col, cover);
}

// ------------------------------------------------------------- gradient sky

float gradientAxis(vec3 rd, int mode, float angle) {
    if (mode == 1) {
        return fract((atan(rd.z, rd.x) + angle) / TAU);
    } else if (mode == 2) {
        return clamp(dot(rd, normalize(SunDir.xyz)) * 0.5 + 0.5, 0.0, 1.0);
    } else if (mode == 3) {
        return clamp(dot(rd, normalize(vec3(sin(angle), cos(angle), 0.0))) * 0.5 + 0.5, 0.0, 1.0);
    } else if (mode == 4) {
        return clamp(1.0 - abs(rd.y), 0.0, 1.0);
    }
    float e = rd.y;
    e = sign(e) * pow(abs(e), max(0.05, HorizonCfg.z));
    return clamp(e * 0.5 + 0.5 + HorizonCfg.x, 0.0, 1.0);
}

vec4 applyGradients(vec3 rd) {
    int count = int(Counts.x);
    vec3 col = vec3(0.0);
    float cover = 0.0;
    for (int i = 0; i < MAX_GRAD; i++) {
        if (i >= count) break;
        vec4 P = GradP[i];
        vec4 Q = GradQ[i];
        float t = gradientAxis(rd, int(Q.w), Q.x);
        if (Q.y > 0.001) {
            // A single tap instead of a three octave fbm: the warp is low frequency anyway.
            vec2 n = vnoise3x2(rd * max(0.5, Q.z) * 6.0 + vec3(0.0, sjTime() * 0.01, 0.0));
            t += (n.x - 0.5) * Q.y;
        }
        // Narrow stops used to alias into a hard line; widen them to at least a pixel.
        float w = aastepD(P.x, t, max(0.0025, P.y));
        vec4 a = GradA[i];
        vec4 b = GradB[i];
        float alpha = clamp(mix(a.a, b.a, w) * P.z, 0.0, 1.0);
        col = blendLayer(col, mix(a.rgb, b.rgb, w), alpha, int(P.w));
        cover = cover + alpha * (1.0 - cover);
    }
    return vec4(col, cover);
}

// --------------------------------------------------------------- atmosphere

vec3 atmosphereColor(vec3 rd) {
    if (Counts2.z < 0.5) return vec3(0.0);
    vec3 sun = normalize(SunDir.xyz);
    float cosT = dot(rd, sun);
    float rayleighPhase = 0.75 * (1.0 + cosT * cosT);
    float g = clamp(Atmos.z, -0.95, 0.95);
    float denom = 1.0 + g * g - 2.0 * g * cosT;
    float miePhase = min((1.0 - g * g) / (4.0 * PI * pow(max(denom, 1e-4), 1.5)), 6.0);

    // Mirrored about the horizon so the lower hemisphere keeps the same air, rather than
    // clamping to a constant and reading as a flat floor.
    float h = abs(rd.y);
    float airMass = 1.0 / (h * 0.85 + 0.18);
    float mieMass = 1.0 / (h * 1.40 + 0.22);
    float horizonT = clamp((airMass - 0.97) / 4.6, 0.0, 1.0);

    vec3 rayleighTint = mix(vec3(0.16, 0.38, 0.92), vec3(0.72, 0.84, 1.0), horizonT * 0.75);
    vec3 col = rayleighTint * rayleighPhase * Atmos.x * airMass * 0.30;
    col += SunColor.rgb * miePhase * Atmos.y * mieMass * 0.05;

    float sunUp = smoothstep(-0.30, 0.22, sun.y);
    float sunset = smoothstep(0.34, -0.05, abs(sun.y)) * max(0.0, cosT * 0.5 + 0.5);
    col += SunColor.rgb * exp(-h * 7.0) * sunset * AtmosTint.a * 0.38;

    col *= AtmosTint.rgb;
    col *= mix(0.02, 1.0, sunUp);
    col *= Atmos.w;
    col *= mix(1.0, 0.45, sjRain());
    // Below the horizon the sky is the same sky, just a little dimmer for being under you.
    col *= mix(1.0, 0.82, clamp(-rd.y * 2.0, 0.0, 1.0));
    return max(col, vec3(0.0));
}

// -------------------------------------------------------------------- stars

/** Masks for the non-round star shapes: blocky particles, crosses, rings and diamonds. */
float starFigure(vec2 d, float radius, int shape) {
    if (shape == 4 || shape == 8) {
        float m = max(abs(d.x), abs(d.y));
        return 1.0 - smoothstep(radius * 0.72, radius * 0.95, m);
    } else if (shape == 5) {
        float bar = min(abs(d.x), abs(d.y));
        float len = max(abs(d.x), abs(d.y));
        return (1.0 - smoothstep(radius * 0.18, radius * 0.34, bar))
             * (1.0 - smoothstep(radius * 1.6, radius * 2.2, len));
    } else if (shape == 6) {
        float r = length(d);
        return (1.0 - smoothstep(radius * 0.9, radius * 1.15, r))
             * smoothstep(radius * 0.45, radius * 0.7, r);
    } else if (shape == 7) {
        float m = abs(d.x) + abs(d.y);
        return 1.0 - smoothstep(radius * 0.7, radius * 1.0, m);
    }
    return 0.0;
}

/**
 * Contribution of one lattice cell to a star field.
 *
 * pixelRadius is the angular size of a screen pixel expressed in cell units. Any star smaller
 * than that is widened to a pixel and dimmed by the area it gained, which is what keeps a field
 * visible at normal field of view instead of only when the camera zooms in.
 */
vec3 starCell(vec3 cell, vec3 p, int i, float pixelRadius, float sparkleSeed) {
    vec4 P = StarP[i];
    vec4 Q = StarQ[i];
    vec4 R = StarR[i];
    vec4 S = StarS[i];
    vec4 T = StarT[i];

    // The seed shifts the lattice into a different part of the hash space, so two layers stop
    // landing their stars in the same places and the sky stops reading as one repeated pattern.
    vec3 rnd = hash33(cell + S.w);
    if (rnd.z > clamp(0.05 + P.x * 0.55, 0.0, 0.95)) return vec3(0.0);

    float jit = clamp(T.x, 0.0, 1.0);
    vec3 scatter = vec3(rnd.x, rnd.y, fract(rnd.x * 31.7 + rnd.y * 17.3)) * 0.62 + 0.19;
    vec3 jitter = mix(vec3(0.5), scatter, jit);
    vec3 delta = p - (cell + jitter);
    float dist = length(delta);

    float mag = mix(1.0 - S.z * 0.75, 1.0, rnd.x * rnd.y);
    float sparkle = step(1.0 - S.y, fract(rnd.y * 71.3 + sparkleSeed));
    mag *= mix(1.0, 2.1, sparkle);

    float radius = max(1e-4, P.y * (0.05 + 0.16 * mag));

    float energy = 1.0;
    // Capped to a little under half a cell: only the eight surrounding cells are sampled, so a
    // star wider than that would be clipped at the lattice boundary and show as a square again.
    float minRadius = min(pixelRadius * max(0.35, R.y), 0.45);
    if (minRadius > radius) {
        energy = (radius * radius) / (minRadius * minRadius);
        radius = minRadius;
    }

    int shape = int(Q.w);
    float core;
    if (shape >= 4) {
        core = starFigure(delta.xy, radius * 1.35, shape) * energy;
        if (shape == 8) {
            // Cube: two lit faces, so it reads as a solid block rather than a flat square.
            float shade = 0.7 + 0.3 * step(0.0, delta.x) + 0.18 * step(0.0, delta.y);
            core *= clamp(shade, 0.0, 1.3);
        }
    } else {
        core = smoothstep(radius, radius * 0.35, dist);
        core = core * core * energy;
    }

    // The halo is bounded inside the cell so neighbouring cells can never clip it into a square.
    float haloR = min(radius * (2.4 + R.x * 6.0), 0.46);
    core += pow(max(0.0, 1.0 - dist / haloR), 3.0) * (0.18 + R.x * 0.5) * energy;

    if (shape == 1) {
        float spike = pow(max(0.0, 1.0 - abs(delta.x) / min(radius * 5.0, 0.44)), 8.0)
                    + pow(max(0.0, 1.0 - abs(delta.y) / min(radius * 5.0, 0.44)), 8.0);
        core += spike * smoothstep(0.46, 0.0, dist) * 0.35 * energy;
    } else if (shape == 2) {
        core += pow(max(0.0, 1.0 - dist / min(radius * 9.0, 0.46)), 2.0) * 0.30 * energy;
    } else if (shape == 3) {
        float ang = atan(delta.y, delta.x);
        float petal = pow(abs(cos(ang * 2.0)), 6.0);
        core += petal * pow(max(0.0, 1.0 - dist / min(radius * 7.0, 0.46)), 4.0) * 0.55 * energy;
    }

    float twinkle = 1.0;
    if (Q.y > 0.001 && T.y < 0.5) {
        twinkle = mix(1.0 - Q.y, 1.0 + Q.y * 0.6,
                      sin(sjTime() * Q.x * 1.3 + rnd.y * TAU) * 0.5 + 0.5);
    }

    vec4 C = StarC[i];
    vec3 tint = mix(C.rgb, StarC2[i].rgb, rnd.y);
    vec3 variance = normalize(mix(vec3(1.0), hash33(cell + 7.77 + S.w) * 1.4 + 0.3, C.a));
    return tint * variance * 1.732 * core * mag * twinkle * P.z;
}

vec3 starLayer(vec3 rd, int i, float pixelAngle) {
    vec4 P = StarP[i];
    vec4 Q = StarQ[i];
    vec4 R = StarR[i];
    vec4 S = StarS[i];
    vec4 T = StarT[i];
    if (P.z <= 0.001) return vec3(0.0);

    // Frozen fields keep their placement but stop moving entirely.
    float motion = T.y > 0.5 ? 0.0 : sjTime();

    vec3 sd = rotateAxis(rd, celestialAxis(R.z), motion * Q.z * 0.01);
    if (abs(T.z) > 0.001) {
        sd = rotateY(sd, motion * T.z * 0.02);
    }
    sd = normalize(sd - CamPos.xyz * (P.w * 2.0e-5));

    float band = 1.0;
    if (abs(R.w) > 0.001) {
        band = exp(-pow(abs(sd.y - R.w) * max(0.2, S.x), 2.0)) * 0.85 + 0.15;
    }

    float scale = 30.0 + P.x * 420.0;
    vec3 p = sd * scale;
    vec3 base = floor(p);
    vec3 frac = p - base;

    float pixelRadius = pixelAngle * scale;
    float sparkleSeed = float(i) * 3.77;

    // Two cells per axis. A single cell clipped every halo at the lattice boundary, which is
    // what made dense fields look like a grid of squares instead of stars.
    vec3 step3 = step(vec3(0.5), frac) * 2.0 - 1.0;
    vec3 sum = vec3(0.0);
    for (int ox = 0; ox < 2; ox++) {
        for (int oy = 0; oy < 2; oy++) {
            for (int oz = 0; oz < 2; oz++) {
                vec3 cell = base + vec3(float(ox), float(oy), float(oz)) * step3;
                sum += starCell(cell, p, i, pixelRadius, sparkleSeed);
            }
        }
    }
    return sum * band;
}

vec3 allStars(vec3 rd, float pixelAngle) {
    float vis = sjStarVis();
    if (vis < 0.004) return vec3(0.0);
    int count = int(Counts.y);
    vec3 sum = vec3(0.0);
    for (int i = 0; i < MAX_STAR; i++) {
        if (i >= count) break;
        sum += starLayer(rd, i, pixelAngle);
    }
    return sum * vis;
}

// ------------------------------------------------------------ nebula/galaxy

vec3 nebulaLayer(vec3 rd, int i) {
    vec4 C = NebC[i];
    vec4 P = NebP[i];
    vec4 Q = NebQ[i];
    if (C.a <= 0.001) return vec3(0.0);

    int oct = clamp(int(P.w) + sjQuality() - 3 + gLod, 2, 8);
    vec3 nd = rotateAxis(rd, celestialAxis(0.0), sjTime() * Q.x * 0.004);
    nd = normalize(nd - CamPos.xyz * (P.z * 1.2e-5));

    vec3 p = nd * max(0.15, P.x);
    // One tap gives both warp axes and the colour mix seed. This used to be two three-octave
    // fbm evaluations, six texture fetches, per layer per pixel.
    vec2 w = vec2(0.5);
    if (Q.y > 0.001) {
        w = vnoise3x2(p * 0.7 + 11.3);
        // The third warp axis is the mean of the other two, which is what the original two-fbm
        // version did as well; a warp does not need a third independent field to look right.
        p += (vec3(w, (w.x + w.y) * 0.5) - 0.5) * Q.y * 4.0;
    }

    // The elevation mask is kept out of the density that gets thresholded. Folding it in first
    // meant the contrast threshold cut the smooth falloff into a hard contour, which showed as a
    // crisp arc drawn across the sky wherever a band was tight or contrasty.
    float density;
    float mask = 1.0;
    int shape = int(Q.w);
    if (shape == 1) {
        density = fbm3(p, oct);
        mask = exp(-pow(abs(nd.y - Q.z) * 2.6, 2.0));
    } else if (shape == 2) {
        float spiral = sin(atan(nd.z, nd.x) * 2.0 + acos(clamp(nd.y, -1.0, 1.0)) * 5.5) * 0.5 + 0.5;
        density = fbm3(p, oct) * mix(0.35, 1.0, spiral);
        mask = exp(-pow(abs(nd.y - Q.z) * 2.2, 2.0));
    } else if (shape == 3) {
        density = ridged3(p, oct);
    } else {
        density = fbm3(p, oct);
        mask = mix(1.0, smoothstep(-0.2, 0.75, nd.y - Q.z), 0.55);
    }

    float low = clamp(0.62 - P.y * 0.42, 0.02, 0.92);
    density = smoothstep(low, min(1.0, low + 0.52), clamp(density, 0.0, 1.0)) * mask;
    if (density <= 0.002) return vec3(0.0);

    vec3 color = mix(C.rgb, NebC2[i].rgb, clamp(w.x * 0.6 + w.y * 0.4, 0.0, 1.0));
    return color * density * C.a * 1.6 * sjStarVis();
}

vec3 allNebulae(vec3 rd) {
    if (sjStarVis() < 0.004) return vec3(0.0);
    int count = int(Counts.z);
    vec3 sum = vec3(0.0);
    for (int i = 0; i < MAX_NEB; i++) {
        if (i >= count) break;
        sum += nebulaLayer(rd, i);
    }
    return sum;
}

// ------------------------------------------------------------------- aurora

#define AURORA_MAX_DIST 11000.0

/**
 * Aurora curtains, sampled through a real volume rather than on one surface.
 *
 * Height samples walk up the curtain, and a small number of bands sit at different depths along
 * the ray. Each band is dimmed by how far back it is, so the curtain has a near face and a far
 * one instead of reading as a decal painted flat onto the sky.
 */
/** Smooth 1D value noise. Used along the arc, where a 2D fetch would buy nothing. */
float vnoise1(float x) {
    float i0 = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash12(vec2(i0, 11.7));
    float b = hash12(vec2(i0 + 1.0, 11.7));
    return mix(a, b, f);
}

float fbm1(float x, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    for (int o = 0; o < 5; o++) {
        if (o >= octaves) break;
        sum += vnoise1(x) * amp;
        x = x * 2.03 + 3.1;
        amp *= 0.5;
    }
    return sum;
}

/**
 * Aurora borealis.
 *
 * A real display is a curtain hanging off an arc in the auroral oval, a few kilometres thick and
 * hundreds of kilometres tall. Three things make it read as the real thing rather than as a
 * green smear:
 *
 *  - The lower edge is sharp and the top is diffuse, because the curtain stops dead where the
 *    atmosphere gets thick enough to quench the emission.
 *  - The colour follows altitude, not the artist's gradient: N2+ pink in the bottom fringe,
 *    557.7nm oxygen green through the body, 630nm oxygen red up top where the air is thin.
 *  - The rays are vertical and hold still as the curtain folds past them, because they run along
 *    magnetic field lines. Sampling the striation on the along-arc coordinate only, with no
 *    height term, is what gives that, and perspective then converges them toward the zenith.
 *
 * The curtain is a Gaussian sheet around a circle of radius S.y, folded sideways by noise in the
 * along-arc coordinate. Marching walks altitude rather than distance, so every step lands on the
 * curtain instead of most of them falling through empty sky.
 */
vec3 auroraBorealis(vec3 rd, int i) {
    vec4 C  = AurC[i];   // body colour, .a intensity
    vec4 C2 = AurC2[i];  // top colour, .a style
    vec4 P  = AurP[i];   // base height, top height, speed, steps
    vec4 Q  = AurQ[i];   // (veil), (veil), (veil), ground glow
    vec4 R  = AurR[i];   // (veil), depth fade, bands, band spacing
    vec4 S  = AurS[i];   // arc azimuth, arc distance, arc width, curvature
    vec4 T  = AurT[i];   // fold amount, fold scale, fold speed, ray amount
    vec4 U  = AurU[i];   // ray scale, ray sharpness, bottom sharpness, top falloff
    vec4 V  = AurV[i];   // bottom colour, .a drift
    vec4 W  = AurW[i];   // pulse, pulse speed

    if (C.a <= 0.001 || rd.y <= 0.002) return vec3(0.0);

    float t = sjTime() * P.z;
    float h0 = P.x;
    float h1 = P.y;

    // The arc as a circle the camera sits inside. A large radius reads as a straight band across
    // the sky; shrinking it wraps the arc overhead, which is what a display directly above looks
    // like from under the oval.
    float az = S.x + t * V.a * 0.02;
    float radius = max(60.0, S.y / max(0.04, S.w));
    vec2 centre = -vec2(sin(az), cos(az)) * (radius - S.y);
    float width = max(12.0, S.z);

    // Cheap reject. If the ray stays on one side of the arc radius between the curtain's foot and
    // its top, nothing on this pixel can be lit, and most of the sky leaves here.
    float dLow = h0 / rd.y;
    float dHigh = min(h1 / rd.y, AURORA_MAX_DIST);
    float nLow = length(rd.xz * dLow - centre) - radius;
    float nHigh = length(rd.xz * dHigh - centre) - radius;
    int bands = clamp(int(R.z), 1, 3);
    float slack = width * (3.0 + T.x * 4.0 + max(0.0, R.w) * 3.0 * float(bands));
    if (min(nLow, nHigh) > slack || max(nLow, nHigh) < -slack) return vec3(0.0);

    int samples = clamp(int(P.w) * sjQuality() / 3, 6, 16);
    float invSamples = 1.0 / float(samples);
    float foldScale = 0.0018 * max(0.05, T.y);
    float rayScale = 0.011 * max(0.05, U.x);
    float raySharp = 1.0 + max(0.0, U.y) * 5.0;
    float rayAmount = clamp(T.z, 0.0, 1.0);

    // Pre-filtering, which is what keeps this smooth instead of dithered.
    //
    // Marching a thin sheet with a handful of jittered samples means neighbouring pixels land on
    // different parts of a steep function, and the jitter pattern itself shows up as a hatch
    // across the sky. Rather than throwing samples at it, each sample is widened to cover the
    // span it actually represents, so the result barely depends on where inside its step the
    // sample fell.
    //
    // How far the crossing distance moves per step is already known from the reject test.
    float stepAcross = abs(nHigh - nLow) * invSamples;
    float filterWidth = sqrt(width * width + stepAcross * stepAcross * 0.30);

    // Same idea for the rays: once one step slides further along the arc than half a ray is
    // wide, that detail cannot be resolved and is faded out rather than aliased. The span is
    // taken from the two ends of the march directly, because estimating it from the distance
    // travelled overshoots wildly for rays that run nearly tangent to the arc.
    vec2 endLow = rd.xz * dLow - centre;
    vec2 endHigh = rd.xz * dHigh - centre;
    float alongStep = abs(atan(endHigh.y, endHigh.x) - atan(endLow.y, endLow.x));
    alongStep = min(alongStep, TAU - alongStep) * radius * invSamples;
    rayAmount *= clamp(1.0 - alongStep * rayScale * 1.6, 0.0, 1.0);

    // Substorms brighten the whole display in slow waves rather than per pixel.
    float pulse = 1.0 + W.x * (sin(t * W.y * 1.7) * 0.6 + sin(t * W.y * 0.63 + 1.9) * 0.4);

    // An even ladder puts every pixel's samples at the same altitudes, which reads as banding.
    float jitter = ign(gl_FragCoord.xy);

    vec3 acc = vec3(0.0);
    float ground = 0.0;

    for (int sIdx = 0; sIdx < 16; sIdx++) {
        if (sIdx >= samples) break;
        float u = (float(sIdx) + jitter) * invSamples;
        float h = mix(h0, h1, u);
        float dist = min(h / rd.y, AURORA_MAX_DIST);
        vec2 pos = rd.xz * dist;

        vec2 toArc = pos - centre;
        float across = length(toArc) - radius;
        // Arc length along the curtain. Taking it from the angle keeps a ray anchored to the arc
        // instead of sliding along it as the sample distance changes with height.
        float along = atan(toArc.y, toArc.x) * radius;

        // Folds travel along the arc and lean with height, which is what curls a quiet arc into
        // the spirals of an active one.
        float fold = (fbm1(along * foldScale + t * T.w * 0.35, 3) - 0.5) * 2.0
                   + (fbm1(along * foldScale * 2.7 - t * T.w * 0.21, 2) - 0.5) * 0.8;
        fold *= T.x * width * 3.2 * (0.55 + 0.75 * u);

        // Vertical rays. No height term, so a ray holds still up the whole curtain the way it
        // does along a field line; perspective then converges them toward the magnetic zenith.
        float rayN = fbm1(along * rayScale + float(i) * 37.0, 3);
        float rays = mix(1.0, pow(clamp(rayN * 1.4, 0.0, 1.0), raySharp) * 1.8, rayAmount);

        float sheet = 0.0;
        for (int b = 0; b < 3; b++) {
            if (b >= bands) break;
            float slot = float(b) - (float(bands) - 1.0) * 0.5;
            float n = (across - fold - slot * width * max(0.0, R.w) * 3.0) / filterWidth;
            // Gaussian across the face: a real curtain has no edge, it just thins out. Narrowed
            // by the filter width so a widened sample still carries the same total light.
            sheet += exp(-n * n) * (width / filterWidth)
                   * mix(1.0, 1.0 - R.y * 0.6, abs(slot) / max(1.0, float(bands)));
        }
        sheet /= float(bands);

        // Hard bottom, long soft top. This profile is most of what sells it as the real thing.
        // The edge is never allowed to be sharper than one step, or it aliases into the hatch
        // the sample widening above exists to remove.
        float edge = max(invSamples * 1.5, mix(0.35, 0.03, clamp(U.z, 0.0, 1.0)));
        float bottom = smoothstep(0.0, edge, u);
        float top = exp(-u * (0.8 + max(0.0, U.w) * 5.0));
        float vertical = bottom * top;

        float fade = 1.0 / (1.0 + dist * 0.00012 * (0.4 + R.y));

        // Altitude colour ramp: N2+ pink in the bottom fringe, 557.7nm green through the body,
        // 630nm red up top where the air is thin enough for the slow transition to survive.
        vec3 tint = mix(V.rgb, C.rgb, smoothstep(0.0, 0.22, u));
        tint = mix(tint, C2.rgb, smoothstep(0.38, 1.0, u));

        float e = sheet * rays * vertical * fade;
        acc += tint * e;
        ground += e * (1.0 - u);
    }

    acc *= invSamples * pulse * smoothstep(0.004, 0.09, rd.y);

    if (Q.w > 0.001) {
        // Airglow along the horizon under the arc, the way a bright display lights the sky below.
        acc += C.rgb * exp(-abs(rd.y) * 12.0) * smoothstep(-0.1, 0.06, rd.y)
             * ground * invSamples * Q.w * 1.4;
    }
    return acc * C.a * 9.0;
}

vec3 auroraVeil(vec3 rd, int i) {
    vec4 C = AurC[i];
    vec4 P = AurP[i];
    vec4 Q = AurQ[i];
    vec4 R = AurR[i];
    if (C.a <= 0.001 || rd.y <= 0.0015) return vec3(0.0);

    float t = sjTime() * P.z;
    float h0 = P.x;
    float h1 = max(P.y, P.x + 8.0);

    // Coverage varies slowly, so one probe at the mid height decides whether this ray has a
    // curtain in front of it at all. Most of the sky leaves here.
    float midDist = min((h0 + h1) * 0.5 / max(rd.y, 0.012), AURORA_MAX_DIST);
    vec2 midPos = (rd.xz * midDist + CamPos.xz) * 0.00085;
    float cov = smoothstep(0.42 - Q.z * 0.42, 0.78, fbm2(midPos * 0.28 + vec2(t * 0.02, 0.0), 2));
    if (cov <= 0.004) return vec3(0.0);

    float warp = fbm2(midPos * 0.65 + vec2(t * 0.11, 0.0), 2) * Q.x * 2.4;

    int samples = clamp(int(P.w * float(sjQuality()) / 3.0), 4, 12);
    // Two octaves more than before, and they keep climbing as the view narrows. The old ceiling
    // of 5 is what made a zoomed veil break into visible cells.
    int oct = clamp(sjQuality() + 1 + gLod, 4, 7);
    float sharpPow = 1.0 + Q.y * 3.0;
    int bands = clamp(int(R.z), 1, 3);
    float spacing = max(0.02, R.w);
    float thickness = max(0.0, R.x);

    // An even ladder puts every pixel's samples at the same distances, which reads as concentric
    // rings around the player. Offsetting each pixel scatters that away and costs nothing.
    float jitter = ign(gl_FragCoord.xy);

    vec3 acc = vec3(0.0);
    float ground = 0.0;
    for (int s = 0; s < 12; s++) {
        if (s >= samples) break;
        float frac = (float(s) + jitter) / float(samples);
        float h = mix(h0, h1, frac);
        float dist = min(h / max(rd.y, 0.012), AURORA_MAX_DIST);
        vec2 pos = (rd.xz * dist + CamPos.xz) * 0.00085;

        float vertical = smoothstep(0.0, 0.3, frac) * (1.0 - smoothstep(0.55, 1.0, frac));
        float fade = 1.0 / (1.0 + dist * 0.00018);
        float weight = vertical * fade;
        if (weight < 0.004) continue;

        // The hoisted warp is sheared by height, which is what gives a curtain its lean.
        vec2 q = vec2(pos.x + warp * (0.6 + 0.8 * frac), pos.y + t * 0.06);

        float layerSum = 0.0;
        float layerNorm = 0.0;
        for (int b = 0; b < 3; b++) {
            if (b >= bands) break;
            float slot = float(b) - (float(bands) - 1.0) * 0.5;
            float back = float(b) / max(1.0, float(bands) - 1.0);
            // Offset along the ray, so each band sits further into the scene than the last.
            vec2 bq = q + rd.xz * (slot * spacing * thickness * 0.00085 / max(rd.y, 0.012));
            float ridge = abs(fbm2(vec2(bq.x * 2.6, bq.y * 0.55), oct) * 2.0 - 1.0);
            // smoothstep instead of a bare power: the power alone clipped to zero over a wide
            // band, and the flat region is what showed up as hard-edged blocks.
            float sharp = pow(smoothstep(1.0, 0.12, ridge * 1.55), 2.0 + sharpPow);
            layerSum += sharp * mix(1.0, 1.0 - R.y * 0.75, back);
            layerNorm += 1.0;
        }
        float sharp = layerSum / max(layerNorm, 1.0);

        acc += mix(C.rgb, AurC2[i].rgb, frac) * sharp * weight;
        ground += sharp * fade * (1.0 - frac);
    }

    // Normalised by the mean of the vertical profile rather than by this pixel's own sampled
    // weights; self-normalising would cancel the jitter and bring the rings straight back.
    float inv = cov / (float(samples) * 0.625);
    acc *= inv * smoothstep(0.012, 0.11, rd.y);

    if (Q.w > 0.001) {
        acc += C.rgb * exp(-abs(rd.y) * 14.0) * smoothstep(-0.12, 0.05, rd.y)
             * ground * inv * Q.w * 1.6;
    }
    return acc * C.a * 6.0;
}

vec3 allAurorae(vec3 rd) {
    int count = int(Counts.w);
    vec3 sum = vec3(0.0);
    for (int i = 0; i < MAX_AUR; i++) {
        if (i >= count) break;
        sum += AurC2[i].a < 0.5 ? auroraBorealis(rd, i) : auroraVeil(rd, i);
    }
    return sum;
}

// --------------------------------------------------------- celestial bodies

vec3 bodyDirection(int i) {
    vec4 P = BodyP[i];
    vec4 R = BodyR[i];
    float ce = cos(P.y);
    vec3 base = normalize(vec3(ce * cos(P.x), sin(P.y), ce * sin(P.x)));

    int motion = int(R.z);
    if (motion == 0) return base;

    float angle = R.y;
    if (motion == 2) {
        // Locked to the day cycle, so the body rises and sets with the sun.
        angle += TimeInfo.x * TAU;
    } else if (motion == 3) {
        // Slow lateral drift around the compass instead of a full orbit.
        return rotateY(base, sin(sjTime() * BodyQ[i].x * 0.05 + R.y) * 0.45);
    } else {
        angle += sjTime() * BodyQ[i].x * 0.02;
    }
    return rotateAxis(base, celestialAxis(R.x), angle);
}

vec3 bodyLayer(vec3 rd, int i) {
    vec4 C = BodyC[i];
    vec4 C2 = BodyC2[i];
    vec4 P = BodyP[i];
    vec4 Q = BodyQ[i];
    vec4 R = BodyR[i];
    vec4 S = BodyS[i];
    if (C.a <= 0.001) return vec3(0.0);

    vec3 bd = bodyDirection(i);
    float cosA = dot(rd, bd);
    if (cosA <= 0.0) return vec3(0.0);

    float size = max(0.0005, P.z);
    vec3 up = abs(bd.y) > 0.95 ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);
    vec3 right = normalize(cross(up, bd));
    vec3 upv = cross(bd, right);
    vec2 uv = vec2(dot(rd, right), dot(rd, upv)) / max(cosA, 1e-4);
    float r = length(uv);
    float reach = size * max(8.0, 3.0 + Q.y * 28.0);
    if (r > reach) return vec3(0.0);

    // uv is divided by cosA, so a screen pixel spans about this much of the body's local space.
    float wpx = gPixel / max(cosA, 1e-4) * 0.5;

    int type = int(P.w);
    vec3 col = C.rgb * pow(max(0.0, 1.0 - r / (size * (2.0 + Q.y * 26.0))), 2.6) * Q.y;

    if (type == 3) {
        float disc = aastepW(-size, -r, size * 0.06, wpx);
        float ring = aastepW(size * 1.03, r, size * 0.03, wpx) - aastepW(size * 1.38, r, size * 0.07, wpx);
        col = mix(col, vec3(0.0), disc);
        col += C.rgb * max(0.0, ring) * 3.0;
        col += C.rgb * smoothstep(size * 3.5, size * 1.1, r) * 0.35;
        return col * C.a;
    }

    if (type == 2) {
        col += C.rgb * pow(max(0.0, 1.0 - r / (size * 6.0)), 3.0);
        return col * C.a;
    }

    // Solid bodies: planet, ringed planet and crescent all share the lit disc.
    float disc = 1.0 - aastepW(size, r, size * 0.02, wpx);
    float limb = sqrt(max(0.0, 1.0 - pow(min(r / size, 1.0), 2.0)));

    float shade = 1.0;
    if (Q.w > 0.001) {
        vec3 lightDir = normalize(SunDir.xyz);
        vec2 luv = vec2(dot(lightDir, right), dot(lightDir, upv));
        float term = dot(normalize(vec3(uv / size, limb + 1e-4)), normalize(vec3(luv, 0.65)));
        // A hard terminator read as "half the planet is missing", so the dark side keeps a floor.
        shade = mix(1.0, mix(0.45, 1.0, clamp(term * 0.5 + 0.5, 0.0, 1.0)), Q.w);
    }

    float detail = 1.0;
    if (S.x > 0.001) {
        float spin = sjTime() * R.w * 0.05;
        vec2 suv = vec2(uv.x / size * 1.4 + spin, uv.y / size * 5.5);
        detail = 1.0 - S.x * 0.25 + S.x * 0.5 * fbm2(suv, 3 + gLod);
    }

    vec3 lit = mix(C2.rgb, C.rgb, clamp(shade, 0.0, 1.0));
    col += lit * disc * mix(0.55, 1.15, limb) * detail;

    if (type == 4) {
        // Crescent: carve a second disc offset along the light direction.
        vec3 lightDir = normalize(SunDir.xyz);
        vec2 luv = vec2(dot(lightDir, right), dot(lightDir, upv));
        vec2 offset = (length(luv) > 1e-4 ? normalize(luv) : vec2(1.0, 0.0)) * size * 0.55;
        float carve = 1.0 - aastepW(size * 0.96, length(uv + offset), size * 0.02, wpx);
        col = mix(col, vec3(0.0), carve * disc);
    }

    if (type == 1 && Q.z > 0.001) {
        float rr = length(vec2(uv.x, uv.y / max(0.05, C2.a)));
        float inner = size * 1.45;
        float outer = inner + size * Q.z * 2.2;
        float ring = aastepW(inner, rr, size * 0.03, wpx) - aastepW(outer, rr, size * 0.05, wpx);
        // Only the slice actually behind the planet is hidden, not the whole upper half.
        float occluded = step(0.0, uv.y) * disc;
        col += C.rgb * max(0.0, ring) * 0.85 * (1.0 - occluded);
    }

    return col * C.a;
}

vec3 allBodies(vec3 rd) {
    int count = int(Counts2.x);
    vec3 sum = vec3(0.0);
    for (int i = 0; i < MAX_BODY; i++) {
        if (i >= count) break;
        sum += bodyLayer(rd, i);
    }
    return sum;
}

// --------------------------------------------------------------- sun & moon

vec3 sunAndMoon(vec3 rd) {
    vec3 col = vec3(0.0);

    if (FogCfg.z > 0.5) {
        float ang = acos(clamp(dot(rd, normalize(SunDir.xyz)), -1.0, 1.0));
        float size = max(0.002, SunColor.a);
        col += SunColor.rgb * ((1.0 - aastepD(size, ang, size * 0.10)) * 2.4
                + pow(max(0.0, 1.0 - ang / (size * 11.0)), 3.0) * 0.55
                + pow(max(0.0, 1.0 - ang / (size * 40.0)), 5.0) * 0.18);
    }

    if (FogCfg.w > 0.5) {
        float ang = acos(clamp(dot(rd, normalize(MoonDir.xyz)), -1.0, 1.0));
        float size = max(0.002, MoonColor.a);
        float limb = sqrt(max(0.0, 1.0 - pow(min(ang / size, 1.0), 2.0)));
        float craters = 0.86 + 0.14 * fbm3(rd * 90.0, 2 + gLod);
        col += MoonColor.rgb * ((1.0 - aastepD(size, ang, size * 0.06)) * mix(0.7, 1.15, limb) * craters
                + pow(max(0.0, 1.0 - ang / (size * 7.0)), 3.0) * 0.25);
    }

    return col;
}

// ------------------------------------------------------------------- clouds

vec4 cloudLayer(vec3 rd, int i) {
    vec4 C = CloudC[i];
    vec4 C2 = CloudC2[i];
    vec4 P = CloudP[i];
    vec4 Q = CloudQ[i];
    vec4 R = CloudR[i];
    if (C.a <= 0.001 || rd.y <= 0.012) return vec4(0.0);

    float dist = min(max(8.0, P.w) / rd.y, 40000.0);
    vec2 p = (rd.xz * dist + CamPos.xz) * 0.00042 / max(0.15, Q.w);

    // Drift is in world units per second along the wind bearing. The old rate worked out at
    // roughly a pixel a minute, which is why the deck looked painted on.
    p += R.xy * (sjTime() * P.z * 0.05);

    int oct = clamp(int(Q.x) + sjQuality() - 3 + gLod, 2, 8);
    int type = int(R.w);

    // Quantity breaks the deck into more, smaller masses without changing how much sky it covers.
    float quantity = max(0.05, R.z);
    float evolve = sjTime() * C2.a * 0.35;
    float base = fbm2Evolving(p * quantity, oct, evolve);

    if (type == 1) {
        // Stratus: flattened, wide sheets.
        base = mix(base, fbm2Evolving(vec2(p.x * quantity * 0.35, p.y * quantity * 1.6), oct, evolve * 0.6), 0.7);
    } else if (type == 2) {
        // Cirrus: stretched, wispy streaks.
        base = mix(base, fbm2Evolving(vec2(p.x * quantity * 2.6, p.y * quantity * 0.3), oct, evolve * 1.4), 0.8);
        base = pow(clamp(base, 0.0, 1.0), 1.4);
    } else if (type == 3) {
        // Storm: ridged and piled up.
        base = 1.0 - abs(base * 2.0 - 1.0);
        base = pow(clamp(base, 0.0, 1.0), 0.7);
    }

    float coverage = clamp(P.x, 0.0, 1.0);
    float sharpness = max(0.02, 1.02 - Q.y);
    float density = smoothstep(1.0 - coverage, 1.0 - coverage + sharpness * 0.42, base);
    density = pow(density, max(0.2, P.y));
    if (density <= 0.001) return vec4(0.0);

    density *= smoothstep(0.012, 0.16, rd.y);

    float light = clamp(dot(rd, normalize(SunDir.xyz)) * 0.5 + 0.5, 0.0, 1.0);
    float self = 1.0 - clamp(Q.z * (1.0 - base), 0.0, 1.0);
    vec3 col = mix(C2.rgb, C.rgb, self);
    col = mix(col, SunColor.rgb * 1.15, pow(light, 7.0) * 0.6);

    return vec4(col, clamp(density * C.a, 0.0, 1.0));
}

/** Decks composite in order, so a deck listed later sits in front of the ones before it. */
vec4 allClouds(vec3 rd) {
    int count = int(Counts2.y);
    vec3 col = vec3(0.0);
    float alpha = 0.0;
    for (int i = 0; i < MAX_CLOUD; i++) {
        if (i >= count) break;
        vec4 c = cloudLayer(rd, i);
        if (c.a <= 0.0) continue;
        col = mix(col, c.rgb, c.a);
        alpha = alpha + c.a * (1.0 - alpha);
    }
    return vec4(col, alpha);
}

// ---------------------------------------------------------------- watermark

/**
 * The SkyJet mark, painted on the bottom face of the skybox. It rides the sky itself, so anyone
 * looking down at a shared SkyJet sky sees where it came from. Deliberately not configurable.
 */
vec3 watermark(vec3 rd) {
    if (MarkCfg.w < 0.5 || rd.y >= -0.02) return vec3(0.0);
    vec2 m = rd.xz / max(-rd.y, 1e-4);
    m /= max(0.05, MarkCfg.y);
    // Looking down at the plate flips both axes against the image: screen right is -X at yaw 0,
    // and screen up runs toward the far edge, which is the top of the texture rather than v=1.
    vec2 uv = vec2(-m.x / max(0.1, MarkCfg.z), -m.y) * 0.5 + 0.5;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return vec3(0.0);
    vec4 t = texture(WatermarkSampler, uv);
    // Fades out toward the horizon so it never smears along the rim.
    float grazing = smoothstep(0.02, 0.35, -rd.y);
    return t.rgb * t.a * MarkCfg.x * grazing;
}

// --------------------------------------------------------------------- main

/** Triangular probability dither. Two uniform samples give a noise floor with no visible grain. */
float tpdf(vec2 seed) {
    float a = hash12(seed);
    float b = hash12(seed + 17.13);
    return a - b;
}

void main() {
    vec3 rd = normalize(rayDir);
    // Derivatives have to be taken before any non-uniform branching.
    float pixelAngle = max(length(fwidth(rd)), 1e-5);
    gPixel = pixelAngle;
    // 0.0022 is roughly a pixel at the default field of view. Narrowing the view shrinks that,
    // and each halving earns another octave of procedural detail instead of magnifying the same
    // one, which is what made a zoomed sky go soft and blocky.
    gLod = int(clamp(log2(0.0022 / pixelAngle) * HorizonCfg.w, 0.0, 3.0));

    // The lower hemisphere can be sampled as the mirror of the upper one, so looking down shows
    // the same sky rather than the dark end of every gradient ramp.
    int below = int(GroundCol.a);
    vec3 sd = (below == 1 && rd.y < 0.0) ? vec3(rd.x, -rd.y, rd.z) : rd;

    vec4 grad = applyGradients(sd);
    vec4 sky = allSkyboxes(rd);
    vec3 atmos = atmosphereColor(rd);
    vec3 neb = allNebulae(sd);
    vec3 stars = allStars(sd, pixelAngle);
    vec3 bodies = allBodies(rd);
    vec3 sunmoon = sunAndMoon(rd);
    vec3 aur = allAurorae(rd);
    vec4 clouds = allClouds(rd);

    // Categories are evaluated once above and composited here in the configured order, each with
    // its own opacity, tint and blend. That is what the Layers tab reorders.
    vec3 col = vec3(0.0);
    for (int s = 0; s < STACK; s++) {
        vec4 A = StackA[s];
        if (A.w < 0.5) continue;
        int cat = int(A.x);
        vec3 c;
        float a;
        if (cat == CAT_GRAD)         { c = grad.rgb;   a = grad.a; }
        else if (cat == CAT_SKY)     { c = sky.rgb;    a = sky.a; }
        else if (cat == CAT_ATMOS)   { c = atmos;      a = 1.0; }
        else if (cat == CAT_NEB)     { c = neb;        a = 1.0; }
        else if (cat == CAT_STAR)    { c = stars;      a = 1.0; }
        else if (cat == CAT_BODY)    { c = bodies;     a = 1.0; }
        else if (cat == CAT_SUNMOON) { c = sunmoon;    a = 1.0; }
        else if (cat == CAT_AUR)     { c = aur;        a = 1.0; }
        else                         { c = clouds.rgb; a = clouds.a; }
        col = blendLayer(col, c * StackT[s].rgb, clamp(a * A.y, 0.0, 1.0), int(A.z));
    }

    col += watermark(rd);

    if (below == 2) {
        col = mix(col, GroundCol.rgb, smoothstep(0.0, -max(0.004, HorizonCfg.y), rd.y));
    }

    if (FogCfg.x > 0.001) {
        // Symmetric around the horizon. Keying off max(rd.y, 0) washed the whole lower
        // hemisphere in fog colour and made the sky look like it stopped at the horizon.
        float band = exp(-abs(rd.y) / max(0.01, FogCfg.y));
        col = mix(col, FogColor.rgb, clamp(band * FogCfg.x * FogColor.a, 0.0, 1.0));
    }

    col *= MasterTint.rgb;
    col *= Master.y * (1.0 - 0.28 * TimeInfo.z);
    float luma = dot(col, vec3(0.2126, 0.7152, 0.0722));
    col = mix(vec3(luma), col, Master.z);
    col = max(col, vec3(0.0));

    float peak = max(max(col.r, col.g), col.b);
    if (peak > 1.0) {
        col /= peak;
    }
    col = pow(col, vec3(1.0 / max(0.05, Master.w)));

    // Eight bit output quantises a smooth sky into visible bands; a triangular dither of about
    // one output step scatters the boundary instead, which is what "clean" actually costs.
    if (Counts3.z > 0.001) {
        vec2 seed = gl_FragCoord.xy + fract(sjTime()) * 71.0;
        col += vec3(tpdf(seed), tpdf(seed + 3.7), tpdf(seed + 8.1)) * (Counts3.z / 255.0);
    }

    fragColor = vec4(col, Master.x) * ColorModulator;
}
