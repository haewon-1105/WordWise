package au.edu.jcu.cp3406.wordwise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserSettingsTest {

    @Test
    fun defaultsRespectTheLearner() {
        val defaults = UserSettings()

        assertEquals(10, defaults.dailyGoal)
        assertEquals(Difficulty.MEDIUM, defaults.difficulty)
        assertFalse("Audio must be opt-in", defaults.readWordsAloud)
    }

    @Test
    fun goalIsClampedToTheAllowedRange() {
        assertEquals(5, UserSettings.sanitizeDailyGoal(0))
        assertEquals(5, UserSettings.sanitizeDailyGoal(-10))
        assertEquals(30, UserSettings.sanitizeDailyGoal(500))
    }

    @Test
    fun goalSnapsToTheNearestStepOfFive() {
        assertEquals(10, UserSettings.sanitizeDailyGoal(10))
        assertEquals(10, UserSettings.sanitizeDailyGoal(11))
        assertEquals(15, UserSettings.sanitizeDailyGoal(13))
        assertEquals(30, UserSettings.sanitizeDailyGoal(29))
    }

    @Test
    fun storedDifficultyNamesAreParsedSafely() {
        assertEquals(Difficulty.HARD, UserSettings.parseDifficulty("HARD"))
        assertEquals(Difficulty.MEDIUM, UserSettings.parseDifficulty("unknown"))
        assertEquals(Difficulty.MEDIUM, UserSettings.parseDifficulty(null))
    }
}
