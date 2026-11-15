# AppDimens Games 3.x (3.3.0) — Skill for Coding Agents

Use this skill whenever writing or reviewing code that sizes game UI/HUD/world
elements with AppDimens Games — Android (Compose/Kotlin/Java/NDK) **and** every other
platform/engine: Windows, Linux, Steam Deck, Proton, macOS, iOS, Web, consoles; Unity,
Unreal Engine 5, Godot 4, custom C/C++ engines, .NET engines.

## Repository layout

`android/` (Gradle: Kotlin, Compose, Java, NDK) · `native/` (C/C++ SDK, every OS) · `engines/{unity,unreal,godot}` ·
`dotnet/` (.NET package + C# drop-in) · `tools/` (oracle, sync) · `DOCUMENTATION/`. Android builds run from `android/`
(`cd android && ./gradlew …`); shared C headers are copied into the engines by `python3 tools/sync_bindings.py`.

## Token calls per language (the AppDimens way: token applied to a value)

C# `64f.ASdp()` / `24f.FTSdp()` (AppDimens.Net casing) · GDScript `AppDimens.asdp(64)` · C++ `appdimens::asdp(64)`,
`64_asdp` (`tokens.hpp`, live snapshot) · C `ad_asdp(&m, 64)` · Unreal `UAppDimensLibrary::ASdp(64)` · Kotlin `64.asdp`.
Never multiply by a factor by hand.

## Cross-platform quick rules (3.3.0)

1. **Pick the binding:** Unity → UPM package components; Unreal → enable the plugin and set the UMG DPI rule to
   `AppDimensDPIScalingRule`; Godot → `addons/appdimens` autoload; C/C++ → `native/include/appdimens/games.h`;
   .NET → `dotnet/AppDimens.Games`; other languages → C ABI.
2. **Scale mode:** games use REFERENCE (author at 1920×1080). Values are px at the reference; plain tokens
   (`sdp`, `asdp`, `ftsdp`) return final px — never add `Px` in REFERENCE mode.
3. **Strategies by element:** HUD/menus `asdp`, crosshair/minimap/anything that must never overflow `ftsdp`,
   backgrounds `flsdp`, text `assp` + accessibility text scale, pixel art `ADG_VIEW_INTEGER`.
4. **Upscaling:** build the snapshot from the *display* size; render 3D at `adg_upscale_render_size`; apply
   `adg_upscale_mip_bias` and `adg_upscale_jitter`; HUD is drawn at display resolution.
5. **Cameras:** `ADG_FOV_HYBRID` by default; FPS: Hor+ with a `max_hfov` cap; convert sensitivity with `adg_sensitivity_scale`.
6. **Graphics API:** use `adg_native_viewport`/`adg_gl_viewport`/`adg_metal_viewport` and `adg_ortho/adg_perspective(api…)`
   — never hand-roll Y flips or depth ranges.
7. **Safe areas:** `adg_safe_rect(w, h, os_insets, overscan)` (0.05 on TVs) + `adg_hud_band(…, 16/9)` on ultrawide.
8. **Threads:** publish from the window thread (`adg_publish_px`), read with `adg_acquire()`/`adg_read()`.

## Golden rules

1. **Family API only.** Extensions/stems/suffixes/facilitators are IDENTICAL to
   appdimens-dynamic 3.x / kmp 1.x. Never invent new stems.
2. **Code side returns px; Compose side returns Dp/TextUnit.**
3. **Auto-adjust is default.** Values follow window resizes. Suffix `i`
   (`ignoreMultiWindows`) = invariant: anchored to the frozen fullscreen snapshot.
   Suffix `a` = aspect-ratio refinement. `ia` = both.
4. **Wire-up once per resize:** `GameScreen.updateFromContext(context)` in
   Activity/Surface callbacks; wrap Compose content in `AppDimensProvider { }`.
5. **Hot paths must not allocate.** Prefer extension fast lanes or
   `GameMath.calculateXxxDp(base, metrics)` with a cached `GameMetrics`.

## Compose version independence

The library declares Jetpack Compose as `compileOnly` (`androidx.compose.runtime`
+ `androidx.compose.ui`; `androidx.compose.material3` is **not** used by the
library). It compiles against the catalog's Compose but **never ships or pins a
version** — the published AAR/POM carries no Compose constraint. The consumer's
Compose (any version) is the single runtime, so there is no version skew and no
"class/function not found" regardless of the Compose version the app chooses.
Consumers must apply their own `compose-bom` / Compose dependency.
When editing a library module, keep Compose as `compileOnly` (plus
`testImplementation` of the runtime for JVM tests) — never `api`/`implementation`.

## World layer (sprites ↔ viewport)

`WorldScale.fitPx/fillPx(metrics, designW, designH)` = `min`/`max(windowW/designW,
windowH/designH) × density` — the exact factor of `ViewportMode.FIT_ALL` / `CROP`
(`ViewportTransform`). Use them together so sprites land inside the letterbox.
Native `math::scaleVecFit` uses the same rule (`render::Mode::FitAll`). Do not
reintroduce the old min/max-dimension (orientation-agnostic) formula.

## Stem map

| Strategy | Dp stems | Text | Example |
|---|---|---|---|
| scaled (core) | sdp/hdp/wdp | ssp/hsp/wsp · sem/hem/wem | `48.sdp(ctx)` · `16.ssp(ctx)` |
| percent | psdp/phdp/pwdp + spaceW/Sw/H | pssp… | `10.spaceW(ctx)` |
| power | pwsdp… | pwssp… | `48f.pwsdp(ctx)` |
| custom *(unreleased)* | cdp, cdpi, cdpDp | csp, cspDp | simplest: typed one-liners with autocomplete `CustomScaleStrategy { (width / 360f).coerceIn(0.85f, 1.8f) }` (factor, once per window) / `scalingFormula { base * pow(sw / 360f, 0.8f) }` (final value; scope names = text names: base sw width height ratio tablet tv landscape pow clamp…); text for JSON `scalingFormula("…")` / `64.cdp("base * …")` (`base * (…)` = one-multiply fast path); C# `CustomScaleStrategy.Create(s => s.Clamp(s.Width / 360f, 0.85f, 1.8f))`, `ScalingStrategy.Formula(s => s.Base * s.Pow(s.Sw / 360f, 0.8f))`; `64f.cdp(Hud)` — your own strategy (DSL/JSON, rules per device class, free-form `expression("…")`/`formula("…")`); C# `CustomKernels.Cdp(64f, hud)`, `64f.Cdp("base * …")`; see DOCUMENTATION/strategies/custom.md, the guides in DOCUMENTATION/strategies/custom/ and the agent notes [custom.md](custom.md) |
| fluid | fsdp… | fssp… | `16f.fsdp(ctx)` |
| auto ⭐ gameplay | asdp… | assp… | `64f.asdp(ctx)` |
| diagonal | dgsdp… | dgssp… | `48f.dgsdp(ctx)` |
| fill | flsdp… | flssp… | backgrounds |
| fit | ftsdp… | ftssp… | viewports/boards |
| interpolated | isdp… | issp… | secondary objects |
| logarithmic | logsdp… | logssp… | TV/large |
| perimeter | prsdp… | prssp… | balanced |
| density | dsdp… | dssp… | pixel-aligned |

Suffixes append directly: `sdpa`, `sdpi`, `sdpia`; Px variants for code px needs:
`sdpPx` etc. Inverters: `sdpPh/Lh/Pw/Lw`, `hdpLw/hdpPw`, `wdpLh/wdpPh`.
Facilitators: `sdpRotate/sdpMode/sdpQualifier/sdpScreen`. Builder: `16.scaledDp()
.aspectRatio(true).screen(UiModeType.TELEVISION,32).qualifier(DpQualifier.SMALL_WIDTH,600,24).sdp(ctx)`.
Java facade: `DimenSdp.sdp/sdpi/getDimensionInPx/getDimensionInDp/warmupCache()`.
Deprecated 2.x names → `com.appdimens.games.compat.GamesCompat` (do not use in new code).

## Element → strategy cheat-sheet

HUD/buttons → DEFAULT (`sdp`) · gameplay objects → AUTO/BALANCED (`asdp`) ⭐ ·
backgrounds/parallax → FILL (`flsdp`) · boards/maps fully visible → FIT (`ftsdp`) ·
score/dialog text → FLUID (`fsdp`) · TV/large → LOGARITHMIC (`logsdp`) ·
touch targets physical size → UNITS (`2f.cmPx(ctx)`) or DIAGONAL (`dgsdp`) ·
pixel-perfect sprites → raw value.

## Native engines

C++20 header-only core: `appdimens::games::{Constants,Metrics,math,render}` — publish
with `updateMetrics(Metrics::make(...))` from surface callbacks; read via
`metrics()` (live) or `invariantMetrics()` (frozen, `i`). Pure C99 single header:
`appdimens_games_c.h` (`adg_*`). JNI bridge object:
`com.appdimens.games.jni.NativeBridge`. Vulkan/DirectX viewport structs are
field-compatible with VkViewport/D3D11_VIEWPORT; letterbox via
`render::viewportRect/vkViewport/dxViewport/glRect`; ortho matrix helper included.

## Validation

Any formula change MUST pass `python3 tools/oracle.py` (30 cases) and the JVM
tests with oracle-generated expectations (`cd android && ./gradlew testDebugUnitTest`; suites cover
GameMath, GameCache, GameMetrics, GameScreen, GameWorld, code API, units, resize).
Release (current: **3.2.0**): bump together `appdimens.version` (published coordinate)
and `VERSION_NAME` in `gradle.properties`, the `orElse("…")` fallbacks in every
module `build.gradle.kts`, `versionName` of `sample`/`benchlab`, and the tag/version
checks in `.github/workflows/release.yml` (tags are bare, e.g. `3.2.0`; `v3.2.0` also accepted). Constants are family-canonical:
300/533/611.6305(literal)/833/1.78 — never "fix" 611.6305 to the true sqrt;
parity beats pedantry (delta documented).

<!-- appdimens-family:begin -->
## Other AppDimens platforms

The same token names and math exist on Android (Compose, Views, XML), Kotlin Multiplatform, iOS/macOS, Flutter, React
Native, Web, .NET, Qt, GTK, LVGL, Roku, Python/Blender and games (Unity, Unreal, Godot, C/C++). For code on another
platform, use that library's skill — every skill, plus the layout-intent decision table, installs with
`npx appdimens-skills` (https://github.com/bodenberg/appdimens-skills). Hub: https://github.com/bodenberg/appdimens.
<!-- appdimens-family:end -->
