package com.luum.michi.app.core

import com.luum.michi.app.core.util.DetailCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetailCacheTest {

    @Test
    fun putAndGetRoundTrip() {
        val cache = DetailCache<Int, String>(maxSize = 3)
        cache.put(1, "a")
        assertEquals("a", cache.get(1))
        assertNull(cache.get(2))
    }

    @Test
    fun evictsTheOldestUntouchedEntry() {
        val cache = DetailCache<Int, String>(maxSize = 2)
        cache.put(1, "a")
        cache.put(2, "b")
        cache.put(3, "c")

        assertNull(cache.get(1), "the first insert is evicted")
        assertEquals("b", cache.get(2))
        assertEquals("c", cache.get(3))
    }

    @Test
    fun puttingAnExistingKeyRefreshesItsPosition() {
        val cache = DetailCache<Int, String>(maxSize = 2)
        cache.put(1, "a")
        cache.put(2, "b")
        cache.put(1, "a2")
        cache.put(3, "c")

        assertEquals("a2", cache.get(1), "the touched key survives")
        assertNull(cache.get(2), "the untouched one is evicted")
    }

    @Test
    fun getRefreshesRecencySoAFrequentlyReadEntrySurvives() {
        val cache = DetailCache<Int, String>(maxSize = 3)
        cache.put(1, "a")
        cache.put(2, "b")
        cache.put(3, "c")
        assertEquals("a", cache.get(1), "the read touches a")
        cache.put(4, "d")

        assertEquals("a", cache.get(1), "the recently read entry survives")
        assertNull(cache.get(2), "the least recently used one is evicted")
        assertEquals("c", cache.get(3))
        assertEquals("d", cache.get(4))
    }

    @Test
    fun removeDropsTheEntry() {
        val cache = DetailCache<Int, String>(maxSize = 2)
        cache.put(1, "a")
        cache.remove(1)
        assertNull(cache.get(1))
    }

    @Test
    fun updateRewritesTheEntryInPlace() {
        val cache = DetailCache<Int, String>(maxSize = 2)
        cache.put(1, "a")
        assertEquals("a!", cache.update(1) { "$it!" })
        assertEquals("a!", cache.get(1))
    }

    @Test
    fun updateOnAMissingKeyIsANoOp() {
        val cache = DetailCache<Int, String>(maxSize = 2)
        assertNull(cache.update(9) { "$it!" })
    }
}
