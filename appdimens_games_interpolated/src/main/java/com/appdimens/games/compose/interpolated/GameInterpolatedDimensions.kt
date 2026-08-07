package com.appdimens.games.compose.interpolated

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGameInterpolatedDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.INTERPOLATED, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGameInterpolatedSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.INTERPOLATED, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.isdp: Dp get() = toGameInterpolatedDp()
@get:Composable @get:ReadOnlyComposable val Number.ishdp: Dp get() = toGameInterpolatedDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.iswdp: Dp get() = toGameInterpolatedDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.issp: TextUnit get() = toGameInterpolatedSp()
@get:Composable @get:ReadOnlyComposable val Number.ishsp: TextUnit get() = toGameInterpolatedSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.iswsp: TextUnit get() = toGameInterpolatedSp(DpQualifier.WIDTH)
