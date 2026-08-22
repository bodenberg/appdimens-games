#!/usr/bin/env python3
"""Generates the AppDimens Games satellite strategy modules (code + compose)."""
import os, stat

ROOT = os.path.join(os.path.dirname(__file__), "..")

# name: (package-suffix, main-stem, h-stem, w-stem, kernel, extra)
STRATEGIES = {
    "percent":      ("percent",      "psdp",   "phdp",   "pwdp",   "calculatePercentDp",      True),
    "power":        ("power",        "pwsdp",  "pwhdp",  "pwwdp",  "calculatePowerDp",        False),
    "fluid":        ("fluid",        "fsdp",   "fhdp",   "fwdp",   "calculateFluidDp",        False),
    "auto":         ("auto",         "asdp",   "ahdp",   "awdp",   "calculateAutoDp",         False),
    "diagonal":     ("diagonal",     "dgsdp",  "dghdp",  "dgwdp",  "calculateDiagonalDp",     False),
    "fill":         ("fill",         "flsdp",  "flhdp",  "flwdp",  "calculateFillDp",         False),
    "fit":          ("fit",          "ftsdp",  "fthdp",  "ftwdp",  "calculateFitDp",          False),
    "interpolated": ("interpolated", "isdp",   "ihdp",   "iwdp",   "calculateInterpolatedDp", False),
    "logarithmic":  ("logarithmic",  "logsdp", "loghdp", "logwdp", "calculateLogarithmicDp",  False),
    "perimeter":    ("perimeter",    "prsdp",  "prhdp",  "prwdp",  "calculatePerimeterDp",    False),
    "density":      ("density",      "dsdp",   "dhdp",   "dwdp",   "calculateDensityDp",      False),
}

BUILD_GRADLE = """plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "com.appdimens.games.__PKG__"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":library"))
}
"""

CODE_TMPL = '''package com.appdimens.games.{pkg}

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] {title} strategy — code-side extensions (`{stem}`/`{hstem}`/`{wstem}` + `a` AR,
 * `i` resize-invariant, `ia`). Values read the live [GameScreen] snapshot and adjust
 * automatically on window resize; `i` variants stay anchored to the frozen fullscreen
 * reference.
 *
 * [PT] Estratégia {title} — extensões fora do Compose. Os valores leem o snapshot vivo
 * e se ajustam no redimensionamento; variantes `i` permanecem na referência fullscreen.
 */
private fun metrics(inv: Boolean): GameMetrics =
    if (inv) GameScreen.invariantMetrics() else GameScreen.metrics()

/** {title}: scaled dp by smallest width ({formula}). */
fun Float.{stem}(context: Context?): Float =
    GameMath.{kernel}(this, metrics(false))

/** AR-aware variant (`a`). */
fun Float.{stem}a(context: Context?): Float =
    GameMath.{kernel}(this, metrics(false), applyAspectRatio = true)

/** Resize-invariant variant (`i`). */
fun Float.{stem}i(context: Context?): Float {{
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.{kernel}(this, m)
}}

/** Invariant + AR (`ia`). */
fun Float.{stem}ia(context: Context?): Float {{
    val m = metrics(true)
    return if (!m.isFullscreen) this else GameMath.{kernel}(this, m, applyAspectRatio = true)
}}

/** Pixels. */
fun Float.{stem}Px(context: Context?): Float =
    GameMath.toPx(GameMath.{kernel}(this, metrics(false)), metrics(false))

/** Height-axis variant. */
fun Float.{hstem}(context: Context?): Float =
    GameMath.{kernel}(this, metrics(false), qualifier = DpQualifier.HEIGHT)

fun Float.{hstem}Px(context: Context?): Float =
    GameMath.toPx(GameMath.{kernel}(this, metrics(false), qualifier = DpQualifier.HEIGHT), metrics(false))

/** Width-axis variant. */
fun Float.{wstem}(context: Context?): Float =
    GameMath.{kernel}(this, metrics(false), qualifier = DpQualifier.WIDTH)

fun Float.{wstem}Px(context: Context?): Float =
    GameMath.toPx(GameMath.{kernel}(this, metrics(false), qualifier = DpQualifier.WIDTH), metrics(false))

/** Int receivers. */
fun Int.{stem}(context: Context?): Float = toFloat().{stem}(context)
fun Int.{stem}Px(context: Context?): Float = toFloat().{stem}Px(context)
'''

COMPOSE_TMPL = '''package com.appdimens.games.{pkg}.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.LocalDimenMetrics
import com.appdimens.games.math.GameMath

/**
 * [EN] {title} strategy — Compose extensions. Reactive by design: any window resize
 * recomputes via [LocalDimenMetrics]. Suffixes: `a` (aspect ratio), `i` (resize-invariant).
 *
 * [PT] Estratégia {title} — extensões Compose. Reativas por definição: qualquer
 * redimensionamento recomputa via [LocalDimenMetrics].
 */

@Composable
private fun compute(b: Float, inv: Boolean, ar: Boolean, q: DpQualifier = DpQualifier.SMALL_WIDTH): Float {{
    val m = LocalDimenMetrics.current
    if (inv && !m.isFullscreen) return b
    val v = GameMath.{kernel}(b, m, applyAspectRatio = ar, qualifier = q)
    return v
}}

/** {title} scaled dp. */
@get:Composable
val Float.{stem}: Dp get() = compute(this, false, false).dp

@get:Composable
val Float.{stem}a: Dp get() = compute(this, false, true).dp

@get:Composable
val Float.{stem}i: Dp get() = compute(this, true, false).dp

@get:Composable
val Float.{stem}px: Float get() = compute(this, false, false) * LocalDimenMetrics.current.density

@get:Composable
val Float.{hstem}: Dp get() = compute(this, false, false, DpQualifier.HEIGHT).dp

@get:Composable
val Float.{wstem}: Dp get() = compute(this, false, false, DpQualifier.WIDTH).dp

@get:Composable
val Int.{stem}: Dp get() = toFloat().{stem}
'''

FORMULAS = {
    "percent": "b·(d/300)", "power": "b·(sw/300)^0.75", "fluid": "lerp band 320–768",
    "auto": "linear ≤480 then log", "diagonal": "b·diag/611.63", "fill": "cover max-ratio",
    "fit": "letterbox min-ratio", "interpolated": "midpoint base↔linear", "logarithmic": "Weber-Fechner",
    "perimeter": "b·(min+max)/833", "density": "b·dpi/160",
}
TITLES = {k: k.capitalize() for k in STRATEGIES}

def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(content)

for name, (pkg, stem, hstem, wstem, kernel, has_space) in STRATEGIES.items():
    mod = f"{ROOT}/library-{name}"
    write(f"{mod}/build.gradle.kts", BUILD_GRADLE.replace("__PKG__", pkg))
    ctx = dict(pkg=pkg, stem=stem, hstem=hstem, wstem=wstem, kernel=kernel,
               title=TITLES[name], formula=FORMULAS[name])
    write(f"{mod}/src/main/kotlin/com/appdimens/games/{pkg}/Dimen{pkg.capitalize()}Code.kt",
          CODE_TMPL.format(**ctx))
    write(f"{mod}/src/main/kotlin/com/appdimens/games/{pkg}/compose/Dimen{pkg.capitalize()}Compose.kt",
          COMPOSE_TMPL.format(**ctx))
    print(f"[gen] library-{name}")

print("done")
