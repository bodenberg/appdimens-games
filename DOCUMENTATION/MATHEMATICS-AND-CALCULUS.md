# 🧮 AppDimens Games — Mathematics & Calculus

> All kernels are validated by [`scripts/oracle.py`](../scripts/oracle.py) (30 cases, 0 failures) against the canonical family math (`appdimens-dynamic` 3.1.9 / `appdimens-kmp` 1.0.1).

## Symbols

| Symbol | Meaning | Value/Unit |
|---|---|---|
| $b$ | base value | dp |
| $d$ | selected screen dimension (after qualifier/inverter) | dp |
| $sw,\ w,\ h$ | smallest width · width · height | dp |
| $\rho$ | density | $dpi/160$ |
| $r_{AR}$ | normalized aspect ratio | $(max/min)/1.78$ |
| $L_{AR}$ | $\ln(r_{AR})$ | — |
| $K$ | AR sensitivity (default $0.08/30$) | — |

## Canonical constants

$$W_0 = 300\ \text{dp},\quad H_0 = 533\ \text{dp},\quad D_0 = 611.6305,\quad P_0 = 833$$

> **Audit note:** the family ships $D_0 = 611.6305$ while the true value is $\sqrt{300^2+533^2} = 611.6281550\ldots$. The literal is kept for **bit-parity** across Kotlin/C++/C#/Python (relative delta ≈ $3.8\times10^{-6}$).

## Engine formulas

| Strategy | Formula |
|---|---|
| SCALED | $f(b)=b\cdot\dfrac{d}{300}$; with AR: $b\left[1+(d-300)\left(\tfrac{0.10}{30}+K L_{AR}\right)\right]$ |
| PERCENT literal | $f(p)=\dfrac{p}{100}\cdot d$ |
| POWER | $f(b)=b\left(\dfrac{sw}{300}\right)^{0.75}$ |
| FLUID | $\mathrm{clamp}_{band}(b\cdot0.8,\ b\cdot1.2;\ 320,768)$ linear inside band |
| AUTO/BALANCED | $sw\le480:\ b\cdot\frac{sw}{300}$; else $b\left(\frac{480}{300}+0.4\ln(1+\frac{sw-480}{300})\right)$ |
| LOGARITHMIC | $b(1+0.4\ln\frac{sw}{300})$ if $sw>300$, else $b(1-0.4\ln\frac{300}{sw})$ |
| INTERPOLATED | $b+(b\frac{d}{300}-b)\cdot\tfrac12$ |
| DIAGONAL | $b\cdot\dfrac{\sqrt{min^2+max^2}}{611.6305}$ |
| PERIMETER | $b\cdot\dfrac{min+max}{833}$ |
| FIT / FILL | $b\cdot\min/\max\left(\dfrac{min}{300},\ \dfrac{max}{533}\right)$ |
| DENSITY | $b\cdot\rho$ |
| AR multiplier | $1 + K\ln\!\big((max/min)/1.78\big)$ |

## Snapshot pre-computation matrix

Computed **once per window snapshot** (`GameMetrics.make`), consumed as single multiplies:

| Factor | Expression | Used by |
|---|---|---|
| `scale` | $sw/300$ | sdp fast lane |
| `wFactor/hFactor` | $w/300,\ h/300$ | wdp/hdp |
| `scaledArMul` | $1+(sw-300)(\tfrac{0.10}{30}+K L_{AR})$ | sdpa |
| `powerScale` | $(sw/300)^{0.75}$ | pwsdp |
| `autoScale` | AUTO piecewise | asdp/balanced |
| `logarithmicScale` | log piecewise | logsdp |
| `interpolatedScale` | $1+(\frac{sw}{300}-1)\cdot0.5$ | isdp |
| `diagonalScale` | $\sqrt{min^2+max^2}/D_0$ | dgsdp |
| `perimeterScale` | $(min+max)/P_0$ | prsdp |
| `fitScale/fillScale` | min/max ratios | ftsdp/flsdp |

```mermaid
journey
    title Lifecycle of a dimension call
    sections
      Resize: 1: Surface/config change → GameScreen.update
      Snapshot: 3: Factors computed once (exact ln)
      Frame: 5: kernel = base × factor (≈2 ns)
```

## Resize fitting (AUTOSIZE)

Step table $[min, max]$ with spacing $step$ (ε-safe), then **binary search** for the largest candidate satisfying the fit predicate — $\mathcal{O}(\log n)$.

## Precision policy

IEEE-754 `float` on device; tests use family tolerance $\Delta \le 0.05$; oracle cross-checks in float64 with relative error $\le 10^{-6}$. Custom sensitivities are validated `isFinite` and never cached.
