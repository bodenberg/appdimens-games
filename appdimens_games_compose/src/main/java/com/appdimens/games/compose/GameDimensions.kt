package com.appdimens.games.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.core.GameDimens
import com.appdimens.games.core.GameScreen
import com.appdimens.games.core.GameStrategy

/** Common calculation plumbing; strategy modules expose the familiar Dynamic-style names. */
@Composable @ReadOnlyComposable
fun Number.toGameDp(strategy: GameStrategy, qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    return GameDimens.calculate(toFloat(), strategy, qualifier,
        GameScreen(configuration.screenWidthDp.toFloat(), configuration.screenHeightDp.toFloat(), density)).dp
}

@Composable @ReadOnlyComposable
fun Number.toGameSp(strategy: GameStrategy, qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit =
    toGameDp(strategy, qualifier).value.sp

@get:Composable @get:ReadOnlyComposable val Number.sdp: Dp get() = toGameDp(GameStrategy.SCALED)
@get:Composable @get:ReadOnlyComposable val Number.hdp: Dp get() = toGameDp(GameStrategy.SCALED, DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.wdp: Dp get() = toGameDp(GameStrategy.SCALED, DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.ssp: TextUnit get() = toGameSp(GameStrategy.SCALED)
@get:Composable @get:ReadOnlyComposable val Number.hsp: TextUnit get() = toGameSp(GameStrategy.SCALED, DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.wsp: TextUnit get() = toGameSp(GameStrategy.SCALED, DpQualifier.WIDTH)
