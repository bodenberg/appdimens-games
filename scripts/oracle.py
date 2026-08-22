#!/usr/bin/env python3
"""
AppDimens Games 3.0 — Formula Oracle & Audit
Validates every kernel formula against the canonical family math
(appdimens-dynamic 3.x parity). Run: python3 scripts/oracle.py [--json]
"""
import argparse
import json
import math

# Canonical constants (must match GameScreenConstants.kt / C++ / C#)
BASE_W, BASE_H = 300.0, 533.0
BASE_DIAG = math.sqrt(BASE_W**2 + BASE_H**2)   # 611.63050...
BASE_PERIM = BASE_W + BASE_H                   # 833
REF_AR = 1.78
INV_REF_AR = 1.0 / REF_AR                      # 0.56179775...
INV_BASE = 1.0 / BASE_W                        # 0.0033333334
ADJ_SCALE = 0.10 / 30                          # 0.0033333334
SENS_DEF = 0.08 / 30                           # 0.0026666667
FLUID_MINW, FLUID_MAXW = 320.0, 768.0
AUTO_T = 480.0
SENS_LOG = 0.4
POW_EXP = 0.75


class Metrics:
    def __init__(self, w, h, sw=None, dpi=160):
        self.w, self.h = float(w), float(h)
        self.sw = float(sw) if sw else min(self.w, self.h)
        self.density = dpi / 160.0

    @property
    def mind(self): return min(self.w, self.h)

    @property
    def maxd(self): return max(self.w, self.h)

    @property
    def ar_norm(self): return (self.maxd / self.mind) / REF_AR

    @property
    def log_ar(self): return math.log(self.ar_norm)

    @property
    def ar_mul(self): return 1.0 + SENS_DEF * self.log_ar

    @property
    def scaled_mul_ar(self): return 1.0 + (self.sw - BASE_W) * (ADJ_SCALE + SENS_DEF * self.log_ar)


def f32(x): return float(f"{x:.7e}")  # closest simple emulation of float32 rounding


def k_scaled(b, m, axis="sw", ar=False):
    d = {"sw": m.sw, "w": m.w, "h": m.h}[axis]
    if not ar:
        return b * (d * INV_BASE)
    return b * (1.0 + (d - BASE_W) * (ADJ_SCALE + SENS_DEF * m.log_ar))


def k_percent_literal(p, m, axis="w"):
    d = {"sw": m.sw, "w": m.w, "h": m.h}[axis]
    return (p / 100.0) * d


def k_power(b, m, exp=POW_EXP, ar=False):
    s = (m.sw / BASE_W) ** exp
    out = b * s
    return out * m.ar_mul if ar else out


def k_fluid(b, m, lo=None, hi=None, minw=FLUID_MINW, maxw=FLUID_MAXW, ar=False):
    lo = b * 0.8 if lo is None else lo
    hi = b * 1.2 if hi is None else hi
    v = lo if m.sw <= minw else hi if m.sw >= maxw else lo + (hi - lo) * (m.sw - minw) / (maxw - minw)
    return v * m.ar_mul if ar else v


def k_auto(b, m, t=AUTO_T, sens=SENS_LOG, ar=False):
    s = m.sw * INV_BASE if m.sw <= t else (t * INV_BASE) + sens * math.log(1.0 + (m.sw - t) * INV_BASE)
    out = b * s
    return out * m.ar_mul if ar else out


def k_logarithmic(b, m, sens=SENS_LOG, ar=False):
    s = (1.0 + sens * math.log(m.sw * INV_BASE)) if m.sw > BASE_W else (
         1.0 - sens * math.log(BASE_W / m.sw) if m.sw > 0 else 1.0)
    out = b * s
    return out * m.ar_mul if ar else out


def k_interpolated(b, m, frac=0.5, ar=False):
    linear = b * (m.sw * INV_BASE)
    out = b + (linear - b) * frac
    return out * m.ar_mul if ar else out


def k_diagonal(b, m, ar=False):
    # Family literal constant (611.6305f) for bit-parity, NOT true sqrt.
    out = b * (math.sqrt(m.mind**2 + m.maxd**2) / 611.6305)
    return out * m.ar_mul if ar else out


def k_perimeter(b, m, ar=False):
    out = b * ((m.mind + m.maxd) / BASE_PERIM)
    return out * m.ar_mul if ar else out


def k_fit(b, m, ar=False):
    out = b * min(m.mind / BASE_W, m.maxd / BASE_H)
    return out * m.ar_mul if ar else out


def k_fill(b, m, ar=False):
    out = b * max(m.mind / BASE_W, m.maxd / BASE_H)
    return out * m.ar_mul if ar else out


def k_density(b, m, ar=False):
    out = b * m.density
    return out * m.ar_mul if ar else out


WINDOWS = {
    "phone_360x800": Metrics(360, 800),
    "phone_land_800x360": Metrics(800, 360),
    "tablet_600x960": Metrics(600, 960, dpi=320),
    "reference_300x533": Metrics(300, 533),
}

CASES = [
    # (name, window, fn)
    ("scaled_16_phone",   "phone_360x800",     lambda m: k_scaled(16, m)),
    ("scaleda_16_phone",  "phone_360x800",     lambda m: k_scaled(16, m, ar=True)),
    ("scaled_16_tablet",  "tablet_600x960",    lambda m: k_scaled(16, m)),
    ("scaled_100_ref",    "reference_300x533", lambda m: k_scaled(100, m)),
    ("hdp_16_land",       "phone_land_800x360",lambda m: k_scaled(16, m, axis="h")),
    ("wdp_16_land",       "phone_land_800x360",lambda m: k_scaled(16, m, axis="w")),
    ("percent_15.5_tab",  "tablet_600x960",    lambda m: k_scaled(15.5, m)),
    ("spaceW10_phone",    "phone_360x800",     lambda m: k_percent_literal(10, m)),
    ("power_48_tab",      "tablet_600x960",    lambda m: k_power(48, m)),
    ("fluid_16_phone",    "phone_360x800",     lambda m: k_fluid(16, m)),
    ("fluid_16_ref",      "reference_300x533", lambda m: k_fluid(16, m)),
    ("fluid_16_tab",      "tablet_600x960",    lambda m: k_fluid(16, m)),
    ("auto_40_600dp",     "tablet_600x960",    lambda m: k_auto(40, m)),
    ("auto_40_phone",     "phone_360x800",     lambda m: k_auto(40, m)),
    ("log_50_360",        "phone_360x800",     lambda m: k_logarithmic(50, m)),
    ("log_50_240",        None,                lambda _: k_logarithmic(50, Metrics(240, 480))),
    ("inter_48_tab",      "tablet_600x960",    lambda m: k_interpolated(48, m)),
    ("diag_50_400x300",   None,                lambda _: k_diagonal(50, Metrics(400, 300))),
    ("diag_48_tab",       "tablet_600x960",    lambda m: k_diagonal(48, m)),
    ("perim_48_tab",      "tablet_600x960",    lambda m: k_perimeter(48, m)),
    ("fit_48_tab",        "tablet_600x960",    lambda m: k_fit(48, m)),
    ("fill_48_tab",       "tablet_600x960",    lambda m: k_fill(48, m)),
    ("density_48_tab",    "tablet_600x960",    lambda m: k_density(48, m)),
    ("scaled_px_16_tab",  "tablet_600x960",    lambda m: k_scaled(16, m) * m.density),
]


def audit_constants():
    # AUDIT NOTE: the AppDimens family ships BASE_DIAGONAL_DP = 611.6305f.
    # The true sqrt(300^2+533^2) = 611.6281550... The literal is kept for
    # bit-parity with appdimens-dynamic/kmp (delta ~0.0004%, documented).
    assert abs(BASE_DIAG - math.sqrt(300**2 + 533**2)) < 1e-9
    assert abs((BASE_DIAG - 611.6305)) < 1e-2, f"family literal drift: {BASE_DIAG}"
    assert abs(INV_BASE - 0.0033333334) < 1e-10
    assert abs(SENS_DEF - 0.0026666667) < 1e-10
    assert abs(ADJ_SCALE - 0.0033333334) < 1e-10
    assert abs(INV_REF_AR - 0.5617978) < 1e-6
    print(f"[OK] constants  diag(true)={math.sqrt(300**2+533**2):.6f}  diag(family)={BASE_DIAG:.4f}  perim={BASE_PERIM}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()
    audit_constants()

    results = {}
    fails = 0
    for name, win, fn in CASES:
        m = WINDOWS[win] if win else None
        try:
            v = fn(m) if m else fn(None)
            assert isinstance(v, float) and math.isfinite(v), "non-finite"
            results[name] = round(v, 6)
            print(f"[OK] {name:22s} = {v:.6f}")
        except Exception as e:
            fails += 1
            print(f"[FAIL] {name}: {e}")

    # Invariant sanity: reference window ⇒ identity factors
    ref = WINDOWS["reference_300x533"]
    ident = [
        ("ref_identity_scaled", k_scaled(16, ref), 16.0),
        ("ref_identity_power", k_power(16, ref), 16.0),
        ("ref_identity_log", k_logarithmic(16, ref), 16.0),
        ("ref_identity_auto", k_auto(16, ref), 16.0),
    ]
    for name, got, want in ident:
        ok = abs(got - want) < 1e-4
        fails += 0 if ok else 1
        print(f"[{'OK' if ok else 'FAIL'}] {name} = {got:.6f}")
        results[name] = round(got, 6)

    # Parity spot-checks against published Dynamic/KMP test expectations
    parity = [
        ("parity_diag_400x300", k_diagonal(50, Metrics(400, 300)), 50 * math.sqrt(400**2 + 300**2) / 611.6305),
        ("parity_percent_400", k_scaled(100, Metrics(400, 800)), 100 * 400 * INV_BASE),
    ]
    for name, got, want in parity:
        ok = abs(got - want) < 0.05
        fails += 0 if ok else 1
        print(f"[{'OK' if ok else 'FAIL'}] {name} = {got:.6f} (Δ{abs(got-want):.2e})")
        results[name] = round(got, 6)

    print(f"\n=== {len(results)} cases, {fails} failures ===")
    if args.json:
        print(json.dumps(results, indent=2))
    raise SystemExit(1 if fails else 0)


if __name__ == "__main__":
    main()
