package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.ebookMode
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalEbookMode = compositionLocalOf { EbookModePreference.default }

class EbookModePreference(val value: Boolean) : Preference() {
    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(DataStoreKey.ebookMode, value)
        }
    }

    fun toggle(context: Context, scope: CoroutineScope) =
        EbookModePreference(!value).put(context, scope)

    companion object {
        val default = EbookModePreference(false)
        fun fromPreference(preference: Preferences): EbookModePreference {
            return EbookModePreference(
                preference[DataStoreKey.keys[ebookMode]?.key as Preferences.Key<Boolean>] ?: return default
            )
        }
    }
}