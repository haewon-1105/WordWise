package au.edu.jcu.cp3406.wordwise.ui

import au.edu.jcu.cp3406.wordwise.model.UserSettings
import au.edu.jcu.cp3406.wordwise.ui.study.StudyUiState
import au.edu.jcu.cp3406.wordwise.ui.study.StudyViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StudyViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository(UserSettings(dailyGoal = 5))
    private val dictionary = FakeDictionaryRepository()
    private val player = FakePronunciationPlayer()

    private fun viewModel(repository: FakeStudyRepository) =
        StudyViewModel(repository, dictionary, settings, player, FixedTimeProvider())

    private fun StudyViewModel.inSession() = uiState.value as StudyUiState.InSession

    @Test
    fun loadsTodaysCardsUsingTheLearnersGoal() = runTest {
        val repository = FakeStudyRepository(cards = listOf(testCard(1), testCard(2)))
        val vm = viewModel(repository)

        vm.refreshIfIdle()

        val state = vm.inSession()
        assertEquals(2, state.session.total)
        assertEquals(1L, state.session.current?.word?.id)
        assertFalse(state.session.isFlipped)
        assertNull(state.lastFeedback)
        assertEquals(5, repository.sessionRequests.single().first)
    }

    @Test
    fun answeringBeforeTurningTheCardIsIgnored() = runTest {
        val repository = FakeStudyRepository(cards = listOf(testCard(1)))
        val vm = viewModel(repository)
        vm.refreshIfIdle()

        vm.answer(correct = true)

        assertTrue(repository.answers.isEmpty())
        assertEquals(0, vm.inSession().session.answeredCount)
    }

    @Test
    fun answerIsSavedAndFeedbackShowsTheNewBox() = runTest {
        val repository = FakeStudyRepository(cards = listOf(testCard(1), testCard(2)))
        val vm = viewModel(repository)
        vm.refreshIfIdle()

        vm.flip()
        vm.answer(correct = true)

        assertEquals(listOf(1L to true), repository.answers)
        val state = vm.inSession()
        assertEquals(2L, state.session.current?.word?.id)
        assertFalse("next card starts face up", state.session.isFlipped)
        assertEquals(2, state.lastFeedback?.box)
        assertEquals(2, state.lastFeedback?.daysUntilNextReview)
    }

    @Test
    fun lastAnswerEndsTheSessionWithoutOfferingMore() = runTest {
        val repository = FakeStudyRepository(cards = listOf(testCard(1)), reviewedToday = 4)
        val vm = viewModel(repository)
        vm.refreshIfIdle()

        vm.flip()
        vm.answer(correct = false)

        val state = vm.uiState.value as StudyUiState.Finished
        assertEquals(1, state.answeredThisSession)
        assertEquals(0, state.correctThisSession)
        assertEquals(5, state.reviewedToday)
        assertTrue("goal of 5 reached", state.goalMet)
    }

    @Test
    fun noDueCardsShowsNothingDueRatherThanDone() = runTest {
        val vm = viewModel(FakeStudyRepository(cards = emptyList(), reviewedToday = 1))

        vm.refreshIfIdle()

        val state = vm.uiState.value as StudyUiState.Finished
        assertEquals(0, state.answeredThisSession)
        assertFalse(state.goalMet)
    }

    @Test
    fun deckFailureShowsTheRetryState() = runTest {
        val repository = FakeStudyRepository(cards = listOf(testCard(1))).apply { failToLoad = true }
        val vm = viewModel(repository)

        vm.refreshIfIdle()

        assertEquals(StudyUiState.LoadFailed, vm.uiState.value)
    }

    @Test
    fun refreshDoesNotRestartASessionPartWayThrough() = runTest {
        val repository = FakeStudyRepository(cards = listOf(testCard(1), testCard(2)))
        val vm = viewModel(repository)
        vm.refreshIfIdle()
        vm.flip()
        vm.answer(correct = true)

        vm.refreshIfIdle()

        assertEquals(1, repository.sessionRequests.size)
        assertEquals(1, vm.inSession().session.answeredCount)
    }

    @Test
    fun readAloudSettingAppliesWithoutRestarting() = runTest {
        val vm = viewModel(FakeStudyRepository(cards = listOf(testCard(1))))
        vm.refreshIfIdle()
        assertFalse(vm.inSession().readWordsAloud)

        settings.setReadWordsAloud(true)

        assertTrue(vm.inSession().readWordsAloud)
    }

    @Test
    fun pronunciationIsFetchedOnceAndPlayed() = runTest {
        dictionary.pronunciationUrl = "https://example.org/word1.mp3"
        val vm = viewModel(FakeStudyRepository(cards = listOf(testCard(1))))
        vm.refreshIfIdle()

        vm.playPronunciation()

        assertEquals(listOf("word1" to "https://example.org/word1.mp3"), player.played)
        assertEquals("https://example.org/word1.mp3", vm.inSession().session.current?.word?.audioUrl)
    }
}
