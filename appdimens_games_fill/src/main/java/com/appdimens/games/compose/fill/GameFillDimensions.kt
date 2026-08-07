package com.appdimens.games.compose.fill

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGameFillDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.FILL, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGameFillSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.FILL, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.flsdp: Dp get() = toGameFillDp()
@get:Composable @get:ReadOnlyComposable val Number.flshdp: Dp get() = toGameFillDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.flswdp: Dp get() = toGameFillDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.flssp: TextUnit get() = toGameFillSp()
@get:Composable @get:ReadOnlyComposable val Number.flshsp: TextUnit get() = toGameFillSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.flswsp: TextUnit get() = toGameFillSp(DpQualifier.WIDTH)
