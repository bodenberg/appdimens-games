# ⚙️ AppDimens Games — Native Game Engines
### C · C++20/NDK · OpenGL ES · Vulkan · DirectX 11/12 · JNI

> Artifact: `io.github.bodenberg:appdimens-games-native:3.0.1`
> Headers **cross-platform** (Android NDK, Windows, Linux, macOS). Header-only hot paths; the only compiled TU is the optional JNI bridge.

---

## 📁 Files & responsibilities

| File | Provides | Use when |
|---|---|---|
| [`include/appdimens/games/core.h`](../library-native/src/main/cpp/include/appdimens/games/core.h) | `Constants`, `Axis`, `Metrics` (+precomputed factors), `updateMetrics()`, `metrics()`, `invariantMetrics()` | any C++ engine |
| [`include/appdimens/games/math.h`](../library-native/src/main/cpp/include/appdimens/games/math.h) | 13 inline kernels + `Vec2/Vec3`, `toPx`, `scaleVecFit` | sizing anything |
| [`include/appdimens/games/render.h`](../library-native/src/main/cpp/include/appdimens/games/render.h) | `Mode`, `viewportRect`, `glRect`, `vkViewport`, `dxViewport`, `ortho` | GL/VK/DX viewports & matrices |
| [`c/appdimens_games_c.h`](../library-native/src/main/cpp/c/appdimens_games_c.h) | pure C99 mirror (`adg_*`) | C engines (raylib, SDL, custom) |
| [`jni/appdimens_games_jni.cpp`](../library-native/src/main/cpp/jni/appdimens_games_jni.cpp) | `libappdimens_games.so` + `com.appdimens.games.jni.NativeBridge` | Kotlin/Java ↔ native |

---

## 🧠 Core concepts (read first)

```cpp
using namespace appdimens::games;

// 1) BUILD once per window size — all factors precomputed here (exact ln() lives ONLY here):
Metrics m = Metrics::make(/*wDp*/360.f, /*hDp*/800.f, /*swDp*/360.f, /*dpi*/440.f,
                          /*fontScale*/1.f, /*fullscreen*/true);

// 2) PUBLISH — lock-free (release/acquire). All threads see it immediately.
//    ⚠️ LIFETIME RULE: updateMetrics stores the ADDRESS. The Metrics object must outlive
//    the next publish — use `static`, a member of your engine, or a heap instance.
updateMetrics(m);

// 3) READ per frame (wait-free):
const Metrics& live = metrics();            // auto-adjust path (no suffix)
const Metrics& froz = invariantMetrics();   // `i` path — last FULLSCREEN snapshot

// 4) SIZE — single multiply fast lanes:
float hudPx    = math::scaledDp(48.f, live) * live.density;
float playerPx = math::autoDp(64.f, live) * live.density;      // BALANCED ⭐
float hudInvPx = live.fullscreen ? hudPx : 48.f * live.density; // `i` semantics
```

**Suffix mapping (family parity):** no suffix → `metrics()` · `a` → `*m.arMul` (or `scaledArDp`) · `i` → `invariantMetrics()` (returns base value under resized windows).

**Publish triggers:** `onSurfaceChanged` (GL), swapchain recreate / `VK_ERROR_OUT_OF_DATE_KHR` (Vulkan), `IDXGISwapChain::ResizeBuffers` (DX), `onConfigurationChanged` (Activity), fold posture changes.

---

## 1️⃣ C++20 / Android NDK — full game-loop integration

```cpp
// GameEngine.h
#pragma once
#include "appdimens/games/core.h"
#include "appdimens/games/math.h"
#include "appdimens/games/render.h"

class GameEngine {
public:
    // Called from ANativeActivity / android_app / your host on surface create.
    void onSurfaceCreated(int32_t widthPx, int32_t heightPx, float dpi) {
        publish(widthPx, heightPx, dpi);
        designW_ = 1920.f; designH_ = 1080.f;          // your design space
        letterbox_ = render::viewportRect(render::Mode::FitAll,
                                          (float)widthPx, (float)heightPx, designW_, designH_);
    }

    // Called on EVERY resize (rotation, fold, freeform drag, panel open/close).
    void onSurfaceChanged(int32_t widthPx, int32_t heightPx, float dpi) {
        publish(widthPx, heightPx, dpi);
        letterbox_ = render::viewportRect(render::Mode::FitAll,
                                          (float)widthPx, (float)heightPx, designW_, designH_);
        applyViewport();                                // GL/VK/DX specific (below)
    }

    // Called once per frame — HOT PATH (allocation-free).
    void frame(float dtSeconds) {
        const appdimens::games::Metrics& m = appdimens::games::metrics(); // 1 atomic load/frame

        // Sizes in px: single-multiply chains.
        const float playerPx = appdimens::games::math::autoDp(64.f, m) * m.density;
        const float enemyPx  = appdimens::games::math::autoDp(40.f, m) * m.density;
        const float hudPadPx = appdimens::games::math::scaledDp(12.f, m) * m.density;

        // World point (design space → px, letterbox-consistent):
        auto screenPos = appdimens::games::math::scaleVecFit({960.f, 540.f},
                             m, designW_, designH_);
        (void)playerPx; (void)enemyPx; (void)hudPadPx; (void)screenPos.x; (void)screenPos.y;

        // … submit draws with your API of choice …
    }

protected:
    void publish(int32_t wPx, int32_t hPx, float dpi) {
        // px → dp using density; sw = min dimension (rotation-invariant).
        const float density = dpi / 160.f;
        static appdimens::games::Metrics m;              // ← static keeps address valid
        m = appdimens::games::Metrics::make(
                (float)wPx / density, (float)hPx / density,
                /*swDp*/ 0.f /*auto=min*/, dpi, 1.f,
                /*fullscreen*/ true /* false under split-screen/freeform */);
        appdimens::games::updateMetrics(m);
    }

    virtual void applyViewport() = 0;

    float designW_, designH_;
    appdimens::games::render::SurfaceRect letterbox_{};
};
```

> 💡 `swDp = 0` lets `Metrics::make` infer the smallest dimension. Pass the real
> `configuration.smallestScreenWidthDp` when available for exact family parity.

### CMake — adding to an existing NDK project

```cmake
# Header-only core: nothing to build.
target_include_directories(your_game PRIVATE path/to/library-native/src/main/cpp/include)

# Optional JNI bridge (only if you use com.appdimens.games.jni.NativeBridge):
add_subdirectory(path/to/library-native/src/main/cpp)   # defines target appdimens_games
# …or replicate: add_library(appdimens_games SHARED jni/appdimens_games_jni.cpp)
#               target_include_directories(appdimens_games PRIVATE include .)
```

---

## 2️⃣ Pure C99 — raylib & SDL style

```c
/* ---- raylib ---- */
#include "appdimens_games_c.h"
#include "raylib.h"

static adg_metrics g_m;

int main(void) {
    InitWindow(1080, 2400, "game");
    SetConfigFlags(FLAG_WINDOW_RESIZABLE);

    float dpi = 420.f;                       // from ActivityManager/DisplayMetrics via JNI, or AConfiguration
    float dens = dpi / 160.0f;

    adg_metrics m = adg_make(GetScreenWidth()/dens, GetScreenHeight()/dens, 0.f, dpi, 1.0f, true);
    g_m = m;                                  // keep alive; republish on resize

    while (!WindowShouldClose()) {
        if (IsWindowResized()) {              // AUTO-ADJUST HOOK
            g_m = adg_make(GetScreenWidth()/dens, GetScreenHeight()/dens, 0.f, dpi, 1.0f, true);
        }
        BeginDrawing();
            ClearBackground(BLACK);
            // BALANCED gameplay sprite + scaled HUD pad (px):
            float player = adg_auto(&g_m, 64.f, false) * g_m.density;
            float pad    = adg_scaled(&g_m, 12.f) * g_m.density;
            DrawCircle(GetScreenWidth()/2, GetScreenHeight()/2, player*0.5f, SKYBLUE);
            DrawRectangle(pad, pad, 120, 40, Fade(WHITE, 0.2f));
        EndDrawing();
    }
    CloseWindow();
}
```

```c
/* ---- SDL2 ---- */
#include "appdimens_games_c.h"
#include <SDL.h>

int main(int argc, char** argv) {
    SDL_Init(SDL_INIT_VIDEO);
    SDL_Window* win = SDL_CreateWindow("game", 0,0,1080,2400, SDL_WINDOW_RESIZABLE);
    float dpi = 160.f * 2.f;                  // e.g., from SDL_GetDisplayDPI
    float dens = dpi / 160.0f;

    int w, h; SDL_GetWindowSize(win, &w, &h);
    static adg_metrics m;                     // static → address stable for publish
    m = adg_make(w/dens, h/dens, 0.f, dpi, 1.f, true);
    adg_publish(&m);                          /* if you keep the C global-slot variant,
                                                 else pass &m explicitly each frame */

    bool run = true;
    while (run) {
        SDL_Event e;
        while (SDL_PollEvent(&e)) {
            if (e.type == SDL_QUIT) run = false;
            if (e.type == SDL_WINDOWEVENT &&
                e.window.event == SDL_WINDOWEVENT_SIZE_CHANGED) {   // AUTO-ADJUST HOOK
                SDL_GetWindowSize(win, &w, &h);
                m = adg_make(w/dens, h/dens, 0.f, dpi, 1.f, true);
            }
        }
        float hudInv = adg_scaled_a(&m, 48.f) * m.density;   // `a` refinement
        float board  = adg_fit(&m, 100.f) * m.density;       // letterbox content
        (void)hudInv; (void)board;
    }
}
```

> The public C header is stateless by design (`adg_*` take `const adg_metrics*`), so you can own the snapshot wherever it fits best. `adg_publish` shown above is optional glue — omit it and simply pass `&m` around.

---

## 3️⃣ OpenGL ES 3.x (C++) — complete renderer

```cpp
// GlRenderer.h — pairs with GameEngine above.
#include <GLES3/gl3.h>
#include "GameEngine.h"

class GlRenderer final : public GameEngine {
protected:
    void applyViewport() override {
        // Letterbox rect in px → glViewport (content fully visible, black bars elsewhere).
        const auto r = render::glRect(render::Mode::FitAll,
                                      (float)surfaceW_, (float)surfaceH_, designW_, designH_);
        glViewport(r.x, r.y, r.width, r.height);
    }
public:
    void draw() {
        const appdimens::games::Metrics& m = appdimens::games::metrics();

        // 1) Orthographic projection over the DESIGN space (column-major, GLSL-ready):
        float proj[16];
        render::ortho(0.f, 0.f, designW_, designH_, -1.f, 1.f, proj);
        glUniformMatrix4fv(uProj_, 1, GL_FALSE, proj);

        // 2) Sprites sized by strategies (dp→design-units here = same scale):
        const float player = appdimens::games::math::autoDp(64.f, m);
        const float enemy  = appdimens::games::math::interpolatedDp(40.f, m);
        drawQuad(960.f, 540.f, player, player);
        for (int i = 0; i < 8; ++i) drawQuad(300.f + i*180.f, 200.f, enemy, enemy);

        // 3) HUD — invariant element (`i`): anchored to frozen fullscreen metrics.
        const appdimens::games::Metrics& f = appdimens::games::invariantMetrics();
        const float hudIcon = appdimens::games::math::scaledDp(20.f, f);
        drawQuad(designW_ - hudIcon, hudIcon, hudIcon, hudIcon);
    }

private:
    void drawQuad(float cx, float cy, float hw, float hh) { /* VBO update + glDrawArrays */ }
    GLint uProj_ = -1; int32_t surfaceW_=0, surfaceH_=0;
};
```

**Resize flow (GLSurfaceView hosts):**

```kotlin
// Kotlin host — publish once per resize; native reads become consistent next frame.
override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
    GameScreen.updateFromContext(this, fullscreen = true)   // Kotlin hub (same values)
    // OR purely native: call engine.onSurfaceChanged(w, h, dpi) through JNI.
}
```

---

## 4️⃣ Vulkan — swapchain, resize & viewports

Focus points where AppDimens plugs in (init boilerplate omitted):

```cpp
#include "appdimens/games/core.h"
#include "appdimens/games/render.h"

struct FrameUBO {                 // std140 — feed factors ONCE per resize, read in shader
    alignas(16) float proj[16];
    alignas(8)  vec2Like{ float worldScaleXY[2]; }   // e.g., fit/fill factor
    alignas(4)  float hudFactor;                      // precomputed dp→px factor
};

void recreateSwapchain(VkDevice dev, VkSwapchainKHR old, uint32_t newW, uint32_t newH,
                       float dpi, float designW, float designH) {
    vkDestroySwapchain(dev, old, nullptr);
    /* …create new swapchain with newW×newH… */

    // AUTO-ADJUST HOOK #1 — publish new window metrics:
    static appdimens::games::Metrics m;                    // lifetime!
    const float dens = dpi / 160.f;
    m = appdimens::games::Metrics::make(newW/dens, newH/dens, 0.f, dpi, 1.f, true);
    appdimens::games::updateMetrics(m);

    // AUTO-ADJUST HOOK #2 — letterbox viewport (field-compatible with VkViewport):
    const auto vp = appdimens::games::render::vkViewport(
        appdimens::games::render::Mode::FitAll, (float)newW, (float)newH, designW, designH);
    VkViewport viewport{ vp.x, vp.y, vp.width, vp.height, vp.minDepth, vp.maxDepth };
    VkRect2D scissor{ {0,0}, {newW, newH} };
    for (VkCommandBuffer cmd : frameCmds_) {
        vkCmdSetViewport(cmd, 0, 1, &viewport);
        vkCmdSetScissor(cmd, 0, 1, &scissor);
    }

    // Refresh UBO factors once (not per-frame):
    const auto& mm = appdimens::games::metrics();
    ubo_.worldScaleXY[0] = ubo_.worldScaleXY[1] =
        std::min(mm.fitScale, mm.fillScale);     // pick your mode
    ubo_.hudFactor = mm.scale * mm.density;
}

void acquireAndDraw() {
    VkResult r = vkAcquireNextImageKHR(dev, swap, UINT64_MAX, semImgAvail, VK_NULL_HANDLE, &idx);
    if (r == VK_ERROR_OUT_OF_DATE_KHR) {         // RESIZE detected by WSI
        recreateSwapchain(dev, swap, newW, newH, dpi, 1920.f, 1080.f);
        return;
    }
    /* …record: bind pipeline → vkCmdSetViewport(vp) → push/UBO → draw sprites… */
    /* present; on VK_SUBOPTIMAL_KHR schedule recreate next frame */
}
```

**Sprite vertex shader consuming the UBO (GLSL):**

```glsl
layout(std140, binding = 0) uniform Frame {
    mat4  proj;
    vec2  worldScale;
    float hudFactor;
} f;
void main() {
    gl_Position = f.proj * vec4(inPosition * f.worldScale, 0.0, 1.0);
}
```

> `render::vkViewport()` returns a struct whose field order matches `VkViewport`
> (`x,y,width,height,minDepth,maxDepth`) — aggregate-init directly, zero conversion cost.

---

## 5️⃣ DirectX 11 / 12 — Windows & Xbox ports (shared-code scenarios)

Same headers compile unmodified on MSVC/clang-cl — useful when your Android title shares the rendering layer with Win32/Xbox builds (or runs through ANGLE-on-Windows).

```cpp
#include "appdimens/games/core.h"
#include "appdimens/games/render.h"

void OnWindowResize(ID3D11DeviceContext* ctx, ID3D11Buffer* frameCB,
                    HWND hwnd, float dpi, float dw, float dh) {
    RECT rc; GetClientRect(hwnd, &rc);
    const float wpx = (float)(rc.right - rc.left), hpx = (float)(rc.bottom - rc.top);

    // 1) Publish metrics (AUTO-ADJUST HOOK):
    static appdimens::games::Metrics m;                       // lifetime!
    const float dens = dpi / 160.f;
    m = appdimens::games::Metrics::make(wpx/dens, hpx/dens, 0.f, dpi, 1.f, true);
    appdimens::games::updateMetrics(m);

    // 2) Viewport — field-compatible with D3D11_VIEWPORT/D3D12_VIEWPORT:
    const auto v = appdimens::games::render::dxViewport(
        appdimens::games::render::Mode::FitAll, wpx, hpx, dw, dh);
    D3D11_VIEWPORT vp{ v.TopLeftX, v.TopLeftY, v.Width, v.Height, v.MinDepth, v.MaxDepth };
    ctx->RSSetViewports(1, &vp);

    // 3) Constant buffer with factors (single upload per resize):
    struct CB { float hudScale; float worldScale; float _pad[2]; } cb{
        /*hudScale=*/ m.scale * m.density,
        /*worldScale=*/ std::min(m.fitScale, m.fillScale), {0,0} };
    ctx->UpdateSubresource(frameCB, 0, nullptr, &cb, 0, 0);

    // DX12: identical struct → CD3DX12_VIEWPORT(v.TopLeftX, v.TopLeftY, v.Width, v.Height);
}

// DXGI swapchain path:
if (swap->ResizeBuffers(0, newW, newH, DXGI_FORMAT_UNKNOWN, 0) == S_OK)
    OnWindowResize(ctx, cb, hwnd, dpi, 1920.f, 1080.f);
```

> HLSL consumes the same constants: `pos.xy *= worldScale;` — one multiply, matching the CPU-side contract.

---

## 6️⃣ JNI bridge — Kotlin/Java ↔ native

```kotlin
// One-time availability probe (graceful fallback to Kotlin kernels):
val bridge = if (com.appdimens.games.jni.NativeBridge.isAvailable)
    com.appdimens.games.jni.NativeBridge else null

// In your GL/Vulkan render THREAD (allocation-free):
fun onSurfaceChanged(wDp: Float, hDp: Float, swDp: Float, dpi: Float) {
    bridge?.updateMetrics(wDp, hDp, swDp, dpi, fontScale = 1f, fullscreen = true)
}
fun frame(): Float {
    val playerPx = bridge?.auto(64f, ar = false) ?: kotlinFallbackAuto(64f)
    val hudInvPx = bridge?.scaledInvariant(48f) ?: 48f           // `i`
    return playerPx + hudInvPx
}
```

```kotlin
private fun kotlinFallbackAuto(dp: Float): Float =
    com.appdimens.games.math.GameMath.toPx(
        com.appdimens.games.math.GameMath.calculateAutoDp(dp, com.appdimens.games.core.GameScreen.metrics()),
        com.appdimens.games.core.GameScreen.metrics())
```

Symbol names follow the standard JNI layout (`Java_com_appdimens_games_jni_NativeBridge_*`) — no `System.loadLibrary` needed beyond `NativeBridge.isAvailable`'s internal load.

---

## 🧵 Thread-safety & performance rules

| Rule | Why |
|---|---|
| **Publish rarely, read freely.** `updateMetrics` only at real resizes; `metrics()` is one relaxed-acquire atomic load. | ~2 ns reads, no locks ever. |
| **Cache `const Metrics& m = metrics();` once per frame**, not per sprite. | Avoid repeated atomics in tight loops. |
| **Never store the reference across publishes.** Copy by value if you must snapshot mid-resize. | Publisher swaps the slot atomically. |
| **Give published `Metrics` static/member lifetime.** | `updateMetrics` keeps its ADDRESS. |
| Keep exact `ln()` inside `Metrics::make` only. | Hot paths are multiplies; precision preserved where it matters. |
| Custom sensitivity `k`: pass `-1.f` to use family default `K=0.08/30`. | Parity with dynamic/kmp. |

## ⚠️ Pitfalls checklist

- [ ] Published `Metrics` outlives usage (use `static` or engine member).
- [ ] Every resize path calls publish **before** reading: GL `onSurfaceChanged`, Vulkan `OUT_OF_DATE/resize`, DX `ResizeBuffers`, Activity `onConfigurationChanged`.
- [ ] Split-screen/freeform: publish with `fullscreen=false` → `i` variants anchor to the frozen fullscreen snapshot automatically.
- [ ] `swDp`: pass the real smallest-width dp when available; otherwise `0` infers `min(w,h)` (rotation-invariant).
- [ ] Letterbox first (`render::viewportRect/glRect/vkViewport/dxViewport`), then size sprites with kernels — order avoids double-scaling mistakes.

[Back to index](README.md) · [Mathematics](MATHEMATICS-AND-CALCULUS.md) · [Sample app](../sample/)
