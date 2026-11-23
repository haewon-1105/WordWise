package au.edu.jcu.cp3406.wordwise.data.repository

import au.edu.jcu.cp3406.wordwise.model.CardProgress
import au.edu.jcu.cp3406.wordwise.model.Difficulty
import au.edu.jcu.cp3406.wordwise.model.Flashcard
import au.edu.jcu.cp3406.wordwise.model.LearningStats
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for study data. ViewModels depend on this interface only, so they can be
 * tested with a fake and never touch Room directly.
 */
interface StudyRepository {

    /** Copies the bundled academic deck into the database the first time the app runs. */
    suspend fun ensureDeckLoaded()

    /** Cards for today, limited by what is left of the learner's daily goal. */
    suspend fun todaysSession(dailyGoal: Int, difficulty: Difficulty): List<Flashcard>

    /** Applies the Leitner rules to an answer and saves it; returns the card's new state. */
    suspend fun recordAnswer(card: Flashcard, correct: Boolean): CardProgress

    fun observeReviewedToday(): Flow<Int>

    fun observeStats(): Flow<LearningStats>
}
