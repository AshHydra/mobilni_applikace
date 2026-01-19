package cz.ash.mobilniapplikace.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context
) {
    private val keyVsCurrency = stringPreferencesKey("vs_currency")
    private val keyManualCurrency = stringPreferencesKey("manual_currency")
    private val keyUseLocationCurrency = booleanPreferencesKey("use_location_currency")

    val vsCurrencyFlow: Flow<String> = context.dataStore.data.map { prefs: Preferences ->
        (prefs[keyVsCurrency] ?: "usd").lowercase()
    }

    val useLocationCurrencyFlow: Flow<Boolean> = context.dataStore.data.map { prefs: Preferences ->
        prefs[keyUseLocationCurrency] ?: false
    }

    suspend fun setVsCurrency(code: String) {
        context.dataStore.edit { prefs ->
            prefs[keyVsCurrency] = code.lowercase()
        }
    }

    suspend fun setManualCurrency(code: String) {
        val c = code.lowercase()
        context.dataStore.edit { prefs ->
            prefs[keyManualCurrency] = c
            prefs[keyVsCurrency] = c
            prefs[keyUseLocationCurrency] = false
        }
    }

    suspend fun setUseLocationCurrency(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyUseLocationCurrency] = enabled
        }
    }

    suspend fun enableLocationCurrency(currentVsCurrency: String) {
        val current = currentVsCurrency.lowercase()
        context.dataStore.edit { prefs ->
            // Remember the previous manual currency so we can restore it when switch turns off.
            if (prefs[keyManualCurrency].isNullOrBlank()) {
                prefs[keyManualCurrency] = current
            } else {
                // Always keep manual currency in sync with last non-location selection.
                prefs[keyManualCurrency] = prefs[keyManualCurrency] ?: current
            }
            prefs[keyUseLocationCurrency] = true
        }
    }

    suspend fun disableLocationCurrencyAndRestoreManual() {
        context.dataStore.edit { prefs ->
            val manual = (prefs[keyManualCurrency] ?: "usd").lowercase()
            prefs[keyUseLocationCurrency] = false
            prefs[keyVsCurrency] = manual
        }
    }
}

