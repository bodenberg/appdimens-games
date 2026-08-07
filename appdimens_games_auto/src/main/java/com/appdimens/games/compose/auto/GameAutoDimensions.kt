package com.appdimens.games.compose.auto

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGameAutoDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.AUTO, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGameAutoSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.AUTO, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.asdp: Dp get() = toGameAutoDp()
@get:Composable @get:ReadOnlyComposable val Number.ashdp: Dp get() = toGameAutoDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.aswdp: Dp get() = toGameAutoDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.assp: TextUnit get() = toGameAutoSp()
@get:Composable @get:ReadOnlyComposable val Number.ashsp: TextUnit get() = toGameAutoSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.aswsp: TextUnit get() = toGameAutoSp(DpQualifier.WIDTH)
