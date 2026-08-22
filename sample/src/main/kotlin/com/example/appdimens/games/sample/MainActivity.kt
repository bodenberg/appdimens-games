package com.example.appdimens.games.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdimens.games.code.DimenSdp
import com.appdimens.games.code.sdpa
import com.appdimens.games.core.GameScreen

/**
 * [EN] Sample launcher — demonstrates the three game backends.
 * [PT] Launcher de exemplo — demonstra os três backends de jogo.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameScreen.updateFromContext(this)

        setContent {
            val bg = Color(0xFF0D0F14)
            Column(
                Modifier
                    .fillMaxSize()
                    .background(bg)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Title("AppDimens Games 3.0")
                Subtitle("Unified scaling for Android games — Compose · OpenGL ES · Vulkan")

                Spacer(Modifier.height(32.dp))
                DemoButton("🎮 Compose Game (2D canvas + HUD)") {
                    startActivity(Intent(this@MainActivity, ComposeGameActivity::class.java))
                }
                DemoButton("🖼️ OpenGL ES Game (native loop)") {
                    startActivity(Intent(this@MainActivity, GlGameActivity::class.java))
                }
                DemoButton("🔥 Vulkan Surface (swapchain resize)") {
                    startActivity(Intent(this@MainActivity, VulkanGameActivity::class.java))
                }

                Spacer(Modifier.height(32.dp))
                val m = GameScreen.metrics()
                InfoCard(
                    "Live snapshot",
                    listOf(
                        "window: ${m.screenWidthDp}×${m.screenHeightDp} dp",
                        "sw: ${m.smallestWidthDp} dp",
                        "density: ${m.density}",
                        "fullscreen: ${m.isFullscreen}",
                        "player sdpa(px): ${64f.sdpa(null)}",
                        "hud sdp(px): ${DimenSdp.sdp(null, 48)}"
                    )
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "Rotate the device or enter split-screen to watch every dimension auto-adjust. " +
                        "Values with suffix `i` stay anchored to the fullscreen reference.",
                    color = Color(0xFF8A93A5), fontSize = 13.sp
                )
            }
        }
    }
}

@Composable private fun Title(t: String) = Text(
    t, color = Color(0xFF00E5FF), fontSize = 26.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(top = 40.dp)
)

@Composable private fun Subtitle(t: String) = Text(
    t, color = Color(0xFF8A93A5), fontSize = 14.sp,
    modifier = Modifier.padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center
)

@Composable
private fun DemoButton(label: String, onClick: () -> Unit) = Box(
    Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
        .background(Color(0xFF171B26))
        .clickable(onClick = onClick)
        .padding(vertical = 18.dp),
    contentAlignment = Alignment.Center
) { Text(label, color = Color.White, fontSize = 16.sp) }

@Composable
private fun InfoCard(title: String, lines: List<String>) = Column(
    Modifier
        .fillMaxWidth()
        .background(Color(0xFF10131B))
        .padding(16.dp)
) {
    Text(title, color = Color(0xFFFFC400), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    lines.forEach { Text(it, color = Color(0xFFB9C1D0), fontSize = 13.sp) }
}
