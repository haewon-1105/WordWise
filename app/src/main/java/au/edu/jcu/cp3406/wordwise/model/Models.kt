package au.edu.jcu.cp3406.wordwise.model

/*
 * Pure Kotlin domain models. Nothing in the model package depends on Android, Room or Hilt,
 * so all of its logic can be verified with plain JVM unit tests.
 */

/** Where a word came from: the built-in academic deck or the learner's own search. */
enum class WordSource { DECK, USER }

/**
 * Difficulty chosen in Settings. Each deck word has a level from 1 to 3, and a difficulty
 * includes every level up to [maxLevel].
 */
enum class Difficulty(val maxLevel: Int) {
    EASY(1),
    MEDIUM(2),
    HARD(3),
}

data class Word(
    val id: Long,
    val term: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val level: Int,
    val source: WordSource,
    val phonetic: String? = null,
    val audioUrl: String? = null,
)

/**
 * Spaced-repetition state of one word. Days are stored as epoch days
 * (java.time.LocalDate.toEpochDay()) so they compare and persist easily.
 */
data class CardProgress(
    val wordId: Long,
    val box: Int,
    val dueEpochDay: Long,
    val correctCount: Int,
    val incorrectCount: Int,
    val lastReviewedEpochDay: Long?,
)

/** A word as shown in a study session. [progress] is null for a word never studied before. */
data class Flashcard(
    val word: Word,
    val progress: CardProgress?,
) {
    val isNew: Boolean get() = progress == null
}

/** Number of reviews made on one day, used for the Stats screen's weekly chart. */
data class DailyReviewCount(
    val epochDay: Long,
    val count: Int,
)

/** Everything the Stats screen shows, built by [LearningStatsCalculator]. */
data class LearningStats(
    val totalWords: Int,
    val wordsStarted: Int,
    val wordsMastered: Int,
    /** Number of words in each Leitner box, index 0 = box 1. */
    val wordsPerBox: List<Int>,
    val totalReviews: Int,
    val correctReviews: Int,
    /** Share of correct answers from 0.0 to 1.0, or null before the first review. */
    val accuracy: Double?,
    val reviewsToday: Int,
    val currentStreakDays: Int,
    /** Seven entries, oldest first, ending today. Days without reviews have a count of 0. */
    val lastSevenDays: List<DailyReviewCount>,
)
