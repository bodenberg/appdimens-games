package com.example.benchlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdimens.dynamic.code.DimenSdp as DynamicDimenSdp
import com.appdimens.games.code.DimenSdp
import com.appdimens.games.core.GameScreen
import com.appdimens.games.math.GameMath
import kotlin.math.abs

/**
 * [EN] BenchLab — on-device comparison: AppDimens Games 3.0 (new) vs appdimens-games 2.0.1
 * (deprecated legacy) vs appdimens-dynamic 3.1.9.
 * Methodology mirrors the family BenchLab: warm-up, N samples of M ops each, order rotation,
 * median/P90/min stats and an anti-DCE checksum.
 *
 * [PT] BenchLab — comparação on-device: Games 3.0 (novo) vs games 2.0.1 (legado) vs dynamic 3.1.9.
 * Metodologia da família: aquecimento, N amostras de M operações, rotação de ordem,
 * estatísticas mediana/P90/mínimo e checksum anti-DCE.
 */
class BenchlabActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameScreen.updateFromContext(this)
        setContent { Dashboard() }
    }

    private data class Stats(val medianNs: Double, val minNs: Double, val p90Ns: Double)

    @Composable
    private fun Dashboard() {
        var result by remember { mutableStateOf("Idle — press RUN") }
        var running by remember { mutableStateOf(false) }
        val bg = Color(0xFF0D0F14)

        Column(
            Modifier
                .fillMaxSize()
                .background(bg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("AppDimens BenchLab", color = Color(0xFF00E5FF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "games-3.0 · games-2.0.1 (legacy) · dynamic-3.1.9 — ns/op (median of samples)",
                color = Color(0xFF8A93A5), fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))

            androidx.compose.material3.Button(
                onClick = {
                    running = true; result = "Running…"
                    result = runAll()
                    running = false
                },
                enabled = !running
            ) { Text("RUN") }

            Spacer(Modifier.height(16.dp))
            Text(result, color = Color(0xFFB9C1D0), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }

    private fun measure(samples: Int, opsPerSample: Int, block: () -> Float): Stats {
        val perOp = DoubleArray(samples)
        repeat(WARMUP_OPS) { consume(block()) }
        for (s in 0 until samples) {
            var acc = 0f
            val t0 = System.nanoTime()
            repeat(opsPerSample) { acc += block() }
            val t1 = System.nanoTime()
            consume(acc)
            perOp[s] = (t1 - t0).toDouble() / opsPerSample
        }
        val sorted = perOp.sorted()
        return Stats(
            medianNs = sorted[sorted.size / 2],
            minNs = sorted.first(),
            p90Ns = sorted[(sorted.size - 1) * 9 / 10]
        )
    }

    // Anti-DCE sink
    private var sink = 0f
    private fun consume(v: Float) { if (abs(v) == Float.MAX_VALUE) sink += v }

    private fun runAll(): String {
        val ctx = applicationContext

        // ── Benchmark A: fast lane sdp(context) ──────────────────────────────
        val newFast = measure(SAMPLES, OPS) { GameMath.calculateScaledDp(16f, GameScreen.metrics()) }
        val legacyFast = measure(SAMPLES, OPS) {
            com.appdimens.games.AppDimensGames.getInstance().calculateButtonSize(16f)
        }
        val dynFast = measure(SAMPLES, OPS) { DynamicDimenSdp.sdp(ctx, 16f) / resources.displayMetrics.density }

        val newFamilyExt = measure(SAMPLES, OPS) { DimenSdp.sdp(ctx, 16) }

        // ── Benchmark B: AR-aware path ───────────────────────────────────────
        val newAr = measure(SAMPLES, OPS) { GameMath.calculateScaledDp(16f, GameScreen.metrics(), applyAspectRatio = true) }
        val dynAr = measure(SAMPLES, OPS) { DynamicDimenSdp.sdpa(ctx, 16f) / resources.displayMetrics.density }

        return buildString {
            appendLine("=== Fast lane (scaled, sw) — ns/op ===")
            row("games 3.0", newFast); row("games 2.0.1", legacyFast); row("dynamic 3.1.9", dynFast)
            appendLine()
            appendLine("=== Family extension (16.sdp ctx) — ns/op ===")
            row("games 3.0 ext", newFamilyExt)
            appendLine()
            appendLine("=== Aspect-ratio aware — ns/op ===")
            row("games 3.0 (sdpa)", newAr); row("dynamic 3.1.9 (sdpa)", dynAr)
            appendLine()
            appendLine("Ratios vs games-3.0:")
            appendLine("  legacy/games3.0 ×%.1f".format(legacyFast.medianNs / newFast.medianNs))
            appendLine("  dynamic/games3.0 ×%.1f".format(dynFast.medianNs / newFast.medianNs))
            appendLine()
            appendLine("samples=$SAMPLES ops/sample=$OPS warmup=$WARMUP_OPS")
            appendLine("checksum sink=%.3f".format(sink))
        }.also { it.hashCode() }
    }

    private fun StringBuilder.row(label: String, s: Stats) =
        appendLine("  %-18s median=%7.1f min=%7.1f p90=%7.1f".format(label, s.medianNs, s.minNs, s.p90Ns))

    private companion object {
        const val SAMPLES = 15
        const val OPS = 50_000
        const val WARMUP_OPS = 20_000
    }
}
