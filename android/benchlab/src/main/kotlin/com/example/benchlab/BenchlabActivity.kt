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
 * Runs entirely on a background thread and streams **per-case** results (median/min/P90)
 * to the UI as each measurement completes — the main thread is never blocked.
 *
 * [PT] BenchLab — comparação on-device: Games 3.0 (novo) vs games 2.0.1 (legado) vs
 * dynamic 3.1.9. Executa 100% em thread de fundo e publica o resultado de **cada caso**
 * (mediana/mín/P90) na UI assim que a medição termina — a main thread nunca bloqueia.
 */
class BenchlabActivity : ComponentActivity() {

    private data class Stats(val medianNs: Double, val minNs: Double, val p90Ns: Double) {
        fun row(label: String): String =
            "  %-22s median=%8.1f min=%8.1f p90=%8.1f".format(label, medianNs, minNs, p90Ns)
    }

    private data class UiState(
        val running: Boolean = false,
        val progress: String = "Idle — press RUN",
        val cases: List<String> = emptyList(),
        val summary: String = "",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameScreen.updateFromContext(this)

        // O gateway legado 2.0.1 exige initialize() explícito antes do uso.
        legacyReady = try {
            com.appdimens.games.AppDimensGames.getInstance().initialize(applicationContext)
        } catch (_: Throwable) { false }

        setContent {
            var state by remember { mutableStateOf(UiState()) }
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
                    "games-3.0 × games-2.0.1 (legacy) × dynamic-3.1.9 — ns/op por caso",
                    color = Color(0xFF8A93A5), fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))

                androidx.compose.material3.Button(
                    onClick = {
                        state = UiState(running = true, progress = "Warming up…")
                        Thread {
                            runBenchmarks(
                                onCase = { line ->
                                    runOnUiThread {
                                        state = state.copy(cases = state.cases + line)
                                    }
                                },
                                onProgress = { p -> runOnUiThread { state = state.copy(progress = p) } },
                                onDone = { s -> runOnUiThread { state = state.copy(running = false, progress = "Done", summary = s) } }
                            )
                        }.start()
                    },
                    enabled = !state.running
                ) { Text(if (state.running) "RUNNING…" else "RUN") }

                Spacer(Modifier.height(12.dp))
                Text(state.progress, color = Color(0xFFFFC400), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))

                state.cases.forEachIndexed { i, line ->
                    Text(line, color = Color(0xFFB9C1D0), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    if (i == state.cases.lastIndex && state.running) Spacer(Modifier.height(4.dp))
                }

                if (state.summary.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(state.summary, color = Color(0xFF00E5FF), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }

    private var legacyReady: Boolean = false

    private var sink = 0f
    private fun consume(v: Float) { if (abs(v) == Float.MAX_VALUE) sink += v }

    /** Measures one case; returns null instead of throwing so one bad case never kills the run. */
    private inline fun measure(samples: Int, opsPerSample: Int, block: () -> Float): Stats? {
        return try {
            repeat(WARMUP_OPS) { consume(block()) }
            val perOp = DoubleArray(samples)
            for (s in 0 until samples) {
                var acc = 0f
                val t0 = System.nanoTime()
                repeat(opsPerSample) { acc += block() }
                val t1 = System.nanoTime()
                consume(acc)
                perOp[s] = (t1 - t0).toDouble() / opsPerSample
            }
            val sorted = perOp.sorted()
            Stats(sorted[sorted.size / 2], sorted.first(), sorted[(sorted.size - 1) * 9 / 10])
        } catch (t: Throwable) {
            null
        }
    }

    private fun runBenchmarks(
        onCase: (String) -> Unit,
        onProgress: (String) -> Unit,
        onDone: (String) -> Unit,
    ) {
        val ctx = applicationContext
        val results = LinkedHashMap<String, Stats>()

        data class Case(val key: String, val label: String, val block: () -> Float)
        val cases = listOf(
            Case("g30", "games 3.0 kernel") {
                GameMath.calculateScaledDp(16f, GameScreen.metrics())
            },
            Case("legacy", "games 2.0.1 legacy") {
                com.appdimens.games.AppDimensGames.getInstance().calculateButtonSize(16f)
            },
            Case("dyn", "dynamic 3.1.9 facade") {
                DynamicDimenSdp.sdp(ctx, 16) / resources.displayMetrics.density
            },
            Case("ext", "games 3.0 ext 16.sdp") {
                DimenSdp.sdp(ctx, 16)
            },
            Case("ar", "games 3.0 AR-aware") {
                GameMath.calculateScaledDp(16f, GameScreen.metrics(), applyAspectRatio = true)
            },
            Case("dynAr", "dynamic 3.1.9 sdpa") {
                DynamicDimenSdp.sdpa(ctx, 16) / resources.displayMetrics.density
            },
        )

        cases.forEachIndexed { idx, c ->
            onProgress("Case ${idx + 1}/${cases.size}: ${c.label} …")
            val st = measure(SAMPLES, OPS, c.block)
            if (st == null) {
                onCase("✗ ${c.label.padEnd(22)} — ERRO (ignorado)")
            } else {
                results[c.key] = st
                onCase("✓ ${st.row(c.label)}")
            }
        }

        val sb = StringBuilder()
        sb.appendLine("--- Ratios vs games-3.0 ---")
        results["g30"]?.let { base ->
            results.forEach { (k, v) ->
                if (k != "g30") sb.appendLine("  ${cases.first { it.key == k }.label}: ×${"%.1f".format(v.medianNs / base.medianNs)}")
            }
        }
        sb.appendLine("samples=$SAMPLES ops=$OPS warmup=$WARMUP_OPS | checksum=%.3f".format(sink))
        onDone(sb.toString())
    }

    private companion object {
        const val SAMPLES = 9
        const val OPS = 10_000
        const val WARMUP_OPS = 3_000
    }
}
