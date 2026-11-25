package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.AiDifficulty
import com.example.game.MatchFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.cricketPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "odd_even_cricket_prefs"
)

data class UserPreferences(
    val format: MatchFormat = MatchFormat.T3_BLITZ,
    val difficulty: AiDifficulty = AiDifficulty.PRO,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val FORMAT = stringPreferencesKey("preferred_format")
        val DIFFICULTY = stringPreferencesKey("preferred_difficulty")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val SOUND = booleanPreferencesKey("sound_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.cricketPrefsDataStore.data
        .catch { e ->
            if (e is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw e
            }
        }
        .map { prefs ->
            UserPreferences(
                format = MatchFormat.fromId(prefs[Keys.FORMAT]),
                difficulty = AiDifficulty.fromId(prefs[Keys.DIFFICULTY]),
                hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
                soundEnabled = prefs[Keys.SOUND] ?: true,
            )
        }

    suspend fun saveFormat(format: MatchFormat) {
        context.cricketPrefsDataStore.edit { it[Keys.FORMAT] = format.id }
    }

    suspend fun saveDifficulty(difficulty: AiDifficulty) {
        context.cricketPrefsDataStore.edit { it[Keys.DIFFICULTY] = difficulty.id }
    }

    suspend fun saveHaptics(enabled: Boolean) {
        context.cricketPrefsDataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun saveSound(enabled: Boolean) {
        context.cricketPrefsDataStore.edit { it[Keys.SOUND] = enabled }
    }
}
