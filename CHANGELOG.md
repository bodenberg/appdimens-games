# Changelog

All notable changes to **AppDimens Games** are documented here.
This project follows [Semantic Versioning](https://semver.org/).

## [3.0.1] — 2026-08-28

### Fixed — Compose version flexibility for consumers

**Bug:** When a consumer project used a different AndroidX Compose version than
the one this library was compiled against (`composeRuntime = "1.9.0"`),
consumers reported *"module not found"* / build-time class resolution errors.

**Root cause:** the Compose runtime/ui artifacts were declared as
`compileOnly` and pinned to a specific version. Although `compileOnly` does
not leak classes transitively, the AAR was still compiled against the
1.9.0 ABI and consumers on 1.7.x / 1.8.x could not resolve the symbols the
library's extension functions reference (e.g. `Dp`, `Composable`, `Modifier`
stubs). No BOM was being published transitively, so consumers had no
way to align versions automatically.

**Fix:** the AndroidX Compose BOM is now published as an `api` dependency
of every library module. The actual Compose artifacts (`runtime`, `ui`)
remain `compileOnly` so non-Compose consumers are not forced to pull
Compose. The result:

- Any Compose 1.x version chosen by the consumer (1.7, 1.8, 1.9, …) is
  accepted: the BOM is the version source of truth.
- Non-Compose consumers are unaffected (Compose stays `compileOnly`).
- 3.0.1 is binary-compatible with 3.0.0.

### Modules touched
`library`, `library-auto`, `library-density`, `library-diagonal`,
`library-fill`, `library-fit`, `library-fluid`, `library-interpolated`,
`library-logarithmic`, `library-percent`, `library-perimeter`,
`library-power`, `library-resize`, `library-units`.

## [3.0.0] — 2026-08

### Added
- Family conversion of AppDimens (`appdimens-dynamic` 3.x, `appdimens-kmp` 1.x)
  to game development. Same API vocabulary, same suffixes, same facilitators.
- 13 scaling strategies: `sdp`, `asdp` (auto), `flsdp`, `ftsdp`, `fsdp`,
  `logsdp`, `dgsdp`, `psdp`, `pwsdp`, `prsdp`, `dsdp`, `isdp`, `esdp`.
- True native layer: header-only C++20 core + pure C99 header + JNI;
  OpenGL ES / Vulkan / DirectX viewport interop.
- Game world layer: `Vec2/Vec3`, `Rect`, `ViewportMode` letterbox/crop,
  world↔screen mapping.
- `i` suffix = `ignoreMultiWindows` invariant under split-screen/freeform.
- Snapshot engine: precomputed factors replace hash-per-call gateway.
- AGP 9.x · compileSdk 37 · Kotlin 2.x.
