package com.appdimens.games.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * [EN] Lock-free, allocation-free-on-hit dimension cache.
 * Keys are packed into a single 64-bit Long; partitions are keyed by [GameMetrics]
 * identity so a window resize automatically isolates stale entries (no clearing storm).
 * Custom sensitivity values are never cached (16-bit aliasing guard, family parity).
 *
 * [PT] Cache de dimensões lock-free e sem alocação em hit.
 * Chaves empacotadas num único Long; as partições são indexadas pela identidade do
 * [GameMetrics], então um redimensionamento isola automaticamente entradas obsoletas.
 * Sensibilidades customizadas nunca são cacheadas (guarda de aliasing de 16 bits).
 */
object GameCache {

    /** [EN] Global cache switch (keep ON for 60+ FPS). [PT] Interruptor global (mantenha ON). */
    @Volatile
    @JvmStatic
    var cacheEnabled: Boolean = true

    private const val PARTITION_SIZE = 512

    private val partitions = ConcurrentHashMap<GameMetrics, AtomicReferenceArray<Slot?>>()

    /** Immutable cached entry: 64-bit key + raw float bits. */
    class Slot(@JvmField val key: Long, @JvmField val valueBits: Long)

    // ─── Key packing ───────────────────────────────────────────────────────
    // [63] applyAR | [62..31] base float bits | [30..27] strategy | [26..24] valueType
    //      | [7..6] qualifier | [5..2] inverter | [1] landscape | [0] ignoreResize

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun buildKey(
        baseValue: Float,
        strategy: Int,
        valueType: Int,
        qualifier: Int,
        inverter: Int,
        landscape: Boolean,
        ignoreResize: Boolean,
        applyAspectRatio: Boolean,
    ): Long =
        ((if (applyAspectRatio) 1L shl 63 else 0L) or
            ((baseValue.toRawBits().toLong() and 0xFFFFFFFFL) shl 31) or
            ((strategy and 0xF).toLong() shl 27) or
            ((valueType and 0x7).toLong() shl 24) or
            ((qualifier and 0x3).toLong() shl 6) or
            ((inverter and 0xF).toLong() shl 2) or
            ((if (landscape) 1 else 0).toLong() shl 1) or
            (if (ignoreResize) 1L else 0L))

    /**
     * [EN] Returns the cached value for [key] or computes via [compute].
     * [PT] Retorna o valor cacheado para [key] ou calcula via [compute].
     */
    inline fun <T> getOrPut(metrics: GameMetrics, key: Long, compute: () -> T): T {
        if (!cacheEnabled) return compute()
        val partition = partitions[metrics] ?: createPartition(metrics)
        val index = mix(key)
        val slot = partition.get(index)
        if (slot != null && slot.key == key) {
            @Suppress("UNCHECKED_CAST")
            return Float.fromBits(slot.valueBits) as T
        }
        val computed = compute()
        val f = computed as? Float
        if (f != null && f.isFinite()) {
            partition.compareAndSet(index, slot, Slot(key, f.toRawBits()))
        }
        return computed
    }

    /** Fast path specialized for Float to avoid boxing on hit. */
    fun getFloatOrPut(metrics: GameMetrics, key: Long, compute: () -> Float): Float {
        if (!cacheEnabled) return compute()
        val partition = partitions[metrics] ?: createPartition(metrics)
        val index = mix(key)
        val slot = partition.get(index)
        if (slot != null && slot.key == key) return Float.fromBits(slot.valueBits)
        val computed = compute()
        if (computed.isFinite()) partition.compareAndSet(index, slot, Slot(key, computed.toRawBits()))
        return computed
    }

    /** Non-caching peek. / Consulta sem cachear. */
    fun peek(metrics: GameMetrics, key: Long): Float? {
        val p = partitions[metrics] ?: return null
        val slot = p.get(mix(key)) ?: return null
        return Float.fromBits(slot.valueBits)
    }

    /** Drops the partition of one snapshot. / Descarta a partição de um snapshot. */
    fun clear(metrics: GameMetrics) { partitions.remove(metrics) }

    /** Drops everything. / Limpa tudo. */
    fun clearAll() { partitions.clear() }

    /** Called by [GameScreen.update] — keeps at most 4 recent partitions alive. */
    internal fun onMetricsChanged(current: GameMetrics) {
        if (partitions.size > 8) {
            partitions.keys.removeIf { it != current && it != GameMetrics.DEFAULT }
        }
    }

    @PublishedApi
    internal fun createPartition(metrics: GameMetrics): AtomicReferenceArray<Slot?> =
        partitions.computeIfAbsent(metrics) { AtomicReferenceArray<Slot?>(PARTITION_SIZE) }

    @PublishedApi
    internal fun mix(key: Long): Int = ((key xor (key ushr 21) xor (key ushr 43)) and 0x1FF).toInt()

    /** Diagnostics. / Diagnóstico. */
    data class Stats(val partitions: Int)

    fun stats(): Stats = Stats(partitions.size)

}
