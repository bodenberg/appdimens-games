package com.appdimens.games.units

import android.content.Context
import com.appdimens.games.core.GameScreen

/**
 * [EN] Physical-unit extensions for game UI (touch targets, bezels, accessibility).
 * Family-parity with `appdimens-dynamic` / `-kmp` units modules.
 *
 * [PT] Extensões de unidades físicas para UI de jogo (alvos de toque, molduras,
 * acessibilidade). Paridade com os módulos units da família.
 *
 * ```kotlin
 * val touchTarget = 2f.cmPx(context)   // 2 cm in px — physical consistency
 * ```
 */

/** Millimeters → dp. */
fun Float.mmDp(context: Context?): Float =
    PhysicalUnits.mmToDp(this, GameScreen.metrics())

/** Millimeters → px. */
fun Float.mmPx(context: Context?): Float =
    PhysicalUnits.mmToPx(this, GameScreen.metrics())

/** Centimeters → dp. */
fun Float.cmDp(context: Context?): Float =
    PhysicalUnits.cmToDp(this, GameScreen.metrics())

/** Centimeters → px. */
fun Float.cmPx(context: Context?): Float =
    PhysicalUnits.cmToPx(this, GameScreen.metrics())

/** Inches → dp. */
fun Float.inchDp(context: Context?): Float =
    PhysicalUnits.inchToDp(this, GameScreen.metrics())

/** Inches → px. */
fun Float.inchPx(context: Context?): Float =
    PhysicalUnits.inchToPx(this, GameScreen.metrics())

/** Int receivers. */
fun Int.cmPx(context: Context?): Float = toFloat().cmPx(context)
fun Int.mmPx(context: Context?): Float = toFloat().mmPx(context)
