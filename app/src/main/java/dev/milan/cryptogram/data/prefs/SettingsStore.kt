package dev.milan.cryptogram.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.milan.cryptogram.engine.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Typed wrapper over DataStore Preferences for every persisted setting (design doc 5.2).
 * Getters return [Flow]; setters are `suspend`.
 */
class SettingsStore(private val context: Context) {

    private val data: Flow<Preferences> get() = context.dataStore.data

    // --- corpus ---------------------------------------------------------------

    val corpusLoadedVersion: Flow<Int> = data.map { it[CORPUS_LOADED_VERSION] ?: 0 }

    suspend fun setCorpusLoadedVersion(value: Int) {
        context.dataStore.edit { it[CORPUS_LOADED_VERSION] = value }
    }

    fun frozenMaxId(difficulty: Difficulty): Flow<Int?> =
        data.map { it[FROZEN_MAX_ID[difficulty]!!] }

    /** Writes the frozen max id only if it has never been set. */
    suspend fun setFrozenMaxIdIfUnset(difficulty: Difficulty, value: Int) {
        context.dataStore.edit { prefs ->
            val key = FROZEN_MAX_ID[difficulty]!!
            if (prefs[key] == null) prefs[key] = value
        }
    }

    // --- billing ------------------------------------------------------------

    val removeAdsOwned: Flow<Boolean> = data.map { it[REMOVE_ADS_OWNED] ?: false }

    suspend fun setRemoveAdsOwned(value: Boolean) {
        context.dataStore.edit { it[REMOVE_ADS_OWNED] = value }
    }

    // --- preferences ------------------------------------------------------

    val soundEnabled: Flow<Boolean> = data.map { it[SOUND_ENABLED] ?: true }

    suspend fun setSoundEnabled(value: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED] = value }
    }

    val hapticsEnabled: Flow<Boolean> = data.map { it[HAPTICS_ENABLED] ?: true }

    suspend fun setHapticsEnabled(value: Boolean) {
        context.dataStore.edit { it[HAPTICS_ENABLED] = value }
    }

    val themeMode: Flow<ThemeMode> = data.map {
        runCatching { ThemeMode.valueOf(it[THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = value.name }
    }

    // --- progress cursors --------------------------------------------------

    fun currentLevel(difficulty: Difficulty): Flow<Int> =
        data.map { it[CURRENT_LEVEL[difficulty]!!] ?: 1 }

    suspend fun setCurrentLevel(difficulty: Difficulty, level: Int) {
        context.dataStore.edit { it[CURRENT_LEVEL[difficulty]!!] = level }
    }

    // --- daily / consent -------------------------------------------------

    val lastDailyDateSeen: Flow<String?> = data.map { it[LAST_DAILY_DATE_SEEN] }

    suspend fun setLastDailyDateSeen(value: String) {
        context.dataStore.edit { it[LAST_DAILY_DATE_SEEN] = value }
    }

    val consentObtained: Flow<Boolean> = data.map { it[CONSENT_OBTAINED] ?: false }

    suspend fun setConsentObtained(value: Boolean) {
        context.dataStore.edit { it[CONSENT_OBTAINED] = value }
    }

    val bestStreak: Flow<Int> = data.map { it[BEST_STREAK] ?: 0 }

    suspend fun setBestStreak(value: Int) {
        context.dataStore.edit { it[BEST_STREAK] = value }
    }

    private companion object {
        val CORPUS_LOADED_VERSION = intPreferencesKey("corpusLoadedVersion")
        val REMOVE_ADS_OWNED = booleanPreferencesKey("removeAdsOwned")
        val SOUND_ENABLED = booleanPreferencesKey("soundEnabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("hapticsEnabled")
        val THEME_MODE = stringPreferencesKey("themeMode")
        val LAST_DAILY_DATE_SEEN = stringPreferencesKey("lastDailyDateSeen")
        val CONSENT_OBTAINED = booleanPreferencesKey("consentObtained")
        val BEST_STREAK = intPreferencesKey("bestStreak")

        val FROZEN_MAX_ID: Map<Difficulty, Preferences.Key<Int>> =
            Difficulty.entries.associateWith { intPreferencesKey("frozenMaxId_${it.name}") }
        val CURRENT_LEVEL: Map<Difficulty, Preferences.Key<Int>> =
            Difficulty.entries.associateWith { intPreferencesKey("currentLevel_${it.name}") }
    }
}
