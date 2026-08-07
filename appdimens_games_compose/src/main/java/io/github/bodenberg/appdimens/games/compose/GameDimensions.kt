package io.github.bodenberg.appdimens.games.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bodenberg.appdimens.games.core.Calculator
import io.github.bodenberg.appdimens.games.core.Screen
import io.github.bodenberg.appdimens.games.core.Strategy

@Immutable
class GameDimensions internal constructor(private val screen: Screen) {
    @Stable fun dp(value: Float, strategy: Strategy = Strategy.BALANCED): Dp =
        Calculator.scale(value, strategy, screen).dp
}

fun gameDimensions(widthDp: Float, heightDp: Float, density: Float = 1f) =
    GameDimensions(Screen(widthDp, heightDp, density))
