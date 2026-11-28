package au.edu.jcu.cp3406.wordwise.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.edu.jcu.cp3406.wordwise.R
import au.edu.jcu.cp3406.wordwise.model.DailyReviewCount
import au.edu.jcu.cp3406.wordwise.model.Flashcard
import au.edu.jcu.cp3406.wordwise.model.LearningStats
import au.edu.jcu.cp3406.wordwise.model.StudySession
import au.edu.jcu.cp3406.wordwise.model.Word
import au.edu.jcu.cp3406.wordwise.model.WordSource
import au.edu.jcu.cp3406.wordwise.ui.home.HomeContent
import au.edu.jcu.cp3406.wordwise.ui.home.HomeUiState
import au.edu.jcu.cp3406.wordwise.ui.stats.StatsContent
import au.edu.jcu.cp3406.wordwise.ui.stats.StatsUiState
import au.edu.jcu.cp3406.wordwise.ui.study.StudyContent
import au.edu.jcu.cp3406.wordwise.ui.study.StudyUiState
import au.edu.jcu.cp3406.wordwise.ui.theme.WordWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GUI tests for the stateless screen content. They check what the learner sees and that each
 * control calls the right action, including the ethical rules: no "keep going" once the goal
 * is met, and both self-assessment answers offered equally.
 */
@RunWith(AndroidJUnit4::class)
class ScreenContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(@StringRes id: Int, vararg args: Any) = context.getString(id, *args)

    private val card = Flashcard(
        word = Word(1, "analyse", "verb", "To examine something in detail.", null, 1, WordSource.DECK),
        progress = null,
    )

    // --- Home -------------------------------------------------------------------------------

    private fun showHome(reviewed: Int, actions: MutableList<String>) = compose.setContent {
        WordWiseTheme {
            HomeContent(
                uiState = HomeUiState(isLoading = false, totalWords = 60, wordsMastered = 4, reviewedToday = reviewed, dailyGoal = 10),
                onStartSession = { actions += "study" },
                onOpenStats = { actions += "stats" },
                onOpenSettings = { actions += "settings" },
                onAddWord = { actions += "add" },
            )
        }
    }

    @Test
    fun homeStartsTheSessionBeforeTheGoalIsMet() {
        val actions = mutableListOf<String>()
        showHome(reviewed = 7, actions = actions)

        compose.onNodeWithContentDescription(text(R.string.home_goal_description, 7, 10, 70)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_start_session)).performClick()

        assertEquals(listOf("study"), actions)
    }

    @Test
    fun homeOffersProgressNotMoreCardsOnceTheGoalIsMet() {
        val actions = mutableListOf<String>()
        showHome(reviewed = 10, actions = actions)

        compose.onNodeWithText(text(R.string.home_start_session)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.home_shortcut_stats)).performClick()

        assertEquals(listOf("stats"), actions)
    }

    @Test
    fun homeShortcutsOpenTheirScreens() {
        val actions = mutableListOf<String>()
        showHome(reviewed = 0, actions = actions)

        compose.onNodeWithText(text(R.string.home_add_word_title)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.home_preferences_title)).performScrollTo().performClick()

        assertEquals(listOf("add", "settings"), actions)
    }

    // --- Study ------------------------------------------------------------------------------

    private fun showStudy(state: StudyUiState, actions: MutableList<String>) = compose.setContent {
        WordWiseTheme {
            StudyContent(
                uiState = state,
                onFlip = { actions += "flip" },
                onAnswer = { correct -> actions += if (correct) "knew" else "learning" },
                onPlayPronunciation = { actions += "play" },
                onRetry = { actions += "retry" },
                onOpenStats = { actions += "stats" },
                onOpenSettings = { actions += "settings" },
                onAddWord = { actions += "add" },
            )
        }
    }

    @Test
    fun answersAppearOnlyAfterTheCardIsTurned() {
        val actions = mutableListOf<String>()
        showStudy(StudyUiState.InSession(StudySession(listOf(card)), null, readWordsAloud = false), actions)

        compose.onNodeWithText(text(R.string.study_knew_it)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.study_show_meaning)).performScrollTo().performClick()

        assertEquals(listOf("flip"), actions)
    }

    @Test
    fun bothSelfAssessmentAnswersAreOffered() {
        val actions = mutableListOf<String>()
        val flipped = StudySession(listOf(card)).flip()
        showStudy(StudyUiState.InSession(flipped, null, readWordsAloud = false), actions)

        compose.onNodeWithText(text(R.string.study_still_learning)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.study_knew_it)).performScrollTo().performClick()

        assertEquals(listOf("learning", "knew"), actions)
    }

    @Test
    fun doneForTodayHasNoKeepGoingButton() {
        val actions = mutableListOf<String>()
        showStudy(StudyUiState.Finished(10, 8, reviewedToday = 10, dailyGoal = 10), actions)

        compose.onNodeWithText(text(R.string.study_done_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.study_show_meaning)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.home_start_session)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.home_shortcut_stats)).performScrollTo().performClick()

        assertEquals(listOf("stats"), actions)
    }

    // --- Stats ------------------------------------------------------------------------------

    @Test
    fun statsExplainThatProgressIsPrivate() {
        val stats = LearningStats(
            totalWords = 60, wordsStarted = 10, wordsMastered = 2,
            wordsPerBox = listOf(4, 2, 1, 1, 2), totalReviews = 20, correctReviews = 15,
            accuracy = 0.75, reviewsToday = 5, currentStreakDays = 3,
            lastSevenDays = (0L..6L).map { DailyReviewCount(20_000 + it, it.toInt()) },
        )
        compose.setContent { WordWiseTheme { StatsContent(StatsUiState.Ready(stats, dailyGoal = 10)) } }

        // Each tile is read as one sentence, e.g. "75%, accuracy (15 of 20 answers)".
        compose.onNodeWithContentDescription("75%", substring = true).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.stats_privacy)).performScrollTo().assertIsDisplayed()
    }
}
