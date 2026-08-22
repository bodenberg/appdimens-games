# AppDimens Games 3.0 — Skill for Coding Agents

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

## Stem map

| Strategy | Dp stems | Text | Example |
|---|---|---|---|
| scaled (core) | sdp/hdp/wdp | ssp/hsp/wsp · sem/hem/wem | `48.sdp(ctx)` · `16.ssp(ctx)` |
| percent | psdp/phdp/pwdp + spaceW/Sw/H | pssp… | `10.spaceW(ctx)` |
| power | pwsdp… | pwssp… | `48f.pwsdp(ctx)` |
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
tests with oracle-generated expectations. Constants are family-canonical:
300/533/611.6305(literal)/833/1.78 — never "fix" 611.6305 to the true sqrt;
parity beats pedantry (delta documented).
