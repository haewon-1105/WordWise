package au.edu.jcu.cp3406.wordwise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LeitnerSchedulerTest {

    private val scheduler = LeitnerScheduler()
    private val today = LocalDate.of(2026, 11, 24)

    private fun progress(box: Int, correct: Int = 0, incorrect: Int = 0) = CardProgress(
        wordId = 7,
        box = box,
        dueEpochDay = today.toEpochDay(),
        correctCount = correct,
        incorrectCount = incorrect,
        lastReviewedEpochDay = today.minusDays(1).toEpochDay(),
    )

    @Test
    fun newWordAnsweredCorrectlyMovesToBoxTwo() {
        val result = scheduler.review(wordId = 7, current = null, correct = true, today = today)

        assertEquals(2, result.box)
        assertEquals(today.plusDays(2).toEpochDay(), result.dueEpochDay)
        assertEquals(1, result.correctCount)
        assertEquals(0, result.incorrectCount)
        assertEquals(today.toEpochDay(), result.lastReviewedEpochDay)
    }

    @Test
    fun newWordAnsweredWronglyStaysInBoxOneAndReturnsTomorrow() {
        val result = scheduler.review(wordId = 7, current = null, correct = false, today = today)

        assertEquals(1, result.box)
        assertEquals(today.plusDays(1).toEpochDay(), result.dueEpochDay)
        assertEquals(0, result.correctCount)
        assertEquals(1, result.incorrectCount)
    }

    @Test
    fun correctAnswerMovesUpOneBoxWithLongerInterval() {
        val result = scheduler.review(7, progress(box = 3, correct = 2), correct = true, today = today)

        assertEquals(4, result.box)
        assertEquals(today.plusDays(8).toEpochDay(), result.dueEpochDay)
        assertEquals(3, result.correctCount)
    }

    @Test
    fun wrongAnswerSendsWordBackToBoxOne() {
        val result = scheduler.review(7, progress(box = 4, correct = 3), correct = false, today = today)

        assertEquals(1, result.box)
        assertEquals(today.plusDays(1).toEpochDay(), result.dueEpochDay)
        assertEquals(3, result.correctCount)
        assertEquals(1, result.incorrectCount)
    }

    @Test
    fun lastBoxIsTheCeiling() {
        val result = scheduler.review(7, progress(box = 5), correct = true, today = today)

        assertEquals(5, result.box)
        assertEquals(today.plusDays(16).toEpochDay(), result.dueEpochDay)
        assertTrue(scheduler.isMastered(result.box))
    }

    @Test
    fun onlyTheLastBoxCountsAsMastered() {
        assertFalse(scheduler.isMastered(4))
        assertTrue(scheduler.isMastered(5))
    }

    @Test
    fun customIntervalsAreRespected() {
        val shortScheduler = LeitnerScheduler(listOf(1, 3))

        assertEquals(2, shortScheduler.boxCount)
        assertEquals(3, shortScheduler.intervalFor(2))
        val result = shortScheduler.review(1, null, correct = true, today = today)
        assertEquals(today.plusDays(3).toEpochDay(), result.dueEpochDay)
    }

    @Test(expected = IllegalArgumentException::class)
    fun boxOutsideRangeIsRejected() {
        scheduler.intervalFor(6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroDayIntervalIsRejected() {
        LeitnerScheduler(listOf(1, 0))
    }
}
