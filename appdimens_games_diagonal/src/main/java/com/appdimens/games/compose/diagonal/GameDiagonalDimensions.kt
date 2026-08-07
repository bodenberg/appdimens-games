package com.appdimens.games.compose.diagonal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGameDiagonalDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.DIAGONAL, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGameDiagonalSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.DIAGONAL, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.dgsdp: Dp get() = toGameDiagonalDp()
@get:Composable @get:ReadOnlyComposable val Number.dgshdp: Dp get() = toGameDiagonalDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.dgswdp: Dp get() = toGameDiagonalDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.dgssp: TextUnit get() = toGameDiagonalSp()
@get:Composable @get:ReadOnlyComposable val Number.dgshsp: TextUnit get() = toGameDiagonalSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.dgswsp: TextUnit get() = toGameDiagonalSp(DpQualifier.WIDTH)
