package au.edu.jcu.cp3406.wordwise.ui

import au.edu.jcu.cp3406.wordwise.model.Difficulty
import au.edu.jcu.cp3406.wordwise.model.UserSettings
import au.edu.jcu.cp3406.wordwise.ui.home.HomeViewModel
import au.edu.jcu.cp3406.wordwise.ui.settings.SettingsViewModel
import au.edu.jcu.cp3406.wordwise.ui.stats.StatsUiState
import au.edu.jcu.cp3406.wordwise.ui.stats.StatsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Settings, Home and Stats ViewModels: thin, so a few focused tests each. */
class SettingsHomeStatsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    // --- Settings ---------------------------------------------------------------------------

    @Test
    fun settingsChangesAreSavedAndShown() = runTest {
        val vm = SettingsViewModel(FakeSettingsRepository(), FakeStudyRepository())
        backgroundScope.launch(mainRule.dispatcher) { vm.settings.collect {} }

        vm.setDailyGoal(15)
        vm.setDifficulty(Difficulty.HARD)
        vm.setReadWordsAloud(true)

        assertEquals(UserSettings(dailyGoal = 15, difficulty = Difficulty.HARD, readWordsAloud = true), vm.settings.value)
    }

    @Test
    fun goalOutsideTheAllowedRangeIsKeptSensible() = runTest {
        val vm = SettingsViewModel(FakeSettingsRepository(), FakeStudyRepository())
        backgroundScope.launch(mainRule.dispatcher) { vm.settings.collect {} }

        vm.setDailyGoal(500)

        assertEquals(UserSettings.MAX_DAILY_GOAL, vm.settings.value.dailyGoal)
    }

    @Test
    fun resetClearsProgressAndConfirmsOnce() = runTest {
        val study = FakeStudyRepository(reviewedToday = 3)
        val vm = SettingsViewModel(FakeSettingsRepository(), study)

        vm.resetProgress()

        assertEquals(1, study.resetCalls)
        assertTrue(vm.progressReset.value)
        vm.onResetMessageShown()
        assertFalse(vm.progressReset.value)
    }

    // --- Home -------------------------------------------------------------------------------

    @Test
    fun homeShowsProgressTowardTheLearnersGoal() = runTest {
        val study = FakeStudyRepository().apply {
            stats.value = emptyStats(totalWords = 60, wordsMastered = 4, reviewsToday = 7)
        }
        val vm = HomeViewModel(study, FakeSettingsRepository(UserSettings(dailyGoal = 10)))
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.loadFailed)
        assertEquals(60, state.totalWords)
        assertEquals(4, state.wordsMastered)
        assertEquals(7, state.reviewedToday)
        assertEquals(10, state.dailyGoal)
        assertFalse(state.goalMet)
    }

    @Test
    fun homeKnowsWhenTheGoalIsMet() = runTest {
        val study = FakeStudyRepository().apply { stats.value = emptyStats(reviewsToday = 10) }
        val vm = HomeViewModel(study, FakeSettingsRepository(UserSettings(dailyGoal = 10)))
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        assertTrue(vm.uiState.value.goalMet)
    }

    @Test
    fun homeReportsAMissingDeck() = runTest {
        val study = FakeStudyRepository().apply { failToLoad = true }
        val vm = HomeViewModel(study, FakeSettingsRepository())
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        assertTrue(vm.uiState.value.loadFailed)
        assertFalse(vm.uiState.value.isLoading)
    }

    // --- Stats ------------------------------------------------------------------------------

    @Test
    fun statsUpdateLiveWhenAnAnswerIsSaved() = runTest {
        val study = FakeStudyRepository()
        val vm = StatsViewModel(study, FakeSettingsRepository(UserSettings(dailyGoal = 20)))
        backgroundScope.launch(mainRule.dispatcher) { vm.uiState.collect {} }

        study.stats.value = emptyStats(totalWords = 60, wordsMastered = 2)

        val state = vm.uiState.value as StatsUiState.Ready
        assertEquals(2, state.stats.wordsMastered)
        assertEquals(20, state.dailyGoal)
    }
}
