package com.appdimens.games.compose.percent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGamePercentDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.PERCENT, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGamePercentSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.PERCENT, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.psdp: Dp get() = toGamePercentDp()
@get:Composable @get:ReadOnlyComposable val Number.pshdp: Dp get() = toGamePercentDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.pswdp: Dp get() = toGamePercentDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.pssp: TextUnit get() = toGamePercentSp()
@get:Composable @get:ReadOnlyComposable val Number.pshsp: TextUnit get() = toGamePercentSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.pswsp: TextUnit get() = toGamePercentSp(DpQualifier.WIDTH)
