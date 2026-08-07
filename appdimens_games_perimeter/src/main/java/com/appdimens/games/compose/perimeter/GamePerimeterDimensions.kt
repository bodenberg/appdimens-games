package com.appdimens.games.compose.perimeter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGamePerimeterDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.PERIMETER, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGamePerimeterSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.PERIMETER, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.prsdp: Dp get() = toGamePerimeterDp()
@get:Composable @get:ReadOnlyComposable val Number.prshdp: Dp get() = toGamePerimeterDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.prswdp: Dp get() = toGamePerimeterDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.prssp: TextUnit get() = toGamePerimeterSp()
@get:Composable @get:ReadOnlyComposable val Number.prshsp: TextUnit get() = toGamePerimeterSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.prswsp: TextUnit get() = toGamePerimeterSp(DpQualifier.WIDTH)
