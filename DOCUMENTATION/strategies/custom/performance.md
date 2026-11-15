# Performance

Custom strategies cost the same as the built-ins when they only depend on the screen. Which form you pick decides the cost per value, in Kotlin and in C#.

| Form | Per window | Per value |
|---|---|---|
| `CustomScaleStrategy`, any curve, rules, factor bounds, aspect ratio | lambda / curve once | one memo check + one multiply |
| text formula shaped `base * E` (E reads no value) | `E` once | one memo check + one multiply |
| `scalingFormula { … }` / `ScalingStrategy.Formula(s => …)`, other text formulas, `transform`, `clampValue` | rules and factor once | the formula, on every call |

## How it works

- **Memo per window.** Each strategy keeps the last resolved window (rules applied, factor computed). The token is the snapshot itself: `GameMetrics` in Kotlin (identity, then equality), `Metrics` in C#. A new snapshot (rotation, resize, split screen, fold, font scale) resolves again once; a hit is allocation-free.
- **No per-value table.** Unlike the Android and KMP modules, the Games facades do not store per-value results: a value-dependent strategy runs its formula on every call. Keep those formulas cheap, or prefer the factor form.
- **Fast path for text.** `base * E` is detected when the formula is parsed; `E` is computed once per window and the result is bit for bit the same as evaluating it per value.
- **Live vs invariant.** `cdp` / `Cdp` read the live window; `cdpi` / `Cdpi` read the frozen fullscreen reference. In fullscreen both are the same snapshot. Each strategy has a single memo slot, so while the live window differs from the reference (split screen, freeform), alternating `cdp` and `cdpi` on the **same** strategy resolves again at every switch: give the invariant HUD its own instance (for example `Hud.extend("hudPinned") { }`).

## Guidelines

1. Prefer a factor (`CustomScaleStrategy`, a curve, rules) over a per-value formula when the math is "base × something about the screen".
2. Use `clampValue` only for real absolute limits (tap targets ≥ 48 dp, max text size).
3. Declare strategies once: a top-level `val`, `object GameStrategies`, a `static readonly` field, or a JSON document loaded at startup. Never build them in `Update()`, `onDrawFrame` or a composable body.
4. Keep lambdas pure and cheap. They run on memo misses (factor) or on every call (formula), on the calling thread.
5. Text formulas inline (`64.cdp("…")`, `64f.Cdp("…")`) are shared per text, so they are fine in a frame loop. Building strategies from dynamic strings is not: past 256 distinct texts each call parses a new strategy.
6. In a hot loop, resolve once and reuse: `val r = Hud.resolveLive(); r.scale(64f)` (C#: `var r = hud.Resolve(Screen.Current); r.Scale(64f)`).

[Back to the custom docs](README.md)
