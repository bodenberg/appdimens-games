package com.appdimens.games.code

import android.content.Context
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.common.Orientation
import com.appdimens.games.common.UiModeType
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath

/**
 * [EN] Facilitators — family parity with `sdpRotate / sdpMode / sdpQualifier /
 * sdpScreen`. Zero-allocation; single resolution per call.
 *
 * [PT] Facilitadores — paridade com `sdpRotate / sdpMode / sdpQualifier /
 * sdpScreen`. Sem alocação; resolução única por chamada.
 */

/**
 * [EN] Uses [rotationValue] when the device matches [targetOrientation], otherwise
 * scales the receiver normally.
 * [PT] Usa [rotationValue] quando o dispositivo está em [targetOrientation];
 * caso contrário escala o receptor normalmente.
 */
fun Number.sdpRotate(
    context: Context?,
    rotationValue: Number,
    targetOrientation: Orientation,
    qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
    ignoreMultiWindows: Boolean = false,
): Float {
    val m = resolveMetrics(ignoreMultiWindows)
    val current = when {
        m.screenWidthDp > m.screenHeightDp -> Orientation.LANDSCAPE
        m.screenHeightDp > m.screenWidthDp -> Orientation.PORTRAIT
        else -> Orientation.DEFAULT
    }
    val base = if (targetOrientation == current) rotationValue.toFloat() else toFloat()
    if (ignoreMultiWindows && isConstrained(m)) return base * m.density
    return GameMath.toPx(GameMath.calculateScaledDp(base, m, qualifier), m)
}

/**
 * [EN] Uses [modeValue] on devices in [uiModeType] (TV, watch, car…).
 * [PT] Usa [modeValue] em dispositivos do [uiModeType] (TV, watch, carro…).
 */
fun Number.sdpMode(
    context: Context?,
    modeValue: Number,
    uiModeType: UiModeType,
    qualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
    ignoreMultiWindows: Boolean = false,
): Float {
    val m = resolveMetrics(ignoreMultiWindows)
    val base = if (m.uiMode == uiModeType) modeValue.toFloat() else toFloat()
    if (ignoreMultiWindows && isConstrained(m)) return base * m.density
    return GameMath.toPx(GameMath.calculateScaledDp(base, m, qualifier), m)
}

/**
 * [EN] Uses [qualifiedValue] when the axis meets [qualifierThreshold] dp.
 * [PT] Usa [qualifiedValue] quando o eixo atinge [qualifierThreshold] dp.
 */
fun Number.sdpQualifier(
    context: Context?,
    qualifiedValue: Number,
    qualifierType: DpQualifier,
    qualifierThreshold: Int,
    scaleQualifier: DpQualifier = DpQualifier.SMALL_WIDTH,
    ignoreMultiWindows: Boolean = false,
): Float {
    val m = resolveMetrics(ignoreMultiWindows)
    val meets = m.axisDp(qualifierType) >= qualifierThreshold
    val base = if (meets) qualifiedValue.toFloat() else toFloat()
    if (ignoreMultiWindows && isConstrained(m)) return base * m.density
    return GameMath.toPx(GameMath.calculateScaledDp(base, m, scaleQualifier), m)
}

/**
 * [EN] Combined mode + qualifier facilitator (`sdpScreen`).
 * [PT] Facilitador combinado de modo + qualificador (`sdpScreen`).
 */
fun Number.sdpScreen(
    context: Context?,
    screenValue: Number,
    uiModeType: UiModeType,
    qualifierType: DpQualifier,
    qualifierThreshold: Int,
    ignoreMultiWindows: Boolean = false,
): Float {
    val m = resolveMetrics(ignoreMultiWindows)
    val byMode = m.uiMode == uiModeType
    val bySize = m.axisDp(qualifierType) >= qualifierThreshold
    val base = if (byMode || bySize) screenValue.toFloat() else toFloat()
    if (ignoreMultiWindows && isConstrained(m)) return base * m.density
    return GameMath.toPx(GameMath.calculateScaledDp(base, m, DpQualifier.SMALL_WIDTH), m)
}
