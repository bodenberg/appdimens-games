# 🎯 AppDimens Games — C# / Unity (and Godot/MAUI)

> File: [`csharp/AppDimensGames/AppDimensGames.cs`](../csharp/AppDimensGames/AppDimensGames.cs) (+ `.asmdef` for Unity)
> Single file, no dependencies, allocation-free hot paths, bit-parity with Kotlin/C++.

## Unity quick start

1. Copy the `AppDimensGames` folder into `Assets/Plugins/`.
2. Update the screen snapshot when the resolution changes:

```csharp
void OnRectTransformDimensionsChange() => UpdateScreen();
// or in a bootstrap MonoBehaviour:
void Start()  => UpdateScreen();
void Update() { /* cheap; only republish on real change */ }

void UpdateScreen()
{
    var cam = Camera.main;
    float dpi = Screen.dpi > 0 ? Screen.dpi : 160f;
    // Convert px → dp (density = dpi/160):
    float wDp = Screen.width  / (dpi / 160f);
    float hDp = Screen.height / (dpi / 160f);
    AppDimensGames.Screen.Update(wDp, hDp, dpi);
}
```

3. Use anywhere (gameplay code, UI builders):

```csharp
float player = MathKernels.Auto(64f);            // BALANCED ⭐
float hud    = MathKernels.Scaled(48f);          // scaled fast lane
float hudAr  = MathKernels.ScaledAr(48f);        // `a`
float hudInv = MathKernels.ScaledInvariant(48f); // `i` — frozen fullscreen ref
float bg     = MathKernels.Fill(100f);           // cover
float board  = MathKernels.Fit(100f);            // letterbox
float touch  = Units.CmToPx(2f);                 // physical 2 cm target
```

## Letterbox viewport (camera setup)

```csharp
var r = World.ViewportRect(Mode.FitAll, Screen.width, Screen.height, 1920f, 1080f);
cam.aspect = r.W / r.H;
cam.orthographicSize = 1080f * 0.5f * (r.H / 1080f);
// Or use r directly as a RawImage/scissor rect.
```

## Godot / MAUI

The file is engine-agnostic (`#if UNITY_5_3_OR_NEWER` guards Unity-only bits). Feed `Screen.Update(...)` from your platform's resize callback and use `MathKernels.*` identically.

## Semantics

Same suffix contract as the family: values follow the live window automatically; use `ScaledInvariant` (`i`) to keep HUD elements anchored to the frozen fullscreen reference under split-screen/windowed play.

[Back to index](README.md)
