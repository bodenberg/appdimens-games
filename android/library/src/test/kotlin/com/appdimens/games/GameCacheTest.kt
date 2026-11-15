package com.appdimens.games

import com.appdimens.games.core.GameCache
import com.appdimens.games.core.GameMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [EN] Lock-free cache contract: hit/miss, snapshot partition isolation, global switch,
 * peek/clear and stats.
 * [PT] Contrato do cache lock-free: hit/miss, isolamento por snapshot, interruptor
 * global, peek/clear e estatísticas.
 */
class GameCacheTest {

    private val m1 = GameMetrics.of(360, 800)
    private val m2 = GameMetrics.of(600, 960)

    @Test fun computes_once_then_serves_from_cache() {
        var calls = 0
        val k = 42L
        val a = GameCache.getFloatOrPut(m1, k) { calls++; 19.2f }
        val b = GameCache.getFloatOrPut(m1, k) { calls++; 99f }
        assertEquals(19.2f, a, 0f)
        assertEquals(19.2f, b, 0f)
        assertEquals(1, calls)
    }

    @Test fun different_keys_do_not_collide() {
        val a = GameCache.getFloatOrPut(m1, 1L) { 1f }
        val b = GameCache.getFloatOrPut(m1, 2L) { 2f }
        assertEquals(1f, a, 0f)
        assertEquals(2f, b, 0f)
    }

    @Test fun partitions_are_isolated_per_snapshot() {
        val a = GameCache.getFloatOrPut(m1, 7L) { 10f }
        val b = GameCache.getFloatOrPut(m2, 7L) { 20f }
        assertEquals(10f, a, 0f)
        assertEquals(20f, b, 0f)
        // same key, different snapshots → independent values persist
        assertEquals(10f, GameCache.peek(m1, 7L)!!, 0f)
        assertEquals(20f, GameCache.peek(m2, 7L)!!, 0f)
    }

    @Test fun non_finite_values_are_not_cached() {
        val key = 99L
        val v = GameCache.getFloatOrPut(GameMetrics.of(300, 533), key) { Float.NaN }
        assertTrue(v.isNaN())
        assertNull(GameCache.peek(GameMetrics.of(300, 533), key))
    }

    @Test fun disabled_cache_bypasses_storage() {
        GameCache.cacheEnabled = false
        try {
            val v = GameCache.getFloatOrPut(m1, 5L) { 3f }
            assertEquals(3f, v, 0f)
            assertNull(GameCache.peek(m1, 5L))
        } finally {
            GameCache.cacheEnabled = true
        }
    }

    @Test fun generic_get_or_put_boxes_float_result() {
        var calls = 0
        val v: Float = GameCache.getOrPut(m1, 11L) { calls++; 7.5f }
        val v2: Float = GameCache.getOrPut(m1, 11L) { calls++; 0f }
        assertEquals(7.5f, v, 0f)
        assertEquals(7.5f, v2, 0f)
        assertEquals(1, calls)
    }

    @Test fun clear_removes_partition_and_clear_all_empties() {
        GameCache.getFloatOrPut(m1, 3L) { 1f }
        GameCache.getFloatOrPut(m2, 3L) { 2f }
        assertNotEquals(0, GameCache.stats().partitions)
        GameCache.clear(m1)
        assertNull(GameCache.peek(m1, 3L))
        GameCache.clearAll()
        assertEquals(0, GameCache.stats().partitions)
    }

    private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
}
