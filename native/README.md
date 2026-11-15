# AppDimens Games — native C / C++ SDK

Header-only core plus an optional shared library with a stable C ABI, for every platform: Windows (MSVC,
clang-cl, MinGW), Proton/Wine, Linux x64/arm64, Steam Deck, macOS, iOS/tvOS, Android NDK, WebAssembly and consoles.

| Path | What |
|---|---|
| `include/appdimens/appdimens.h` | portable C99 core (12 strategies, tokens) — shared with Qt, GTK and LVGL |
| `include/appdimens/games.h` | viewports, OpenGL/Vulkan/D3D/Metal/WebGPU projections, FOV, upscaling, HUD, LOD |
| `include/appdimens/appdimens_custom.h` | custom scaling strategies (shared JSON, text formulas) |
| `include/appdimens/appdimens_games_api.h` | C ABI of the shared library (Rust, Zig, Python, Lua, Swift, Java Panama) |
| `src/` · `tests/` | shared library and tests |

```bash
cmake -S native -B build -DCMAKE_BUILD_TYPE=Release && cmake --build build && ctest --test-dir build
cmake -S native -B build-win -DCMAKE_TOOLCHAIN_FILE=cmake-mingw64.cmake   # Windows from Linux (Proton path; resolved in native/)
```

In CMake: `add_subdirectory(native)` → `AppDimens::games` (shared library) or `AppDimens::games_headers`.
The Android AAR (`../android/library-native`) and the Unreal plugin (`../engines/unreal`) build on these headers.
Guides: [`../DOCUMENTATION/CROSS-PLATFORM.md`](../DOCUMENTATION/CROSS-PLATFORM.md) ·
[`GRAPHICS-APIS.md`](../DOCUMENTATION/GRAPHICS-APIS.md) · [`UPSCALING.md`](../DOCUMENTATION/UPSCALING.md) ·
[`CUSTOM-NATIVE.md`](../DOCUMENTATION/CUSTOM-NATIVE.md).
