# 🎯 AppDimens Games — C# · Unity · Godot · MAUI

> File: [`csharp/AppDimensGames/AppDimensGames.cs`](../csharp/AppDimensGames/AppDimensGames.cs) (+ `.asmdef` for Unity)
> Single file · zero dependencies · allocation-free hot paths · bit-parity with Kotlin/C++ core.

---

## 📡 API surface (same semantics as the family)

| Family concept | C# member |
|---|---|
| publish snapshot (`GameScreen.updateFromContext`) | `AppDimensGames.Screen.Update(wDp, hDp, dpi[, fontScale, fullscreen])` |
| live snapshot | `Screen.Current` |
| `i` invariant (frozen fullscreen) | `Screen.Invariant` |
| `sdp` fast lane | `MathKernels.Scaled(b)` |
| `sdpa` | `MathKernels.ScaledAr(b)` |
| `sdpi` | `MathKernels.ScaledInvariant(b)` |
| `wdp/hdp` | `MathKernels.Width/Height(b)` |
| `psdp` literal % | `MathKernels.PercentOfWidth(p)` |
| `pwsdp` / `fsdp` / `asdp` ⭐ | `Power` / `Fluid` / `Auto` |
| `logsdp/isdp/dgsdp/prsdp/dsdp` | `Logarithmic` / `Interpolated` / `Diagonal` / `Perimeter` / `Density` |
| `ftsdp` / `flsdp` | `Fit` / `Fill` |
| dp→px | `MathKernels.ToPx(dp)` |
| letterbox/crop viewport | `World.ViewportRect(mode, swpx, shpx, designW, designH)` |
| physical units (`cmPx`) | `Units.CmToPx/MmToPx/InchToPx` |

---

## 1️⃣ Unity — bootstrap (wire-up once)

```csharp
// AppDimensBootstrap.cs
using AppDimensGames;
using UnityEngine;

[DefaultExecutionOrder(-1000)]   // run before everything reads metrics
public sealed class AppDimensBootstrap : MonoBehaviour
{
    public static Metrics M => Screen.Current;    // convenience accessor

    void OnEnable()  { Publish(); }
    void Update()    { if (ResolutionChanged()) Publish(); }          // cheap guard below
    void OnRectTransformDimensionsChange() => Publish();              // UI resizes

    bool ResolutionChanged() =>
        Screen.width != _w || Screen.height != _h;
    int _w, _h;

    public void Publish()
    {
        // ⚠️ Screen.dpi can be 0 on some Android devices → family fallback:
        float dpi = UnityEngine.Screen.dpi > 0 ? UnityEngine.Screen.dpi : 160f;
        float density = dpi / 160f;

        float wDp = UnityEngine.Screen.width  / density;
        float hDp = UnityEngine.Screen.height / density;
        bool fullscreen = !IsSplitScreenLike(wDp, hDp);

        Screen.Update(wDp, hDp, dpi, fontScale: 1f, fullscreen);
        _w = UnityEngine.Screen.width; _h = UnityEngine.Screen.height;
    }

    // Heuristic (family parity): window much smaller than display ⇒ resized container.
    static bool IsSplitScreenLike(float wDp, float hDp)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        // Optional: query Android smallest-width via JNI; heuristic suffices in practice.
#endif
        return false;
    }
}
```

> Every value auto-adjusts after `Publish()` — call it from `OnEnable`, resolution changes, orientation change and fold posture events. That's the entire wiring.

---

## 2️⃣ Unity UI (uGUI) — scaled panels, invariant HUD

```csharp
using AppDimensGames;
using UnityEngine;

public sealed class HudPanel : MonoBehaviour
{
    [SerializeField] RectTransform panel;      // follows window (`sdp`)
    [SerializeField] RectTransform lockIcon;   // frozen reference (`i`)
    [SerializeField] TMPro.TextMeshProUGUI score;

    void OnEnable() { Apply(); }
    void Update()   { Apply(); }               // re-applies only when values differ (guard below)

    float _lastW = -1f;
    void Apply()
    {
        var m = Screen.Current;
        if (Mathf.Approximately(m.Scale, _lastW)) return;   // skip work between resizes
        _lastW = m.Scale;

        // 48.sdp → px → convert once to your canvas scale:
        float px = MathKernels.Scaled(48f);                 // single multiply
        float scaleFactor = GetComponentInParent<Canvas>().scaleFactor;
        panel.sizeDelta = new Vector2(px, px * 2f) / scaleFactor;

        // 🔒 Invariant HUD element (`i`) — anchored to fullscreen reference:
        float inv = MathKernels.ScaledInvariant(20f);
        lockIcon.sizeDelta = Vector2.one * inv / scaleFactor;

        score.fontSize = MathKernels.Auto(16f) / scaleFactor;   // BALANCED text ⭐
    }
}
```

---

## 3️⃣ Unity 2D — letterbox camera & world sprites

```csharp
using AppDimensGames;
using UnityEngine;

/// Fits a 1920×1080 design space inside any aspect ratio (letterbox/crop).
[RequireComponent(typeof(Camera))]
public sealed class LetterboxCamera2D : MonoBehaviour
{
    public float designW = 1920f, designH = 1080f;
    public Mode mode = Mode.FitAll;
    Camera cam;

    void Awake()  { cam = GetComponent<Camera>(); Apply(); }
    void LateUpdate() => Apply();

    void Apply()
    {
        var r = World.ViewportRect(mode, UnityEngine.Screen.width, UnityEngine.Screen.height,
                                   designW, designH);

        // Orthographic height that shows exactly r.H pixels of design space:
        float worldPerPx = 1f;                       // 1 design unit = 1 px here
        cam.orthographicSize = r.H * worldPerPx * 0.5f;
        cam.aspect = r.W / Mathf.Max(1f, r.H);

        // Optional: offset your content root by (r.X, r.Y) when bars appear.
    }
}
```

```csharp
using AppDimensGames;
using UnityEngine;

/// Spawns gameplay sprites sized by strategies (BALANCED default for gameplay).
public sealed class EnemySpawner : MonoBehaviour
{
    [SerializeField] GameObject enemyPrefab;

    void Start()
    {
        float enemyWorld = DesignToWorldUnits(MathKernels.Auto(40f));   // ⭐
        float bgCover    = DesignToWorldUnits(MathKernels.Fill(100f));  // backgrounds

        var go = Instantiate(enemyPrefab, transform);
        go.transform.localScale = Vector3.one * enemyWorld;
    }

    /// Converts design-px (from kernels) to world units under an ortho camera.
    static float DesignToWorldUnits(float px)
    {
        float orthoH = Camera.main.orthographicSize * 2f;
        float screenH = UnityEngine.Screen.height;
        return px * (orthoH / screenH);
    }
}
```

**Physical touch targets** (accessibility-critical):

```csharp
float buttonPx = Units.CmToPx(2f);                    // exactly 2 cm across devices
btn.GetComponent<RectTransform>().sizeDelta =
    Vector2.one * buttonPx / canvas.scaleFactor;
```

---

## 4️⃣ Unity — device-tier asset selection & difficulty scaling

```csharp
using AppDimensGames;
using UnityEngine;

public static class QualityTiers
{
    public static int SelectAssetTier()
    {
        float sw = Screen.Current.SmallestWidthDp;         // rotation-invariant
        if (sw >= 720f) return 2;   // hi-res atlases
        if (sw >= 600f) return 1;   // tablet atlases
        return 0;                   // phone atlases
    }

    // POWER strategy doubles as a smooth difficulty curve knob:
    public static float DifficultyMultiplier(float baseDifficulty)
        => MathKernels.Power(baseDifficulty, ar: false);   // (sw/300)^0.75
}
```

**DOTS/Burst note:** `MathKernels` are pure static float math over a class snapshot — safe to read inside `[BurstCompile]` jobs by copying the few factors you need into a blittable struct during system setup:

```csharp
[BurstCompile]
struct ScaleJob : IJobParallelForTransform
{
    public float HudFactor;     // copied from Screen.Current.Scale * Density
    public void Execute(int i, TransformAccess t) { /* pure math */ }
}
```

---

## 5️⃣ Godot 4 (C#) — viewport-driven

```csharp
using AppDimensGames;
using Godot;

public partial class GameRoot : Node2D
{
    const float DesignW = 1920f, DesignH = 1080f;
    float _dpi = 160f;                 // feed from OS/display info when available

    public override void _Ready()
    {
        Publish();
        GetViewport().SizeChanged += Publish;      // AUTO-ADJUST HOOK
    }

    void Publish()
    {
        var size = GetViewport().GetVisibleRect().Size;   // pixels
        float dens = _dpi / 160f;
        Screen.Update(size.X / dens, size.Y / dens, _dpi, 1f, fullscreen: true);

        // Letterbox content rect in px (for a SubViewportContainer, etc.):
        var r = World.ViewportRect(Mode.FitAll, size.X, size.Y, DesignW, DesignH);
        GD.Print($"content {r.W}x{r.H} at {r.X},{r.Y}");
    }

    public override void _Process(double delta)
    {
        // Sprites sized like Kotlin `64.asdp`:
        float playerPx = MathKernels.Auto(64f);
        GetNode<Sprite2D>("Player").Scale =
            Vector2.One * PxToUnits(playerPx);
    }

    float PxToUnits(float px)
    {
        float screenH = GetViewport().GetVisibleRect().Size.Y;
        float orthoH = GetNode<Camera2D>("Camera").GetScreenCenterPosition().Y; // placeholder-safe
        return screenH > 0f ? px / screenH : 0f;   // world-units-per-px under unit zoom
    }
}
```

> Keep `Publish()` as the single source of truth — identical pattern across Unity/Godot/MAUI.

---

## 6️⃣ MAUI / desktop-adjacent C#

```csharp
using AppDimensGames;

protected override void OnSizeAllocated(double w, double h)   // or Window.SizeChanged
{
    base.OnSizeAllocated(w, h);
    double density = DeviceDisplay.MainDisplayInfo.Density;   // ≥1
    float dpi = 160f * (float)density;

    Screen.Update((float)(w / density), (float)(h / density), dpi,
                  fontScale: (float)DeviceDisplay.MainDisplayInfo.Density /*approx*/,
                  fullscreen: true);

    hudButton.WidthRequest = MathKernels.Scaled(48f);         // px
    scoreLabel.FontSize    = MathKernels.Auto(16f);           // sp-like
}
```

---

## 🧵 Performance rules (managed)

| Rule | Why |
|---|---|
| Cache locals in hot loops: `var m = Screen.Current; float f = m.Scale;` | property hops are virtual-free but avoid repeated field walks |
| Guard re-application until values actually change (see `_lastW` above) | layout writes dominate cost |
| No allocations in `MathKernels.*` — safe per-frame | structs & statics only |
| Publish on resize events only | mirrors native contract |

## ⚠️ Pitfalls checklist

- [ ] `Screen.Update(...)` called **before** first kernel read (bootstrap order −1000).
- [ ] Android: handle `Screen.dpi == 0` (fallback `160f`, or fetch real density).
- [ ] Split-screen/windowed play: pass `fullscreen:false` → `ScaledInvariant` anchors automatically.
- [ ] uGUI: divide px by `canvas.scaleFactor`; TextMeshPro expects pt-scale — apply once at setup.
- [ ] Prefer `World.ViewportRect(FitAll/FitWidth/FitHeight/Crop)` over manual camera math.

[Back to index](README.md) · [Native engines](NATIVE-GAME-ENGINES.md) · [Mathematics](MATHEMATICS-AND-CALCULUS.md)
