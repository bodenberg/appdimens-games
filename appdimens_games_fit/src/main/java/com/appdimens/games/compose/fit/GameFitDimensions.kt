package com.appdimens.games.compose.fit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGameFitDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.FIT, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGameFitSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.FIT, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.ftsdp: Dp get() = toGameFitDp()
@get:Composable @get:ReadOnlyComposable val Number.ftshdp: Dp get() = toGameFitDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.ftswdp: Dp get() = toGameFitDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.ftssp: TextUnit get() = toGameFitSp()
@get:Composable @get:ReadOnlyComposable val Number.ftshsp: TextUnit get() = toGameFitSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.ftswsp: TextUnit get() = toGameFitSp(DpQualifier.WIDTH)
