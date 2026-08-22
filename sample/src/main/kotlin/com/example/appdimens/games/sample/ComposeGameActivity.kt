package com.example.appdimens.games.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdimens.games.auto.compose.asdp
import com.appdimens.games.compose.AppDimensProvider
import com.appdimens.games.compose.currentDimenMetrics
import com.appdimens.games.compose.sdpi
import com.appdimens.games.compose.sdp
import com.appdimens.games.core.GameScreen
import kotlinx.coroutines.delay

/**
 * [EN] Compose game demo: 2D canvas mini-game. Sprites use the BALANCED kernel
 * (auto-adjust on resize); the HUD uses the invariant path (`i`) so it stays
 * anchored to the frozen fullscreen reference under split-screen.
 *
 * [PT] Demo de jogo em Compose: canvas 2D. Sprites usam o kernel BALANCED
 * (ajuste automático no resize); o HUD usa o caminho invariante (`i`),
 * permanecendo ancorado na referência fullscreen sob split-screen.
 */
class ComposeGameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameScreen.updateFromContext(this)
        setContent {
            AppDimensProvider { GameScene() }
        }
    }
}

private data class Sprite(val fx: Float, val fy: Float, val hue: Float)

@Composable
private fun GameScene() {
    val metrics = currentDimenMetrics()
    var t by remember { mutableStateOf(0f) }

    // Game loop tick (~60 FPS). Metrics come from GameScreen — updated by the provider.
    LaunchedEffect(Unit) {
        val start = System.nanoTime()
        while (true) {
            t = (System.nanoTime() - start) / 1_000_000_000f
            delay(16)
        }
    }

    // Family-standard sizes (recomputed automatically when the window changes).
    val playerDp = 48f.asdp      // BALANCED strategy (library-auto)
    val enemyDp = 32f.asdp
    val hudPadDp = 12.sdp        // scaled — follows the window
    val hudIconInv = 20.sdpi     // invariant (`i`) HUD element

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C12))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val playerPx = playerDp.value * metrics.density
            val enemyPx = enemyDp.value * metrics.density

            // Player (center)
            drawCircle(Color(0xFF00E5FF), radius = playerPx / 2f, center = Offset(w / 2f, h / 2f))

            // Enemies orbiting
            repeat(8) { i ->
                val a = t * 0.9f + i * (2f * Math.PI.toFloat() / 8)
                val cx = w / 2f + kotlin.math.cos(a) * w * 0.33f
                val cy = h / 2f + kotlin.math.sin(a) * h * 0.28f
                drawCircle(
                    Color.hsv(hue = i * 40f, saturation = 0.7f, value = 0.95f),
                    radius = enemyPx / 2f,
                    center = Offset(cx, cy)
                )
            }
        }

        // HUD — top bar (invariant sizes: `i` semantics)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = hudPadDp, vertical = hudPadDp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudChip("SCORE ${((t * 100).toInt())}", hudIcon)
            HudChip("HP ▮▮▮▮▮", hudIcon)
            HudChip("×${"%.2f".format(metrics.scale)}", hudIcon)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${metrics.screenWidthDp}×${metrics.screenHeightDp} dp · sw=${metrics.smallestWidthDp} · " +
                    "fullscreen=${metrics.isFullscreen}",
                color = Color(0xFF8A93A5), fontSize = 12.sp
            )
            Text(
                "Rotate or split-screen: sprites auto-adjust; HUD stays (suffix i)",
                color = Color(0xFF59627A), fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun HudChip(label: String, iconBaseDp: Float) {
    val m = GameScreen.metrics()
    Text(
        label,
        color = Color.White,
        fontSize = with(m) { iconBaseDp * scale }.sp.coerceAtLeast(10.sp),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(Color(0x80101420))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
