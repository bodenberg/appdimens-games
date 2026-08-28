# 📘 AppDimens Games — The Complete Beginner's Guide (with tutorials)

> **What this guide is:** a step-by-step tutorial that takes you from "what is
> AppDimens Games?" to working code in **every supported stack** — Kotlin, Java,
> Compose, C++/NDK, pure C, OpenGL ES, Vulkan, DirectX and Unity/C# (plus Godot/MAUI).
>
> **PT:** guia tutorial completo — do "o que é" ao código funcionando em todas as stacks.

---

## 1. What is AppDimens Games?

**The problem it solves.** Games run on phones, foldables, tablets, TVs and desktops.
A button designed at 48 dp on a 360 dp-wide phone must stay *usable* on a 600 dp tablet
without becoming gigantic, must survive rotation/split-screen/fold posture changes at
runtime, and your game loop asks for dozens of sizes **per frame**.

**The solution.** One number describes the window (a *snapshot*). From that snapshot the
library pre-computes every scaling factor once. Asking for a size then costs a single
float multiply (~2 ns) with zero allocations. When the window resizes, a new snapshot is
published atomically and every subsequent value auto-adjusts. Values marked with the `i`
suffix are excluded from that adjustment on purpose (stable HUD).

```
┌──────────────┐   resize    ┌────────────────────────┐   per frame   ┌─────────────────┐
│ window event │ ──────────▶ │ GameMetrics (snapshot) │ ────────────▶ │ size = base × f │
│ (any engine) │             │ all factors precomputed │               │     ≈ 2 ns      │
└──────────────┘             └────────────────────────┘               └─────────────────┘
                                     ▲                    ▲
                          no suffix: live snapshot      suffix i: frozen fullscreen snapshot
```

### 1.1 The three pillars

1. **One snapshot per window** — measured once; everything derives from it.
2. **One multiply per value** — hot-path cost of an addition.
3. **Auto-adjust, except `i`** — resize the window and values follow; `i` pins them.

### 1.2 Vocabulary you will see everywhere

| Term | Meaning |
|---|---|
| `dp` | density-independent pixel (design unit) |
| snapshot / metrics | immutable window measurements + precomputed factors |
| stem | extension name, e.g. `sdp`, `asdp`, `fsdp` |
| suffix `a` | apply aspect-ratio refinement |
| suffix `i` | **invariant**: ignore resized-window/multi-window adjustments |
| qualifier | which axis drives scaling: smallest width (`sdp`), width (`wdp`), height (`hdp`) |
| strategy | the growth curve: scaled/balanced/fluid/power/logarithmic/fit/fill… |

### 1.3 Which strategy do I use? (cheat-sheet)

| You are sizing… | Strategy & stem | Why |
|---|---|---|
| HUD buttons/icons/menus | DEFAULT → `sdp` | ~97% linear, familiar UI feel |
| Player/enemy/projectile ⭐ | BALANCED → `asdp` (library-auto) | linear on phones, log on tablets/TV |
| World bounds, containers | PERCENTAGE → `psdp`, `spaceW/H` | keeps screen proportion |
| Backgrounds, parallax | FILL → `flsdp` (library-fill) | covers any aspect ratio |
| Boards/maps fully visible | FIT → `ftsdp` (library-fit) | letterbox, nothing cropped |
| Score/dialog text | FLUID → `fsdp` (library-fluid) | bounded 0.8×–1.2× typography |
| TV/large screens UI | LOGARITHMIC → `logsdp` | maximum control on big screens |
| Touch targets (physical) | DIAGONAL → `dgsdp` or units `2f.cmPx(ctx)` | physical-size consistency |
| Pixel-art sprites | NONE → raw base value | pixel-perfect rendering |

**Worked example (phone 360 dp sw):**
`scaled`: `16 × 360/300 = 19.2`
`balanced @ 600 dp tablet`: `40 × (1.6 + 0.4·ln(1+120/300)) = 69.38`

---

## 2. Tutorial A — Kotlin / Java game (Views or custom loop)

**Goal:** sizes that follow any window change, with one wiring line.

**Step 1 — add dependency**

```kotlin
dependencies {
    implementation("io.github.bodenberg:appdimens-games:3.0.1")
}
```

**Step 2 — wire the resize hook** (this is what makes everything auto-adjust)

```kotlin
class GameActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameScreen.updateFromContext(this)          // first publish
    }
    override fun onConfigurationChanged(c: Configuration) {
        super.onConfigurationChanged(c)
        GameScreen.updateFromContext(this)          // republish on rotate/split-screen
    }
}
```

**Step 3 — use extensions anywhere** (code side returns **px**)

```kotlin
val hudPadPx   = 12.sdp(context)       // follows window resizes
val hudPadInv  = 20.sdpi(context)      // 🔒 invariant (`i`) HUD element
val titlePx    = 16.ssp(context)       // text respecting system font scale
val fixedPx    = 14.sem(context)       // fixed text (ignores font scale)
val worldW     = 100.wdp(context)
val boardH     = 200.hdp(context)
```

**Step 4 (Java)** — static facade:

```java
float pad = DimenSdp.sdp(ctx, 12);
float inv = DimenSdp.sdpi(ctx, 20);
float px  = DimenSdp.getDimensionInPx(ctx, DpQualifier.SMALL_WIDTH, 48,
                                      Inverter.DEFAULT, false, false, null);
DimenSdp.warmupCache();                // pre-touch lazy factors after first publish
```

**Expected result:** rotate the device or drag into split-screen → `hudPadPx` changes;
`hudPadInv` does not.

---

## 3. Tutorial B — Compose game

**Goal:** reactive sizing without manual listeners.

```kotlin
dependencies {
    implementation("io.github.bodenberg:appdimens-games:3.0.1")          // core
    implementation("io.github.bodenberg:appdimens-games-auto:3.0.1")     // gameplay ⭐
}
```

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppDimensProvider {        // ← publishes to composition
            GameScene()
        } }
    }
}

@Composable fun GameScene() {
    Box(Modifier.fillMaxSize()) {
        // gameplay sprite — BALANCED (auto satellite), Dp result:
        Image(painterResource(R.drawable.player),
              modifier = Modifier.size(64.asdp).align(Alignment.Center))

        Text("SCORE", fontSize = 16.ssp, modifier = Modifier.padding(12.sdp))

        IconButton(onClick = { }, modifier = Modifier.size(20.sdpi)) { // 🔒 invariant
            Icon(Icons.Default.Settings, null)
        }
    }
}
```

That is all — `AppDimensProvider` recomposes on every configuration/window change.

---

## 4. Tutorial C — C++20 / NDK (engine-agnostic)

**Goal:** same snapshot model inside your native loop.

**Step 1 — headers** (copy `library-native/src/main/cpp/include` into your include path):

```cmake
target_include_directories(your_game PRIVATE path/to/include)
```

**Step 2 — publish on surface events** (⚠️ keep the Metrics object alive — `static` or member; the hub stores its address):

```cpp
#include "appdimens/games/core.h"
using namespace appdimens::games;

static Metrics g_metrics;                       // lifetime!

void OnSurfaceChanged(int wPx, int hPx, float dpi, bool fullscreen) {
    const float dens = dpi / 160.f;
    g_metrics = Metrics::make(wPx/dens, hPx/dens, /*swDp*/0.f, dpi, 1.f, fullscreen);
    updateMetrics(g_metrics);                   // atomic publish → everyone adjusts
}
```

**Step 3 — read per frame** (one atomic load; cache the reference once per frame):

```cpp
void Frame() {
    const Metrics& m = metrics();                                   // live
    float playerPx = math::autoDp(64.f, m) * m.density;             // BALANCED
    float hudPx    = math::scaledDp(48.f, m) * m.density;           // sdp
    float invPx    = invariantMetrics().fullscreen                  // `i`
                       ? math::scaledDp(20.f, invariantMetrics()) * m.density
                       : 20.f * m.density;
    /* …draw… */
}
```

Full engine class, raylib/SDL examples, thread rules → [NATIVE-GAME-ENGINES.md](NATIVE-GAME-ENGINES.md).

---

## 5. Tutorial D — OpenGL ES 3.x (C++)

**Goal:** letterboxed content + sprites sized by strategies.

**Step 1 — viewport letterbox** on resize:

```cpp
#include "appdimens/games/render.h"
// design space 1920×1080:
auto r = render::glRect(render::Mode::FitAll, (float)wPx, (float)hPx, 1920.f, 1080.f);
glViewport(r.x, r.y, r.width, r.height);
```

**Step 2 — orthographic matrix over design units** (column-major, GLSL-ready):

```cpp
float proj[16];
render::ortho(0.f, 0.f, 1920.f, 1080.f, -1.f, 1.f, proj);
glUniformMatrix4fv(uProj, 1, GL_FALSE, proj);
```

**Step 3 — draw sized content:**

```cpp
const Metrics& m = metrics();
float playerUnits = math::autoDp(64.f, m);          // stays in design units here
DrawQuad(960.f, 540.f, playerUnits, playerUnits);   // your quad renderer
```

**Why this order works:** viewport maps design→screen once; kernels give sizes in the
same design scale; nothing double-scales. Resize → recompute steps 1–3 (cheap).

---

## 6. Tutorial E — Vulkan

**Goal:** correct swapchain recreation + factors fed to shaders.

**Step 1 — detect resize and recreate:**

```cpp
VkResult r = vkAcquireNextImageKHR(dev, swap, UINT64_MAX, sem, VK_NULL_HANDLE, &idx);
if (r == VK_ERROR_OUT_OF_DATE_KHR || r == VK_SUBOPTIMAL_KHR) RecreateSwapchain();
```

**Step 2 — inside `RecreateSwapchain()` publish + set viewport:**

```cpp
static Metrics m;                                        // lifetime!
m = Metrics::make(newW/dens, newH/dens, 0.f, dpi, 1.f, true);
updateMetrics(m);

auto vp = render::vkViewport(render::Mode::FitAll, (float)newW, (float)newH, 1920.f, 1080.f);
VkViewport vk{ vp.x, vp.y, vp.width, vp.height, vp.minDepth, vp.maxDepth };  // field-compatible
VkRect2D sc{ {0,0}, {newW,newH} };
for (auto cmd : frameCmds_) { vkCmdSetViewport(cmd,0,1,&vk); vkCmdSetScissor(cmd,0,1,&sc); }

// refresh UBO once (std140): proj matrix via render::ortho + factors:
ubo.hudFactor   = m.scale * m.density;
ubo.worldScale  = std::min(m.fitScale, m.fillScale);
```

**Step 3 — shader side consumes multiplies only:**

```glsl
layout(std140, binding=0) uniform Frame { mat4 proj; vec2 worldScale; float hudFactor; } f;
gl_Position = f.proj * vec4(inPos * f.worldScale, 0.0, 1.0);
```

---

## 7. Tutorial F — DirectX 11/12 (Windows/Xbox shared code)

Same headers compile with MSVC/clang-cl — ideal when the Android title shares code with Win32/Xbox builds:

```cpp
void OnResize(ID3D11DeviceContext* ctx, HWND hwnd, float dpi, ID3D11Buffer* cb) {
    RECT rc; GetClientRect(hwnd, &rc);
    float wpx=(float)(rc.right-rc.left), hpx=(float)(rc.bottom-rc.top), dens=dpi/160.f;

    static Metrics m;                                    // lifetime!
    m = Metrics::make(wpx/dens, hpx/dens, 0.f, dpi, 1.f, true);
    updateMetrics(m);

    auto v = render::dxViewport(render::Mode::FitAll, wpx, hpx, 1920.f, 1080.f);
    D3D11_VIEWPORT vp{ v.TopLeftX, v.TopLeftY, v.Width, v.Height, v.MinDepth, v.MaxDepth };
    ctx->RSSetViewports(1, &vp);

    struct CBData { float hudScale, worldScale, _pad[2]; } data{ m.scale*m.density,
                                                                std::min(m.fitScale,m.fillScale),{} };
    ctx->UpdateSubresource(cb, 0, nullptr, &data, 0, 0);
}
// DXGI: call OnResize right after swap->ResizeBuffers(...).
```

DX12: same struct feeds `CD3DX12_VIEWPORT`. HLSL mirrors the Vulkan shader above.

---

## 8. Tutorial G — Unity (C#)

**Goal:** family semantics inside Unity, uGUI + 2D camera.

**Step 1 — copy** [`csharp/AppDimensGames/`](../csharp/AppDimensGames/AppDimensGames.cs) (+`.asmdef`) into `Assets/Plugins/`.

**Step 2 — bootstrap (runs before everything):**

```csharp
[DefaultExecutionOrder(-1000)]
public sealed class AppDimensBootstrap : MonoBehaviour {
    int _w, _h;
    void OnEnable() => Publish();
    void Update() { if (Screen.width != _w || Screen.height != _h) Publish(); }

    void Publish() {
        float dpi = UnityEngine.Screen.dpi > 0 ? UnityEngine.Screen.dpi : 160f; // Android fallback!
        float d   = dpi / 160f;
        Screen.Update(UnityEngine.Screen.width / d, UnityEngine.Screen.height / d, dpi);
        _w = UnityEngine.Screen.width; _h = UnityEngine.Screen.height;
    }
}
```

**Step 3 — use anywhere:**

```csharp
panel.sizeDelta  = Vector2.one * MathKernels.Scaled(48f) / canvas.scaleFactor;  // sdp
lockIcon.sizeDelta = Vector2.one * MathKernels.ScaledInvariant(20f) / canvas.scaleFactor; // 🔒 i
score.fontSize   = MathKernels.Auto(16f) / canvas.scaleFactor;                  // BALANCED ⭐
touchTarget      = Units.CmToPx(2f);                                            // 2 cm physical
```

**Step 4 — letterbox camera (2D):** see [`CSHARP-UNITY.md`](CSHARP-UNITY.md) §3 (`LetterboxCamera2D`)
and device-tier selection §4 (`QualityTiers.SelectAssetTier()`).

Godot 4 and MAUI variants are in the same doc (§5–6).

---

## 9. Suffixes — the complete mental model

| Call | Window resized? | Result |
|---|---|---|
| `16.sdp(ctx)` | yes | scales to the NEW window (auto-adjust) |
| `16.sdpa(ctx)` | yes | idem + aspect-ratio refinement |
| `16.sdpi(ctx)` | yes | 🔒 uses FROZEN fullscreen reference (HUD stability) |
| *(true split-screen)* `16.sdpi(ctx)` | — | returns raw base value (family parity) |

Native equivalent: `metrics()` vs `invariantMetrics()` · C#: `Screen.Current` vs `Screen.Invariant`.

## 10. Facilitators & builder (quick reference)

```kotlin
30.sdpRotate(ctx, 44f, Orientation.LANDSCAPE)                 // per-orientation value
12.sdpMode(ctx, 24f, UiModeType.TELEVISION)                   // device-class override
60.sdpQualifier(ctx, 120f, DpQualifier.SMALL_WIDTH, 600)      // threshold override
16.scaledDp().aspectRatio(true).screen(UiModeType.TELEVISION, 32)
              .qualifier(DpQualifier.SMALL_WIDTH, 600, 24).sdp(ctx)
```

Inverters: `32.hdpLw(ctx)`, `32.wdpLh(ctx)`, `32.sdpPh(ctx)`, `32.sdpLw(ctx)`.
Escape hatch: `Number.toDynamicScaledDp/Px(ctx, qualifier, inverter, ignoreMultiWindows, applyAR, k)`.

## 11. Common mistakes (avoid these!)

1. ❌ Forgetting the resize hook (`GameScreen.updateFromContext` / `AppDimensProvider` / `updateMetrics`). Values freeze at the first measurement.
2. ❌ Publishing a local `Metrics` in C++ (dangling address) — use `static`/member.
3. ❌ Using `sdpi` everywhere — `i` is for elements that must NOT adapt (HUD locks), not a default.
4. ❌ Mixing design-unit and px spaces: pick one (letterbox first, then kernels).
5. ❌ Unity: skipping the `dpi == 0` fallback on Android.
6. ❌ Calling satellites' stems without adding their module (`asdp` needs `-auto`).

## 12. Next steps

[MATHEMATICS](MATHEMATICS-AND-CALCULUS.md) (formulas/LaTeX) · [MODULES](MODULES.md) (artifacts + migration 2.x→3.x) · [NATIVE-GAME-ENGINES](NATIVE-GAME-ENGINES.md) (deep dive C/C++/GL/VK/DX/JNI) · [CSHARP-UNITY](CSHARP-UNITY.md) (Unity/Godot/MAUI deep dive) · [PERFORMANCE](../PERFORMANCE.md) (BenchLab) · strategy docs under [`strategies/`](README.md).

[Back to index](README.md)
