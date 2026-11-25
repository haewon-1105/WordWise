package au.edu.jcu.cp3406.wordwise.model

/**
 * One study session as an immutable state machine: show a card, turn it over, answer, move on.
 * Kept free of Android so the rules are unit tested.
 *
 * The learner must turn a card over before answering, so every answer is given after seeing
 * the meaning (an honest self-check rather than a guess).
 */
data class StudySession(
    val cards: List<Flashcard>,
    val position: Int = 0,
    val correctCount: Int = 0,
    val isFlipped: Boolean = false,
) {
    val total: Int get() = cards.size
    val answeredCount: Int get() = position
    val current: Flashcard? get() = cards.getOrNull(position)
    val isFinished: Boolean get() = position >= cards.size

    /** Share of the session completed, from 0.0 to 1.0. */
    val progress: Float get() = if (total == 0) 1f else answeredCount.toFloat() / total

    fun flip(): StudySession = if (isFinished) this else copy(isFlipped = !isFlipped)

    fun answer(correct: Boolean): StudySession {
        check(!isFinished) { "The session is already finished" }
        check(isFlipped) { "Turn the card over before answering" }
        return copy(
            position = position + 1,
            correctCount = correctCount + if (correct) 1 else 0,
            isFlipped = false,
        )
    }

    /** Replaces the current card's word, e.g. once its pronunciation has been fetched. */
    fun updateCurrentWord(word: Word): StudySession {
        val card = current ?: return this
        if (card.word.id != word.id) return this
        return copy(cards = cards.toMutableList().apply { set(position, card.copy(word = word)) })
    }
}

/** What the learner is told after each answer, so the scheduling is never a black box. */
data class AnswerFeedback(
    val correct: Boolean,
    val box: Int,
    val daysUntilNextReview: Int,
) {
    companion object {
        fun from(correct: Boolean, progress: CardProgress, todayEpochDay: Long) = AnswerFeedback(
            correct = correct,
            box = progress.box,
            daysUntilNextReview = (progress.dueEpochDay - todayEpochDay).toInt().coerceAtLeast(0),
        )
    }
}

/** Preferences the learner controls in Settings. */
data class UserSettings(
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    /** Off by default: nothing plays unless the learner asks for it. */
    val readWordsAloud: Boolean = false,
) {
    companion object {
        const val DEFAULT_DAILY_GOAL = 10
        const val MIN_DAILY_GOAL = 5
        const val MAX_DAILY_GOAL = 30
        const val DAILY_GOAL_STEP = 5

        /** Keeps a stored or chosen goal inside the allowed range and on a step of 5. */
        fun sanitizeDailyGoal(goal: Int): Int {
            val clamped = goal.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)
            val stepped = ((clamped + DAILY_GOAL_STEP / 2) / DAILY_GOAL_STEP) * DAILY_GOAL_STEP
            return stepped.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)
        }

        /** Reads a stored difficulty name, falling back to the default for unknown values. */
        fun parseDifficulty(name: String?): Difficulty =
            Difficulty.entries.firstOrNull { it.name == name } ?: Difficulty.MEDIUM
    }
}
