package au.edu.jcu.cp3406.wordwise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StudySessionTest {

    private fun card(id: Long) = Flashcard(
        word = Word(id, "word$id", "noun", "definition $id", null, 1, WordSource.DECK),
        progress = null,
    )

    private val session = StudySession(listOf(card(1), card(2), card(3)))

    @Test
    fun newSessionStartsOnTheFirstCardFaceDown() {
        assertEquals(1L, session.current!!.word.id)
        assertFalse(session.isFlipped)
        assertEquals(0, session.answeredCount)
        assertEquals(0f, session.progress, 1e-6f)
        assertFalse(session.isFinished)
    }

    @Test
    fun flipTurnsTheCardBothWays() {
        assertTrue(session.flip().isFlipped)
        assertFalse(session.flip().flip().isFlipped)
    }

    @Test
    fun answeringMovesToTheNextCardFaceDownAndCountsCorrectAnswers() {
        val afterOne = session.flip().answer(correct = true)
        val afterTwo = afterOne.flip().answer(correct = false)

        assertEquals(2L, afterOne.current!!.word.id)
        assertFalse(afterOne.isFlipped)
        assertEquals(2, afterTwo.answeredCount)
        assertEquals(1, afterTwo.correctCount)
        assertEquals(2f / 3f, afterTwo.progress, 1e-6f)
    }

    @Test
    fun sessionFinishesAfterTheLastCard() {
        val finished = session
            .flip().answer(true)
            .flip().answer(true)
            .flip().answer(false)

        assertTrue(finished.isFinished)
        assertNull(finished.current)
        assertEquals(2, finished.correctCount)
        assertEquals(1f, finished.progress, 1e-6f)
        assertSame(finished, finished.flip())
    }

    @Test(expected = IllegalStateException::class)
    fun answeringBeforeTurningTheCardIsNotAllowed() {
        session.answer(correct = true)
    }

    @Test(expected = IllegalStateException::class)
    fun answeringAfterTheEndIsNotAllowed() {
        StudySession(emptyList()).answer(correct = true)
    }

    @Test
    fun emptySessionIsFinishedWithFullProgress() {
        val empty = StudySession(emptyList())

        assertTrue(empty.isFinished)
        assertEquals(1f, empty.progress, 1e-6f)
    }

    @Test
    fun updatingTheCurrentWordKeepsEverythingElse() {
        val withPhonetic = session.current!!.word.copy(phonetic = "/wɜːd/")

        val updated = session.updateCurrentWord(withPhonetic)

        assertEquals("/wɜːd/", updated.current!!.word.phonetic)
        assertEquals(session.cards.drop(1), updated.cards.drop(1))
        assertEquals(session.position, updated.position)
    }

    @Test
    fun updatingWithADifferentWordChangesNothing() {
        val other = card(99).word

        assertSame(session, session.updateCurrentWord(other))
    }

    @Test
    fun feedbackDescribesTheNewBoxAndWaitingTime() {
        val progress = CardProgress(1, box = 3, dueEpochDay = 104, correctCount = 2, incorrectCount = 0, lastReviewedEpochDay = 100)

        val feedback = AnswerFeedback.from(correct = true, progress = progress, todayEpochDay = 100)

        assertEquals(AnswerFeedback(correct = true, box = 3, daysUntilNextReview = 4), feedback)
    }

    @Test
    fun feedbackNeverShowsNegativeDays() {
        val overdue = CardProgress(1, 1, dueEpochDay = 90, correctCount = 0, incorrectCount = 1, lastReviewedEpochDay = 89)

        assertEquals(0, AnswerFeedback.from(false, overdue, todayEpochDay = 100).daysUntilNextReview)
    }
}
