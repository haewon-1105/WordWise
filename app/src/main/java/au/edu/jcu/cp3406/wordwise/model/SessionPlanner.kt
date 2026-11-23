package au.edu.jcu.cp3406.wordwise.model

/**
 * Builds today's study session from the learner's own daily goal.
 *
 * Due reviews come first, because forgetting them undoes earlier work; new words fill any
 * remaining space. Once the goal is met the session is empty and the Study screen shows
 * "Done for today" instead of offering more cards (an ethical choice from Assessment 2:
 * the learner, not the app, decides how much is enough).
 */
object SessionPlanner {

    /**
     * @param dueCards cards whose review date has arrived, most overdue first.
     * @param newCards cards never studied before, in deck order.
     * @param dailyGoal number of cards the learner chose to study per day.
     * @param reviewedToday answers already given today.
     */
    fun plan(
        dueCards: List<Flashcard>,
        newCards: List<Flashcard>,
        dailyGoal: Int,
        reviewedToday: Int,
    ): List<Flashcard> {
        require(dailyGoal >= 0) { "The daily goal cannot be negative" }
        val remaining = (dailyGoal - reviewedToday).coerceAtLeast(0)
        return (dueCards + newCards)
            .distinctBy { it.word.id }
            .take(remaining)
    }

    /** True when the learner has met their goal for today. */
    fun isGoalMet(dailyGoal: Int, reviewedToday: Int): Boolean = reviewedToday >= dailyGoal
}
