# ⚙️ AppDimens Games — Native Game Engines (C · C++ · JNI · OpenGL · Vulkan · DirectX)

> Artifact: `io.github.bodenberg:appdimens-games-native:3.0.0`
> Headers are **cross-platform**: the same files compile on Android NDK, Windows (DirectX/ANGLE), Linux and macOS. No engine lock-in.

## Layout

```
library-native/src/main/cpp/
├── include/appdimens/games/
│   ├── core.h      Metrics snapshot + lock-free publish (header-only)
│   ├── math.h      13 inline kernels + Vec2/Vec3 (header-only)
│   └── render.h    GL/Vulkan/DirectX viewport interop + ortho matrix
├── c/appdimens_games_c.h   Pure C99 API (single header)
└── jni/appdimens_games_jni.cpp   JNI bridge (the only compiled TU)
```

## 1) C++ (custom engines / NDK)

```cpp
#include "appdimens/games/core.h"
#include "appdimens/games/math.h"
using namespace appdimens::games;

// Publish once per resize (any thread; release/acquire safe):
void onResize(float wDp, float hDp, float swDp, float dpi) {
    static Metrics m = Metrics::make(wDp, hDp, swDp, dpi);
    updateMetrics(m);
}

// Frame loop — single-multiply fast lanes:
float hud = math::scaledDp(48.f, metrics());
float ar  = math::scaledArDp(48.f, metrics());          // `a`
float inv = metrics().fullscreen ? hud : 48.f;           // `i` semantics:
                                                          // use invariantMetrics() for frozen ref.
float px  = math::toPx(hud, metrics());
```

## 2) Pure C (raylib, SDL, custom C engines)

```c
#include "appdimens_games_c.h"

adg_metrics m = adg_make(360.f, 800.f, 0.f, 440.f, 1.0f, true);
float player = adg_auto(&m, 64.f, false);   // BALANCED
float hud_px = adg_to_px(&m, adg_scaled_a(&m, 48.f));
```

## 3) JNI (Kotlin/Java ↔ native)

```kotlin
// library-native exposes NativeBridge — call from render threads:
NativeBridge.updateMetrics(wDp, hDp, swDp, dpi, fontScale, fullscreen = true)
val playerPx = NativeBridge.auto(64f, ar = false)
val hudInv   = NativeBridge.scaledInvariant(48f)   // `i` variant
if (!NativeBridge.isAvailable) { /* fallback to Kotlin kernels */ }
```

## 4) OpenGL ES

```cpp
auto r = render::glRect(render::Mode::FitAll, surfaceW, surfaceH, designW, designH);
glViewport(r.x, r.y, r.width, r.height);

float ortho[16];
render::ortho(0.f, 0.f, designW, designH, -1.f, 1.f, ortho); // column-major mat4
```

## 5) Vulkan

```cpp
// Field-layout compatible with VkViewport:
render::VkLikeViewport vp =
    render::vkViewport(render::Mode::FitAll, extent.width, extent.height, 1920.f, 1080.f);
VkViewport vk{vp.x, vp.y, vp.width, vp.height, vp.minDepth, vp.maxDepth};
vkCmdSetViewport(cmd, 0, 1, &vk);

// Swapchain recreate → republish metrics (auto-adjust):
onResize(newWdp, newHdp, swDp, dpi);
```

## 6) DirectX 11/12 (Windows/Xbox ports via shared code or ANGLE scenarios)

```cpp
// Field-compatible with D3D11_VIEWPORT / D3D12_VIEWPORT:
render::DxViewport vp =
    render::dxViewport(render::Mode::FitAll, wpx, hpx, dw, dh);
D3D11_VIEWPORT d3d{vp.TopLeftX, vp.TopLeftY, vp.Width, vp.Height, vp.MinDepth, vp.MaxDepth};
context->RSSetViewports(1, &d3d);
```

## Performance notes

* Hot math is `inline` in headers: zero call overhead, zero allocation, no exceptions (`-fno-exceptions`).
* Snapshot publish uses `std::atomic<const Metrics*>` with acquire/release — reads are wait-free on all CPUs.
* The only compiled translation unit is the JNI bridge; everything else is header-only.

See [CSHARP-UNITY.md](CSHARP-UNITY.md) for the managed path and [MATHEMATICS-AND-CALCULUS.md](MATHEMATICS-AND-CALCULUS.md) for formulas.

[Back to index](README.md)
