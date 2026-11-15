# AppDimens Games — Android

Gradle build of the Android artifacts (Kotlin, Jetpack Compose, Java, NDK), published to Maven Central as
`io.github.bodenberg:appdimens-games*`.

```bash
cd android
./gradlew assembleDebug assembleRelease testDebugUnitTest
```

| Module | Artifact |
|---|---|
| `library/` | `appdimens-games` — core: `GameScreen`, `GameMetrics`, `GameMath`, Compose and code APIs, world layer |
| `library-{auto,density,diagonal,fill,fit,fluid,interpolated,logarithmic,percent,perimeter,power,resize,units}/` | `appdimens-games-{strategy}` |
| `library-custom/` | `appdimens-games-custom` — custom scaling strategies |
| `library-native/` | `appdimens-games-native` — JNI + the C/C++ SDK from [`../native`](../native) |
| `library-bom/` | `appdimens-games-bom` |
| `sample/` | demo game (Compose, OpenGL ES, Vulkan surface) |
| `benchlab/` | on-device benchmarks |

Other platforms live next to this folder: [`../native`](../native) (C/C++), [`../engines`](../engines) (Unity,
Unreal, Godot) and [`../dotnet`](../dotnet). Guides: [`../DOCUMENTATION`](../DOCUMENTATION/README.md).
