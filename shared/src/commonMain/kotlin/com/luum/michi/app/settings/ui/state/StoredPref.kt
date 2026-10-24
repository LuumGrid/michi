package com.luum.michi.app.settings.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.settings.domain.model.NotificationPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Codec separator: the unit separator never appears in category names. */
private const val NAME_SEPARATOR = "\u001F"

/**
 * One persisted preference: its Compose state, its default and its store codec.
 * [onChanged] is the public setter's side effect — schedule the AniList save for
 * a synced pref, nothing for a local one; [hydrate]/[reset]/[setSilently] never
 * call it. Used as the property delegate for the public `var`s on SettingsState.
 */
internal class StoredPref<T>(
    private val default: T,
    private val read: () -> T,
    private val write: (T) -> Unit,
    private val onChanged: () -> Unit = {},
) : ReadWriteProperty<Any?, T> {
    var value: T by mutableStateOf(default)
        private set

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        write(value)
        onChanged()
    }

    /** Store -> state; a missing key (or unknown enum name) keeps the default. */
    fun hydrate() {
        value = read()
    }

    /** Back to the default, persisted, without scheduling a save. */
    fun reset() {
        value = default
        write(default)
    }

    /** State + store without a save ([seedFrom] / [applyLoaded]). */
    fun setSilently(newValue: T) {
        value = newValue
        write(newValue)
    }

    /** [applyLoaded]: keep the user's edit when it differs from the load-start value. */
    fun applyLoaded(loaded: T, baseline: T?) {
        setSilently(if (baseline != null && value != baseline) value else loaded)
    }
}

internal fun booleanPref(
    key: String,
    default: Boolean,
    store: SettingsStore,
    onChanged: () -> Unit = {},
): StoredPref<Boolean> = StoredPref(
    default = default,
    read = { store.getBoolean(key, default) },
    write = { store.putBoolean(key, it) },
    onChanged = onChanged,
)

internal fun <E : Enum<E>> enumPref(
    key: String,
    default: E,
    values: List<E>,
    store: SettingsStore,
    onChanged: () -> Unit = {},
): StoredPref<E> = StoredPref(
    default = default,
    read = { store.getString(key)?.let { saved -> values.firstOrNull { it.name == saved } } ?: default },
    write = { store.putString(key, it.name) },
    onChanged = onChanged,
)

/**
 * A value persisted through an explicit codec instead of an enum name: themes
 * and language use stable ids/codes so a class or enum rename never loses what
 * the user already has saved.
 */
internal fun <T> codedPref(
    key: String,
    store: SettingsStore,
    decode: (String?) -> T,
    encode: (T) -> String,
    onChanged: () -> Unit = {},
): StoredPref<T> = StoredPref(
    default = decode(null),
    read = { decode(store.getString(key)) },
    write = { store.putString(key, encode(it)) },
    onChanged = onChanged,
)

internal fun stringPref(
    key: String,
    default: String = "",
    store: SettingsStore,
    onChanged: () -> Unit = {},
): StoredPref<String> = StoredPref(
    default = default,
    read = { store.getString(key) ?: default },
    write = { store.putString(key, it) },
    onChanged = onChanged,
)

internal fun stringListPref(
    key: String,
    store: SettingsStore,
    onChanged: () -> Unit = {},
): StoredPref<List<String>> = StoredPref(
    default = emptyList(),
    read = { decodeNames(store.getString(key)) },
    write = { store.putString(key, encodeNames(it)) },
    onChanged = onChanged,
)

/** The six notification bucket keys, grouped so the factory stays readable. */
internal data class NotificationKeys(
    val airing: String,
    val activity: String,
    val following: String,
    val forum: String,
    val messages: String,
    val media: String,
)

internal fun notificationsStoredPref(
    keys: NotificationKeys,
    store: SettingsStore,
    onChanged: () -> Unit = {},
): StoredPref<NotificationPreferences> {
    val default = NotificationPreferences()
    return StoredPref(
        default = default,
        read = {
            NotificationPreferences(
                airing = store.getBoolean(keys.airing, default.airing),
                activity = store.getBoolean(keys.activity, default.activity),
                following = store.getBoolean(keys.following, default.following),
                forum = store.getBoolean(keys.forum, default.forum),
                messages = store.getBoolean(keys.messages, default.messages),
                media = store.getBoolean(keys.media, default.media),
            )
        },
        write = { value ->
            store.putBoolean(keys.airing, value.airing)
            store.putBoolean(keys.activity, value.activity)
            store.putBoolean(keys.following, value.following)
            store.putBoolean(keys.forum, value.forum)
            store.putBoolean(keys.messages, value.messages)
            store.putBoolean(keys.media, value.media)
        },
        onChanged = onChanged,
    )
}

private fun encodeNames(names: List<String>): String =
    names.filter { it.isNotBlank() }.joinToString(NAME_SEPARATOR)

private fun decodeNames(raw: String?): List<String> =
    raw?.split(NAME_SEPARATOR).orEmpty().filter { it.isNotBlank() }
