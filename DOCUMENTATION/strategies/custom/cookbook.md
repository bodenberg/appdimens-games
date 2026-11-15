# Cookbook

Ready-to-use strategies. Each one shows the Kotlin form, the C# (Unity) form and, where it matters, the JSON form.

## Game HUD: shorter side, aspect-ratio aware, fixed text

```kotlin
val Hud = scalingStrategy("hud") {
    axis = ScalingAxis.MIN
    curve = power(0.85f)
    aspectRatio()
    clamp(0.75f, 2.5f)
    ignoreFontScale()                    // HUD text never follows the system font size
}
val Hud2 = CustomPresets.GAME_HUD        // the same, ready-made
```

```csharp
public static readonly ScalingStrategy Hud = ScalingStrategy.Define("hud", b =>
{
    b.Axis = ScalingAxis.Min;
    b.Curve = ScalingCurve.Power(0.85f);
    b.EnableAspectRatio();
    b.Clamp(0.75f, 2.5f);
    b.IgnoreFontScale();
});
// Unity uGUI
rect.sizeDelta = new Vector2(CustomKernels.Cdp(64f, Hud), CustomKernels.Cdp(64f, Hud));
label.fontSize = Mathf.RoundToInt(CustomKernels.Csp(18f, Hud));
```

## Design frame of 360 dp, never below 85% or above 160%

```kotlin
val Brand = CustomScaleStrategy { (sw / 360f).coerceIn(0.85f, 1.6f) }
```

```csharp
var brand = CustomScaleStrategy.Create(s => s.Clamp(s.Sw / 360f, 0.85f, 1.6f));
```

```json
{ "id": "brand", "reference": { "width": 360, "height": 640 }, "clamp": { "min": 0.85, "max": 1.6 } }
```

## Grow on phones, calm down on tablets

```kotlin
val Calm = CustomScaleStrategy { if (sw < 600f) ratio else 2f + (sw - 600f) / 1000f }
val Calm2 = CustomPresets.TABLET_FRIENDLY        // linear on phones, power(0.5) on tablets, ≤ 1.8
```

```csharp
var calm = CustomScaleStrategy.Create(s => s.Sw < 600f ? s.Ratio : 2f + (s.Sw - 600f) / 1000f);
```

## Sublinear growth (big screens grow slower)

```kotlin
val Soft = CustomScaleStrategy { pow(sw / 360f, 0.75f) }
```

```csharp
var soft = CustomScaleStrategy.Create(s => s.Pow(s.Sw / 360f, 0.75f));
```

```json
{ "id": "soft", "reference": { "width": 360, "height": 640 }, "curve": { "type": "power", "exponent": 0.75 } }
```

## TV and consoles: scale by height, couch distance

```kotlin
val Living = CustomScaleStrategy { if (tv) (height / 540f).coerceIn(1f, 2f) else ratio }
```

```csharp
var living = CustomScaleStrategy.Create(s => s.Tv ? s.Clamp(s.Height / 540f, 1f, 2f) : s.Ratio);
// Unity: consoles report DeviceType.Console → tv. Force it elsewhere:
CustomGames.DeviceClassResolver = m => Application.platform == RuntimePlatform.Android && m.WidthDp >= 960f ? DeviceClass.Tv : (DeviceClass?)null;
```

```json
{ "id": "living", "rules": [ { "when": { "device": "tv" },
  "set": { "axis": "height", "reference": { "width": 960, "height": 540 }, "clamp": { "min": 1, "max": 2 } } } ] }
```

## Split screen: keep the design values

```kotlin
val Steady = scalingStrategy("steady") { ignoreMultiWindow = true }
```

```csharp
var steady = ScalingStrategy.Define("steady", b => b.IgnoreMultiWindow = true);
```

Or pin the HUD to the fullscreen reference while the window is resized: `64.cdpi(Hud)` (Kotlin, px) · `64f.Cdpi(hud)` (C#, dp).

## Readable menu text: grow slowly, cap the font scale

```kotlin
val Body = scalingStrategy("body") { curve = logarithmic(0.4f); clampFontScale(1.3f) }
Text("…", fontSize = 16.csp(Body))
```

```csharp
var body = ScalingStrategy.Define("body", b => { b.Curve = ScalingCurve.Logarithmic(0.4f); b.ClampFontScale(1.3f); });
label.fontSize = Mathf.RoundToInt(16f.Csp(body));
```

## Minimum tap target

```kotlin
val Touch = scalingStrategy("touch") { clampValue(min = 48) }
val buttonPx = 40.cdp(Touch)             // never below 48 dp
```

```csharp
var touch = ScalingStrategy.Define("touch", b => b.ClampValue(min: 48f));
```

## Whole dp only (pixel-art HUD)

```kotlin
val Snap = scalingFormula { round(base * ratio) }
```

```csharp
var snap = ScalingStrategy.Formula(s => s.Round(s.Base * s.Ratio));
```

```json
{ "id": "snap", "formula": "round(base * ratio)" }
```

## Never smaller than the design value

```kotlin
val Floor = scalingFormula { max(base * ratio, base) }
```

```csharp
var floor = ScalingStrategy.Formula(s => s.Max(s.Base * s.Ratio, s.Base));
```

## Landscape phones: a bit smaller

```kotlin
import com.appdimens.games.common.Orientation

val Brand = scalingStrategy("brand") {
    curve = power(0.8f)
    onScreen(maxSmallestWidth = 600f, orientation = Orientation.LANDSCAPE) { multiply(0.9f) }
}
```

```csharp
var brand = ScalingStrategy.Define("brand", b =>
{
    b.Curve = ScalingCurve.Power(0.8f);
    b.OnScreen(r => r.Multiply(0.9f), maxSmallestWidth: 600f, landscape: true);
});
```

## A/B test or remote config

```kotlin
val json = remoteConfig.getString("appdimens_strategies")      // a strategies document
runCatching { ScalingStrategies.loadJson(json) }               // atomic: a bad document changes nothing
    .onFailure { Log.w("dimens", "bad strategies JSON", it) }
val buttonPx = 64.cdp(ScalingStrategies.getOrThrow("hud"))
```

```csharp
try { ScalingStrategies.LoadJson(remoteJson); }                // atomic: a bad document changes nothing
catch (ArgumentException e) { Debug.LogWarning($"bad strategies JSON: {e.Message}"); }
float button = 64f.Cdp(ScalingStrategies.Require("hud"));
```

## One growth law for the game, the launcher and the website

Ship a single `appdimens-strategies.json` with the design tokens, load it in each runtime (`ScalingStrategies.loadAsset` on Android, `ScalingStrategies.LoadJson` in Unity, `loadJson` / `LoadJson` elsewhere), and use the ids only. Android, KMP, iOS, Flutter, React Native, Web and .NET read the same file and get the same numbers.

[Back to the custom docs](README.md)
