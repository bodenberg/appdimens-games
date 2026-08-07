package com.appdimens.games.compose.density

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGameDensityDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.DENSITY, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGameDensitySp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.DENSITY, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.dsdp: Dp get() = toGameDensityDp()
@get:Composable @get:ReadOnlyComposable val Number.dshdp: Dp get() = toGameDensityDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.dswdp: Dp get() = toGameDensityDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.dssp: TextUnit get() = toGameDensitySp()
@get:Composable @get:ReadOnlyComposable val Number.dshsp: TextUnit get() = toGameDensitySp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.dswsp: TextUnit get() = toGameDensitySp(DpQualifier.WIDTH)
