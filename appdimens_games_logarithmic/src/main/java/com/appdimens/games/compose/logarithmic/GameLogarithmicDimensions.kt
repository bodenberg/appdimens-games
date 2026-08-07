package com.appdimens.games.compose.logarithmic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGameLogarithmicDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.LOGARITHMIC, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGameLogarithmicSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.LOGARITHMIC, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.logsdp: Dp get() = toGameLogarithmicDp()
@get:Composable @get:ReadOnlyComposable val Number.logshdp: Dp get() = toGameLogarithmicDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.logswdp: Dp get() = toGameLogarithmicDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.logssp: TextUnit get() = toGameLogarithmicSp()
@get:Composable @get:ReadOnlyComposable val Number.logshsp: TextUnit get() = toGameLogarithmicSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.logswsp: TextUnit get() = toGameLogarithmicSp(DpQualifier.WIDTH)
