package au.edu.jcu.cp3406.wordwise.model

import java.time.LocalDate

/**
 * Leitner spaced-repetition scheduler.
 *
 * Every word sits in one of several boxes. A correct answer moves it up one box, so it comes
 * back after a longer gap; a wrong answer sends it back to box 1, so it comes back tomorrow.
 * Words are scheduled by what memory needs, not by what keeps the learner in the app.
 *
 * @param intervalsInDays days until the next review for each box; index 0 is box 1.
 */
class LeitnerScheduler(
    private val intervalsInDays: List<Int> = DEFAULT_INTERVALS,
) {
    init {
        require(intervalsInDays.isNotEmpty()) { "At least one box is needed" }
        require(intervalsInDays.all { it > 0 }) { "Every interval must be at least one day" }
    }

    val boxCount: Int get() = intervalsInDays.size

    /** Days until the next review for a word in [box] (1-based). */
    fun intervalFor(box: Int): Int {
        require(box in FIRST_BOX..boxCount) { "Box $box is outside 1..$boxCount" }
        return intervalsInDays[box - 1]
    }

    /** True when the word is in the last box, i.e. the learner has mastered it. */
    fun isMastered(box: Int): Boolean = box == boxCount

    /**
     * Returns the new progress after one answer.
     *
     * @param current the word's progress, or null if it has never been studied.
     */
    fun review(
        wordId: Long,
        current: CardProgress?,
        correct: Boolean,
        today: LocalDate,
    ): CardProgress {
        val currentBox = current?.box ?: FIRST_BOX
        val newBox = if (correct) minOf(currentBox + 1, boxCount) else FIRST_BOX
        return CardProgress(
            wordId = wordId,
            box = newBox,
            dueEpochDay = today.plusDays(intervalFor(newBox).toLong()).toEpochDay(),
            correctCount = (current?.correctCount ?: 0) + if (correct) 1 else 0,
            incorrectCount = (current?.incorrectCount ?: 0) + if (correct) 0 else 1,
            lastReviewedEpochDay = today.toEpochDay(),
        )
    }

    companion object {
        const val FIRST_BOX = 1

        /** Five boxes reviewed after 1, 2, 4, 8 and 16 days. */
        val DEFAULT_INTERVALS = listOf(1, 2, 4, 8, 16)
    }
}
