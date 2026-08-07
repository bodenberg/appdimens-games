package com.appdimens.games.compose.resize

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGameResizeDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.RESIZE, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGameResizeSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.RESIZE, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.rsdp: Dp get() = toGameResizeDp()
@get:Composable @get:ReadOnlyComposable val Number.rshdp: Dp get() = toGameResizeDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.rswdp: Dp get() = toGameResizeDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.rssp: TextUnit get() = toGameResizeSp()
@get:Composable @get:ReadOnlyComposable val Number.rshsp: TextUnit get() = toGameResizeSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.rswsp: TextUnit get() = toGameResizeSp(DpQualifier.WIDTH)
