package com.luum.michi.app.core.util

/**
 * Tiny generic cache with an insertion-order eviction policy — the closest
 * approximation of LRU available in Kotlin/Native's [LinkedHashMap]: a `put` of
 * an existing key re-inserts it (a touch), so the oldest untouched entry is
 * evicted first once [maxSize] is exceeded. Pure (no Compose), so it is
 * unit-tested in `core/` and shared by the four detail holders.
 */
internal class DetailCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()

    fun get(key: K): V? = map[key]

    fun put(key: K, value: V) {
        map.remove(key)
        map[key] = value
        if (map.size > maxSize) map.remove(map.keys.first())
    }

    fun remove(key: K) {
        map.remove(key)
    }

    /** Drops every snapshot (e.g. a language change re-localizes all of them). */
    fun clear() {
        map.clear()
    }

    /**
     * Rewrites the entry for [key] in place when it is present, so a late
     * response can update **its own** snapshot without touching the one being
     * shown (the toggle-favourite fix). Returns the new value, or null when the
     * key is not cached.
     */
    fun update(key: K, transform: (V) -> V): V? {
        val current = map[key] ?: return null
        val next = transform(current)
        put(key, next)
        return next
    }
}
