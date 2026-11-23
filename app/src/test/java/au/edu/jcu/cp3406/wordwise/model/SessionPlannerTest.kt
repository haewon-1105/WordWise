package au.edu.jcu.cp3406.wordwise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlannerTest {

    private fun card(id: Long, studied: Boolean) = Flashcard(
        word = Word(
            id = id,
            term = "word$id",
            partOfSpeech = "noun",
            definition = "definition $id",
            example = null,
            level = 1,
            source = WordSource.DECK,
        ),
        progress = if (studied) CardProgress(id, 2, 0, 1, 0, 0) else null,
    )

    private val due = listOf(card(1, true), card(2, true), card(3, true))
    private val new = listOf(card(10, false), card(11, false), card(12, false))

    @Test
    fun dueReviewsComeBeforeNewWords() {
        val session = SessionPlanner.plan(due, new, dailyGoal = 5, reviewedToday = 0)

        assertEquals(listOf(1L, 2L, 3L, 10L, 11L), session.map { it.word.id })
    }

    @Test
    fun sessionIsLimitedByWhatIsLeftOfTheGoal() {
        val session = SessionPlanner.plan(due, new, dailyGoal = 10, reviewedToday = 8)

        assertEquals(listOf(1L, 2L), session.map { it.word.id })
    }

    @Test
    fun sessionIsEmptyOnceTheGoalIsMet() {
        val session = SessionPlanner.plan(due, new, dailyGoal = 10, reviewedToday = 10)

        assertTrue(session.isEmpty())
        assertTrue(SessionPlanner.isGoalMet(dailyGoal = 10, reviewedToday = 10))
    }

    @Test
    fun goingPastTheGoalNeverProducesMoreCards() {
        val session = SessionPlanner.plan(due, new, dailyGoal = 5, reviewedToday = 9)

        assertTrue(session.isEmpty())
    }

    @Test
    fun aCardInBothListsAppearsOnce() {
        val session = SessionPlanner.plan(due, listOf(card(2, false), card(10, false)), 10, 0)

        assertEquals(listOf(1L, 2L, 3L, 10L), session.map { it.word.id })
    }

    @Test
    fun goalNotMetWhileCardsRemain() {
        assertFalse(SessionPlanner.isGoalMet(dailyGoal = 10, reviewedToday = 4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeGoalIsRejected() {
        SessionPlanner.plan(due, new, dailyGoal = -1, reviewedToday = 0)
    }
}
