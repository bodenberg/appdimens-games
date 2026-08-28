# Logarithmic (Weber–Fechner) — `logsdp…`

> **Artifact:** `io.github.bodenberg:appdimens-games-logarithmic:3.0.1`

## What it is
Maximum control on TV/large screens; subtle UI elements.

## Calculation used
``sw>300: b·(1+0.4·ln(sw/300))`; else `b·(1−0.4·ln(300/sw))``

Constants: `W₀=300 · H₀=533 · diag₀=611.6305 · perim₀=833 · K=0.08/30`.

## How to use
```kotlin
val tvUi = 50f.logsdp(context)
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
