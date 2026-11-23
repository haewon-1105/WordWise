package au.edu.jcu.cp3406.wordwise.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 11, 24)
    private fun daysAgo(vararg n: Long) = n.map { today.minusDays(it) }.toSet()

    private val forgiving = StreakCalculator(restDaysAllowed = 1)
    private val strict = StreakCalculator(restDaysAllowed = 0)

    @Test
    fun noStudyDaysMeansNoStreak() {
        assertEquals(0, forgiving.currentStreak(emptySet(), today))
    }

    @Test
    fun consecutiveDaysIncludingTodayAreCounted() {
        assertEquals(3, forgiving.currentStreak(daysAgo(0, 1, 2), today))
    }

    @Test
    fun todayNotYetStudiedDoesNotBreakTheStreak() {
        assertEquals(2, forgiving.currentStreak(daysAgo(1, 2), today))
    }

    @Test
    fun oneRestDayIsForgiven() {
        // Studied today, rested yesterday, studied the two days before.
        assertEquals(3, forgiving.currentStreak(daysAgo(0, 2, 3), today))
    }

    @Test
    fun twoRestDaysInARowEndTheStreak() {
        assertEquals(1, forgiving.currentStreak(daysAgo(0, 3, 4), today))
    }

    @Test
    fun restDayAfterYesterdayIsAlsoForgiven() {
        // Not studied yet today, missed yesterday, studied two and three days ago.
        assertEquals(2, forgiving.currentStreak(daysAgo(2, 3), today))
    }

    @Test
    fun strictModeBreaksOnAnyMissedDay() {
        assertEquals(1, strict.currentStreak(daysAgo(0, 2, 3), today))
    }

    @Test
    fun oldStudyDaysBeyondAGapAreIgnored() {
        assertEquals(0, forgiving.currentStreak(daysAgo(5, 6, 7), today))
    }
}
