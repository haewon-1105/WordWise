package au.edu.jcu.cp3406.wordwise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LearningStatsCalculatorTest {

    private val today = LocalDate.of(2026, 11, 24)
    private val calculator = LearningStatsCalculator()

    @Test
    fun emptyDatabaseGivesZerosAndNoAccuracy() {
        val stats = calculator.calculate(
            totalWords = 60,
            boxCounts = emptyMap(),
            totalReviews = 0,
            correctReviews = 0,
            dailyCounts = emptyList(),
            studyDays = emptySet(),
            today = today,
        )

        assertEquals(60, stats.totalWords)
        assertEquals(0, stats.wordsStarted)
        assertEquals(0, stats.wordsMastered)
        assertEquals(listOf(0, 0, 0, 0, 0), stats.wordsPerBox)
        assertNull(stats.accuracy)
        assertEquals(0, stats.reviewsToday)
        assertEquals(0, stats.currentStreakDays)
        assertEquals(7, stats.lastSevenDays.size)
    }

    @Test
    fun boxCountsGiveStartedAndMasteredWords() {
        val stats = calculator.calculate(
            totalWords = 60,
            boxCounts = mapOf(1 to 4, 2 to 3, 5 to 2),
            totalReviews = 20,
            correctReviews = 15,
            dailyCounts = emptyList(),
            studyDays = emptySet(),
            today = today,
        )

        assertEquals(listOf(4, 3, 0, 0, 2), stats.wordsPerBox)
        assertEquals(9, stats.wordsStarted)
        assertEquals(2, stats.wordsMastered)
        assertEquals(0.75, stats.accuracy!!, 1e-9)
    }

    @Test
    fun lastSevenDaysIsOldestFirstAndFillsMissingDaysWithZero() {
        val counts = listOf(
            DailyReviewCount(today.toEpochDay(), 5),
            DailyReviewCount(today.minusDays(2).toEpochDay(), 8),
            DailyReviewCount(today.minusDays(30).toEpochDay(), 99), // outside the window
        )

        val stats = calculator.calculate(60, emptyMap(), 13, 10, counts, emptySet(), today)

        assertEquals(today.minusDays(6).toEpochDay(), stats.lastSevenDays.first().epochDay)
        assertEquals(today.toEpochDay(), stats.lastSevenDays.last().epochDay)
        assertEquals(listOf(0, 0, 0, 0, 8, 0, 5), stats.lastSevenDays.map { it.count })
        assertEquals(5, stats.reviewsToday)
    }

    @Test
    fun streakUsesTheForgivingCalculator() {
        val days = setOf(today, today.minusDays(2))

        val stats = calculator.calculate(60, emptyMap(), 2, 2, emptyList(), days, today)

        assertEquals(2, stats.currentStreakDays)
    }

    @Test
    fun accuracyHelperHandlesTheEdges() {
        assertNull(LearningStatsCalculator.accuracy(0, 0))
        assertEquals(1.0, LearningStatsCalculator.accuracy(4, 4)!!, 1e-9)
        assertEquals(0.0, LearningStatsCalculator.accuracy(0, 4)!!, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun moreCorrectThanTotalIsRejected() {
        LearningStatsCalculator.accuracy(5, 4)
    }
}
