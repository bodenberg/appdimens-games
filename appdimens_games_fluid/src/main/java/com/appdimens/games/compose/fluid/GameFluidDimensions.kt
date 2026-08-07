package com.appdimens.games.compose.fluid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGameFluidDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.FLUID, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGameFluidSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.FLUID, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.fsdp: Dp get() = toGameFluidDp()
@get:Composable @get:ReadOnlyComposable val Number.fshdp: Dp get() = toGameFluidDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.fswdp: Dp get() = toGameFluidDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.fssp: TextUnit get() = toGameFluidSp()
@get:Composable @get:ReadOnlyComposable val Number.fshsp: TextUnit get() = toGameFluidSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.fswsp: TextUnit get() = toGameFluidSp(DpQualifier.WIDTH)
