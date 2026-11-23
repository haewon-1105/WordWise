package au.edu.jcu.cp3406.wordwise.model

import java.time.LocalDate

/**
 * Turns raw counts from the database into the figures shown on the Stats screen.
 * Kept separate from the repository so the arithmetic can be unit tested.
 */
class LearningStatsCalculator(
    private val boxCount: Int = LeitnerScheduler.DEFAULT_INTERVALS.size,
    private val streakCalculator: StreakCalculator = StreakCalculator(),
) {

    /**
     * @param totalWords words in the database.
     * @param boxCounts number of words in each box, keyed by box number (1-based).
     * @param totalReviews all answers ever given.
     * @param correctReviews correct answers ever given.
     * @param dailyCounts review counts per day; any range, unordered.
     * @param studyDays every day with at least one review.
     */
    fun calculate(
        totalWords: Int,
        boxCounts: Map<Int, Int>,
        totalReviews: Int,
        correctReviews: Int,
        dailyCounts: List<DailyReviewCount>,
        studyDays: Set<LocalDate>,
        today: LocalDate,
    ): LearningStats {
        val wordsPerBox = (1..boxCount).map { box -> boxCounts[box] ?: 0 }
        val countsByDay = dailyCounts.associate { it.epochDay to it.count }
        val lastSevenDays = (6 downTo 0).map { daysAgo ->
            val epochDay = today.minusDays(daysAgo.toLong()).toEpochDay()
            DailyReviewCount(epochDay, countsByDay[epochDay] ?: 0)
        }

        return LearningStats(
            totalWords = totalWords,
            wordsStarted = wordsPerBox.sum(),
            wordsMastered = wordsPerBox.last(),
            wordsPerBox = wordsPerBox,
            totalReviews = totalReviews,
            correctReviews = correctReviews,
            accuracy = accuracy(correctReviews, totalReviews),
            reviewsToday = countsByDay[today.toEpochDay()] ?: 0,
            currentStreakDays = streakCalculator.currentStreak(studyDays, today),
            lastSevenDays = lastSevenDays,
        )
    }

    companion object {
        /** Share of correct answers, or null when nothing has been answered yet. */
        fun accuracy(correct: Int, total: Int): Double? {
            require(total >= 0 && correct in 0..total) { "Correct answers must be between 0 and the total" }
            return if (total == 0) null else correct.toDouble() / total
        }
    }
}
