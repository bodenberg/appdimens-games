# Resize (container auto-fit)

> **Artifact:** `io.github.bodenberg:appdimens-games-resize:3.0.0`

## What it is
Dynamic score bars, variable text, adaptive panels in HUD.

## Calculation used
`step table + binary search `O(log n)` over `[min,max]` against the inner box`

Constants: `W₀=300 · H₀=533 · diag₀=611.6305 · perim₀=833 · K=0.08/30`.

## How to use
```kotlin
val side = DimenGameResize.fittingSquareSidePx(ctx, boxW, boxH, minDp=24f, maxDp=96f)
```
Suffixes: `a` (aspect ratio), `i` (invariant to window resize), `ia`. All values auto-adjust on resize except `i`.

## Why use it
Consistent, predictable scaling across phones → tablets → TVs with a single line of code, at O(1) cost.

## When to use it
See the decision flow in [the index](../README.md).

## Advantages & trade-offs
- ✅ Zero-allocation hot path (~2 ns fast lane), lock-free cache
- ✅ Bit-parity with dynamic/kmp family
- ⚠️ Pick by element type — see recommendation below

## Recommended usage strategy
Follow [GUIDE-FOR-BEGINNERS](../GUIDE-FOR-BEGINNERS.md): start with SCALED, switch per element type.

[Back to index](../README.md)
