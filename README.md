<p align="center"><img src="src/main/resources/assets/skyjet/icon.png" width="128"></p>

# SkyJet

A fully shader-driven procedural sky engine for Minecraft, by **Wayysss** and **HitsOnly**.

SkyJet takes over the vanilla sky pass and replaces it with a single full screen shader that
draws gradients, atmospheric scattering, depth-layered star fields, nebulae, aurorae, volumetric
clouds, celestial bodies and textured skyboxes — all configurable live from an in-game editor with
a real-time preview.

## Supported versions

| Minecraft | Java | Loader | Jar |
|-----------|------|--------|-----|
| 1.21.11   | 21   | Fabric | `skyjet-1.0.0+1.21.11.jar` |
| 26.1      | 25   | Fabric | `skyjet-1.0.0+26.1.jar` |
| 26.2      | 25   | Fabric | `skyjet-1.0.0+26.2.jar` |

Client-side only. Requires Fabric API.

### Fabric Loader floor

Each jar declares the **lowest loader that branch supports**, not whatever was newest at
build time:

| Minecraft | Minimum Fabric Loader |
|-----------|-----------------------|
| 1.21.11   | 0.17.3 |
| 26.1      | 0.18.3 |
| 26.2      | 0.18.4 |

`deps.fabric_loader` in `stonecutter.properties.toml` is the loader the dev environment builds
against and is deliberately kept apart from `mod.loader_dep`, the declared floor, so bumping the
toolchain does not silently raise what players need installed.

Older **Fabric API** builds are handled at runtime rather than by a version bound:

- `KeyMappingHelper` / `KeyBindingHelper` and `ClientCommands` / `ClientCommandManager` were
  renamed mid-line, so `Platform` resolves whichever exists reflectively.
- The world label registers against `LevelRenderEvents` (26.x) or `WorldRenderEvents` (1.21.11)
  inside a guard. An API build without the event costs the label, not the mod.
- If no command factory is present, `/skyjet` is skipped and the keybind still opens the editor.

## Opening the editor

- Press **Z** (rebindable under Controls → SkyJet), or
- Run `/skyjet`

A second, unbound keybind toggles the whole mod on and off.

## Editor tabs

| Tab | What it controls |
|-----|------------------|
| **Presets** | 28 built-in skies, plus **Reset sky** and **Start from scratch** |
| **Skybox** | Up to 2 textured skybox layers: panorama, six-face cubemap or overhead dome, from any resource pack |
| **Gradients** | Up to 8 stacking gradient layers: two colours with alpha, position, softness, opacity, 7 blend modes, 5 projection axes, angle and animated noise |
| **Stars** | Up to 6 star layers: two colours, variance, density, size and size variance, brightness, minimum pixel size, glow, four shapes, hero-star fraction, parallax depth, twinkle, rotation, drift, orbit tilt and band clustering |
| **Cosmos** | Up to 4 nebula/galaxy volumes: two colours, intensity, scale, contrast, parallax depth, octaves, drift, domain warp, elevation bias, four shapes |
| **Aurora** | Up to 3 curtains with a real height range, base/tip colours, waviness, sharpness, coverage, ground glow, speed and height sample count |
| **Bodies** | Up to 6 celestial bodies: planets, ringed planets, glow orbs, black holes and crescents, with azimuth/elevation, size, lit and dark side colours, glow, ring width and tilt, sun shading, surface detail and spin, four motion modes, and **click-to-place in the preview** |
| **Clouds** | Volumetric cloud deck: lit/shadow colour, coverage, density, sharpness, self shadowing, altitude, scale, wind speed and bearing, shape churn, detail octaves |
| **Atmosphere** | Rayleigh/Mie scattering, Mie directionality, density, tint, sunset glow, sun & moon discs, horizon offset/softness/curve, ground plane, fog blending |
| **Image** | Master opacity, exposure, saturation, gamma, dithering, edge smoothing, detail level and auto detail |
| **Animation** | Time source (world / local clock / frozen), clock offset and speed, animation speed, star fade curve, night boost, sun path offset and tilt, weather response |
| **World** | The vanilla sky override master switch and its per-element toggles, weather suppression, fog colour override, per-dimension toggles |
| **Label** | The floating SkyJet label under the player: text, font, style, colour, shadow, outline glow, height, scale, bob and spin |
| **Profiles** | Create, rename, duplicate and delete profiles; copy/paste a whole sky as JSON through the clipboard |

Every colour input has a modern picker (saturation/value square, hue bar, alpha bar, quick
swatches) alongside a live hex field. An open picker takes mouse input before every other widget,
so clicking inside it can never fall through to whatever is drawn underneath.

## Time

The default time source is **Local Clock**: the sky follows the wall clock of the machine running
the game, so midnight in the room is midnight in the sky. `Clock offset` shifts it in hours and
`Clock speed` compresses the day (1 is a true 24 hour day, 4 runs four sky days per real day).
The other sources are the world's own time and a frozen time of day; freezing the time of day does
not freeze the animation, because that is what the animation speed slider is for.

## Using SkyJet as a skybox mod

The **Skybox** tab wraps existing sky art around the world, so packs like Dramatic Skies, Purple
Cosmos and Aqua Pack can be worn by SkyJet with its own layers stacked on top.

- **Cubemap** takes the shared prefix of six faces and finds them itself. Accepted suffixes are
  `_top/_bottom/_north/_south/_east/_west`, `_0`..`_5` and `_up`/`_down`. The faces are stitched
  once into a 3×2 atlas and cached.
- **Panorama** takes one equirectangular image.
- **Overhead dome** takes one polar image covering the upper hemisphere, for overlay art.

Each layer has a tint, opacity, brightness, blend mode, rotation, constant spin, tilt, a night-only
fade and a "treat black as transparent" switch for art authored without an alpha channel. Use
**Reload textures** after swapping resource packs.

## The world label

While SkyJet is switched on, a label floats in the world just below the player, billboarded to the
camera with a drop shadow and an optional eight-way outline glow. It is real world-space geometry
with correct depth, not a HUD overlay, and it is configurable down to the font (default, uniform,
galactic, illager), style, colour, height, scale, bob and spin.

## How it works

The sky is one draw call. `SkyRendererMixin` hooks the vanilla sky pass and dispatches a single
full screen triangle through a custom `RenderPipeline`; the fragment shader reconstructs a world
space ray per pixel and evaluates every enabled layer analytically. It covers the full sphere, so
looking down puts sky under you rather than a cut-off dome.

- All configuration reaches the GPU through one 2560-byte std140 uniform block, packed in
  `SkyUniforms` and consumed by `assets/skyjet/shaders/core/skyjet_sky.fsh`.
- Layers that are switched off are skipped by the shader's loop bounds, so they cost nothing.
- Star fields and nebulae are skipped entirely once they have faded out in daylight.
- The **Detail level** slider scales noise octaves and aurora height samples. **Auto detail**
  walks that level down below 45 fps and back up above 75.

### What the performance work actually changed

- **Aurorae** were the frame. The old ray march evaluated two fbm chains per step, so a thirteen
  step curtain cost about eighty dependent texture fetches per pixel. A curtain is a vertical
  sheet, so it is now sampled at three to six heights and blended; each height still looks at a
  different world position, so the parallax survives. That is roughly a five times cut on the
  most expensive layer in the mod.
- **Nebula domain warping** was two three-octave fbm evaluations, six fetches per layer per pixel.
  `vnoise3x3` returns two independent 3D fields and a 2D field from one fetch, which covers both
  warp axes and the colour mix seed.
- **Per-frame allocations** in the render path (direct byte buffers, matrices, vectors) are now
  reused instead of churned.

### Why the sky looked pixelated

Three separate causes, all fixed:

- **Banding.** Eight bit output quantises a smooth sky into visible steps. The final colour now
  gets a triangular dither of about one output step, which scatters the boundary instead.
- **Star lattice squares.** Each star was looked up in a single noise cell, so any halo wider than
  the cell was clipped flat at the boundary and dense fields read as a grid of squares. Stars now
  sample the eight surrounding cells and their radius is capped to stay inside that neighbourhood.
- **Aliased analytic edges.** Gradient stops, body rims, rings and the sun and moon discs are
  widened to at least one pixel using screen space derivatives. Derivatives are only taken in
  uniform control flow; code that runs after a per-pixel early out gets its pixel width passed in.

### Why stars only showed up zoomed in

A star thinner than a screen pixel falls between samples. Every star is now widened to at least
`Minimum pixel size` pixels and dimmed by the area it gained, so the field keeps its brightness
and shows at normal field of view.

Configuration lives in `config/skyjet/skyjet.json`.

## Building

```
./gradlew build
```

Produces a jar per version under `versions/<version>/build/libs/`. Stonecutter drives the
multi-version source; switch with `./gradlew "Set active project to <version>"` and reset with
`./gradlew "Reset active project"` before committing.

all rights reserved. made by wysss.
