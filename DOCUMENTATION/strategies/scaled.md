# Scaled — `sdp/hdp/wdp`

> **Artifact:** `io.github.bodenberg:appdimens-games (core):3.0.1`

## What it is
The family baseline. Linear growth anchored on the smallest width; rotation-invariant.

## Calculation used
``f(d)=b·(d/300)`; AR-aware: `f=b·[1+(d−300)(0.10/30+K·ln((max/min)/1.78))]``

Constants: `W₀=300 · H₀=533 · diag₀=611.6305 · perim₀=833 · K=0.08/30`.

## How to use
```kotlin
val pad = 16.sdp(context)          // Kotlin/Java
val px = 16.sdpPx(context)
// Compose: 16.sdp
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
