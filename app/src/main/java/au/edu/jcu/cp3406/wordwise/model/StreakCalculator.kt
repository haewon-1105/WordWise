package au.edu.jcu.cp3406.wordwise.model

import java.time.LocalDate

/**
 * Counts the learner's current study streak, with forgiveness built in.
 *
 * A strict streak resets after one missed day, which is a well-known source of guilt and
 * compulsive checking. Here a gap of up to [restDaysAllowed] days in a row does not break the
 * streak, and today never counts as missed because the day is not over yet.
 *
 * The streak is the number of days the learner actually studied, not calendar days.
 */
class StreakCalculator(
    private val restDaysAllowed: Int = 1,
) {
    init {
        require(restDaysAllowed >= 0) { "Rest days cannot be negative" }
    }

    fun currentStreak(studyDays: Set<LocalDate>, today: LocalDate): Int {
        val earliest = studyDays.minOrNull() ?: return 0

        var streak = 0
        var gap = 0
        // Today only counts if the learner has already studied; otherwise start from yesterday.
        var day = if (today in studyDays) today else today.minusDays(1)

        while (!day.isBefore(earliest)) {
            if (day in studyDays) {
                streak++
                gap = 0
            } else {
                gap++
                if (gap > restDaysAllowed) break
            }
            day = day.minusDays(1)
        }
        return streak
    }
}
