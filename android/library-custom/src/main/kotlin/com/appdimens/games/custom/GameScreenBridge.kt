/**
 * Author & Developer: Jean Bodenberg
 * GIT: https://github.com/bodenberg/appdimens-games.git
 *
 * Library: AppDimens — Custom scaling strategies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdimens.games.custom

import com.appdimens.games.common.UiModeType
import com.appdimens.games.core.GameMetrics
import com.appdimens.games.core.GameScreen

/**
 * EN Maps the Games [UiModeType] (its own enum, not Android's raw constants) plus the
 * smallest width to the strategy [DeviceClass].
 * PT Mapeia o [UiModeType] do Games (enum próprio) e a menor largura para [DeviceClass].
 */
fun DeviceClass.Companion.fromGameUiMode(uiMode: UiModeType, smallestWidthDp: Float): DeviceClass = when (uiMode) {
    UiModeType.TELEVISION -> DeviceClass.TV
    UiModeType.CAR -> DeviceClass.CAR
    UiModeType.WATCH -> DeviceClass.WATCH
    UiModeType.DESK -> DeviceClass.DESKTOP
    UiModeType.VR_HEADSET -> DeviceClass.VR
    else -> if (smallestWidthDp >= DeviceClass.TABLET_MIN_SMALLEST_WIDTH_DP) DeviceClass.TABLET else DeviceClass.PHONE
}

/**
 * EN Converts a [GameMetrics] snapshot into the platform-free [ScreenSnapshot].
 * A non-fullscreen surface (split-screen / freeform) counts as multi-window.
 * PT Converte um [GameMetrics] no [ScreenSnapshot]. Superfície não-fullscreen = multi-janela.
 */
fun GameMetrics.toScreenSnapshot(): ScreenSnapshot = ScreenSnapshot(
    widthDp = screenWidthDp.coerceAtLeast(0).toFloat(),
    heightDp = screenHeightDp.coerceAtLeast(0).toFloat(),
    smallestWidthDp = smallestWidthDpF,
    density = density,
    fontScale = fontScale,
    isLandscape = screenWidthDp > screenHeightDp,
    isMultiWindow = !isFullscreen,
    device = DeviceClass.fromGameUiMode(uiMode, smallestWidthDpF),
)

/**
 * EN Resolves for a Games snapshot. Memoized per [GameMetrics] (identity, then equality):
 * the hot path is one memo check + one multiply, allocation-free on hit.
 * PT Resolve para um snapshot do Games. Memoizado por [GameMetrics]: no hot path é uma
 * checagem de memo + uma multiplicação, sem alocação.
 */
fun ScalingStrategy.resolve(metrics: GameMetrics): ResolvedScaling = resolve(metrics) { metrics.toScreenSnapshot() }

/** EN Resolution for the live [GameScreen] snapshot. PT Resolução para o snapshot vivo. */
fun ScalingStrategy.resolveLive(): ResolvedScaling = resolve(GameScreen.metrics())

/**
 * EN Resolution for the frozen fullscreen reference used by `i` variants
 * (`null` under true multi-window, where `i` variants return the base value).
 * PT Resolução para a referência fullscreen congelada (variantes `i`).
 */
fun ScalingStrategy.resolveInvariant(): ResolvedScaling? {
    val m = GameScreen.invariantMetrics()
    return if (m.isFullscreen) resolve(m) else null
}
