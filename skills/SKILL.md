# AppDimens Games 3.x (3.2.0) — Skill for Coding Agents

Use this skill whenever writing or reviewing code that sizes game UI/HUD/world
elements on Android with the AppDimens Games library (family: appdimens-dynamic,
appdimens-kmp, appdimens-games — same vocabulary).

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

Any formula change MUST pass `python3 scripts/oracle.py` (30 cases) and the JVM
tests with oracle-generated expectations (`./gradlew testDebugUnitTest`; suites cover
GameMath, GameCache, GameMetrics, GameScreen, GameWorld, code API, units, resize).
Release (current: **3.2.0**): bump together `appdimens.version` (published coordinate)
and `VERSION_NAME` in `gradle.properties`, the `orElse("…")` fallbacks in every
module `build.gradle.kts`, `versionName` of `sample`/`benchlab`, and the tag/version
checks in `.github/workflows/release.yml` (tags are bare, e.g. `3.2.0`; `v3.2.0` also accepted). Constants are family-canonical:
300/533/611.6305(literal)/833/1.78 — never "fix" 611.6305 to the true sqrt;
parity beats pedantry (delta documented).
