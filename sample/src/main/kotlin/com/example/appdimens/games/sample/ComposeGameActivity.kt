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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdimens.games.auto.compose.asdp
import com.appdimens.games.compose.AppDimensProvider
import com.appdimens.games.compose.currentDimenMetrics
import com.appdimens.games.compose.sdpi
import com.appdimens.games.compose.sdp
import com.appdimens.games.core.GameScreen
import com.appdimens.games.compose.custom.cdp
import com.appdimens.games.compose.custom.csp
import com.appdimens.games.custom.CustomScaleStrategy
import com.appdimens.games.custom.scalingFormula
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

// Custom scaling (library-custom): declared once at top level, never inside the frame loop.
// Boss: factor from the window width, kept between 0.85× and 1.8×.
private val BossScale = CustomScaleStrategy { (width / 360f).coerceIn(0.85f, 1.8f) }
// Banner text: the formula is the final value (smaller growth on tablets/TVs).
private val BannerText = scalingFormula { base * pow(sw / 360f, if (tablet || tv) 0.5f else 0.8f) }

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
    val bossDp = 40.cdp(BossScale)        // custom strategy (library-custom)
    val bannerSp = 13.csp(BannerText)     // custom text formula

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
            val bossPx = bossDp.value * metrics.density

            // Player (center)
            drawCircle(Color(0xFF00E5FF), radius = playerPx / 2f, center = Offset(w / 2f, h / 2f))

            // Boss (custom strategy) bobbing near the top
            drawCircle(
                Color(0xFFFF7043), radius = bossPx / 2f,
                center = Offset(w / 2f, h * 0.22f + kotlin.math.sin(t * 1.4f) * h * 0.03f)
            )

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
            HudChip("SCORE ${((t * 100).toInt())}", hudIconInv)
            HudChip("HP ▮▮▮▮▮", hudIconInv)
            HudChip("×${"%.2f".format(metrics.scale)}", hudIconInv)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${metrics.screenWidthDp}×${metrics.screenHeightDp} dp · sw=${metrics.smallestScreenWidthDp} · " +
                    "fullscreen=${metrics.isFullscreen}",
                color = Color(0xFF8A93A5), fontSize = 12.sp
            )
            Text(
                "Rotate or split-screen: sprites auto-adjust; HUD stays (suffix i)",
                color = Color(0xFF59627A), fontSize = 11.sp
            )
            Text(
                "Boss: 40.cdp(BossScale) = ${"%.1f".format(bossDp.value)} dp · custom formula text",
                color = Color(0xFFFFAB91), fontSize = bannerSp
            )
        }
    }
}

@Composable
private fun HudChip(label: String, iconBaseDp: Dp) {
    val m = GameScreen.metrics()
    Text(
        label,
        color = Color.White,
        // Fonte proporcional ao ícone, limitada para o HUD caber sempre na tela.
        fontSize = (iconBaseDp.value * 0.55f).coerceIn(11f, 15f).sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(Color(0x80101420))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
