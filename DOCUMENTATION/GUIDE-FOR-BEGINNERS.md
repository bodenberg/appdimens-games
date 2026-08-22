# 📘 AppDimens Games — Guide for Beginners (Game Developers)

## The 3 pillars

1. **One snapshot per window** — the library measures your game's window once and pre-computes everything.
2. **One multiply per value** — asking "how big is my player?" costs about as much as adding two floats.
3. **Auto-adjust, except `i`** — resize the window and every value follows; add `i` to pin HUD elements.

## Choose by element type 🎯

| You're sizing… | Use | One-liner |
|---|---|---|
| HUD buttons/icons | DEFAULT | `48.sdp(ctx)` / `AppDimensGamesJava.hud(48f)` |
| Player/enemy/projectile | BALANCED ⭐ | `64f.asdp(ctx)` / `playerSize(64f)` |
| World bounds/containers | PERCENTAGE | `200f.psdp(ctx)` · `10.spaceW(ctx)` |
| Background/parallax | FILL | `100f.flsdp(ctx)` |
| Puzzle board/map | FIT | `AppDimensGamesJava.viewportContent(100f)` |
| Score/dialog text | FLUID | `16f.fsdp(ctx)` |
| TV/large-screen UI | LOGARITHMIC | `50f.logsdp(ctx)` |
| Touch targets | DIAGONAL | `48f.dgsdp(ctx)` · physical: `2f.cmPx(ctx)` |
| Pixel-art sprites | NONE | raw base value |

Numeric example — phone 360 dp sw:
* scaled: $16 × 360/300 = 19.2$
* balanced(600 dp tablet): $40 × (1.6 + 0.4\ln(1.4)) = 69.38$

## Wire-up in 30 seconds

```kotlin
class MyGame : Activity() {
    override fun onConfigurationChanged(c: Configuration) {
        super.onConfigurationChanged(c)
        GameScreen.updateFromContext(this)     // ← that's the auto-adjust hook
    }
}
// loop code anywhere:
val enemy = AppDimensGamesJava.playerSize(32f)
```

Compose games: wrap content in `AppDimensGamesProvider { ... }` — done.

Native engines: call `updateMetrics(Metrics::make(...))` from surface callbacks (see NATIVE-GAME-ENGINES.md).

## Suffixes

```kotlin
16.sdp(ctx)    // follows window resizes
16.sdpa(ctx)   // + aspect-ratio refinement
16.sdpi(ctx)   // 🔒 invariant: pinned to fullscreen reference
```
