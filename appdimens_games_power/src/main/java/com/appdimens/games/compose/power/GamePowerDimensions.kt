package com.appdimens.games.compose.power

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appdimens.games.common.DpQualifier
import com.appdimens.games.compose.toGameDp
import com.appdimens.games.compose.toGameSp
import com.appdimens.games.core.GameStrategy

@Composable @ReadOnlyComposable
fun Number.toGamePowerDp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): Dp = toGameDp(GameStrategy.POWER, qualifier)
@Composable @ReadOnlyComposable
fun Number.toGamePowerSp(qualifier: DpQualifier = DpQualifier.SMALL_WIDTH): TextUnit = toGameSp(GameStrategy.POWER, qualifier)
@get:Composable @get:ReadOnlyComposable val Number.pwsdp: Dp get() = toGamePowerDp()
@get:Composable @get:ReadOnlyComposable val Number.pwshdp: Dp get() = toGamePowerDp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.pwswdp: Dp get() = toGamePowerDp(DpQualifier.WIDTH)
@get:Composable @get:ReadOnlyComposable val Number.pwssp: TextUnit get() = toGamePowerSp()
@get:Composable @get:ReadOnlyComposable val Number.pwshsp: TextUnit get() = toGamePowerSp(DpQualifier.HEIGHT)
@get:Composable @get:ReadOnlyComposable val Number.pwswsp: TextUnit get() = toGamePowerSp(DpQualifier.WIDTH)
