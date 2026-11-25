package au.edu.jcu.cp3406.wordwise.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import au.edu.jcu.cp3406.wordwise.model.Difficulty
import au.edu.jcu.cp3406.wordwise.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** The learner's preferences. An interface so ViewModels can be tested with a fake. */
interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setDailyGoal(goal: Int)
    suspend fun setDifficulty(difficulty: Difficulty)
    suspend fun setReadWordsAloud(enabled: Boolean)
}

/** Stores preferences with Jetpack DataStore, on the device only. */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data
        .catch { e ->
            // A corrupted or unreadable file falls back to the defaults instead of crashing.
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            UserSettings(
                dailyGoal = UserSettings.sanitizeDailyGoal(
                    prefs[DAILY_GOAL] ?: UserSettings.DEFAULT_DAILY_GOAL,
                ),
                difficulty = UserSettings.parseDifficulty(prefs[DIFFICULTY]),
                readWordsAloud = prefs[READ_ALOUD] ?: false,
            )
        }

    override suspend fun setDailyGoal(goal: Int) {
        dataStore.edit { it[DAILY_GOAL] = UserSettings.sanitizeDailyGoal(goal) }
    }

    override suspend fun setDifficulty(difficulty: Difficulty) {
        dataStore.edit { it[DIFFICULTY] = difficulty.name }
    }

    override suspend fun setReadWordsAloud(enabled: Boolean) {
        dataStore.edit { it[READ_ALOUD] = enabled }
    }

    private companion object {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val READ_ALOUD = booleanPreferencesKey("read_words_aloud")
    }
}
