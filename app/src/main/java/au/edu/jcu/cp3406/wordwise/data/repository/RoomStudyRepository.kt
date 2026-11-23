package au.edu.jcu.cp3406.wordwise.data.repository

import au.edu.jcu.cp3406.wordwise.data.deck.DeckSource
import au.edu.jcu.cp3406.wordwise.data.local.dao.StatsDao
import au.edu.jcu.cp3406.wordwise.data.local.dao.StudyDao
import au.edu.jcu.cp3406.wordwise.data.local.dao.WordDao
import au.edu.jcu.cp3406.wordwise.data.local.entity.ReviewLogEntity
import au.edu.jcu.cp3406.wordwise.data.local.toEntity
import au.edu.jcu.cp3406.wordwise.data.local.toModel
import au.edu.jcu.cp3406.wordwise.model.CardProgress
import au.edu.jcu.cp3406.wordwise.model.DailyReviewCount
import au.edu.jcu.cp3406.wordwise.model.Difficulty
import au.edu.jcu.cp3406.wordwise.model.Flashcard
import au.edu.jcu.cp3406.wordwise.model.LearningStats
import au.edu.jcu.cp3406.wordwise.model.LearningStatsCalculator
import au.edu.jcu.cp3406.wordwise.model.LeitnerScheduler
import au.edu.jcu.cp3406.wordwise.model.SessionPlanner
import au.edu.jcu.cp3406.wordwise.model.WordSource
import au.edu.jcu.cp3406.wordwise.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** [StudyRepository] backed by the Room database. All data stays on the device. */
@Singleton
class RoomStudyRepository @Inject constructor(
    private val wordDao: WordDao,
    private val studyDao: StudyDao,
    private val statsDao: StatsDao,
    private val deckSource: DeckSource,
    private val timeProvider: TimeProvider,
    private val scheduler: LeitnerScheduler,
    private val statsCalculator: LearningStatsCalculator,
) : StudyRepository {

    // Stops two screens from seeding the deck at the same time on first launch.
    private val seedMutex = Mutex()

    override suspend fun ensureDeckLoaded() {
        seedMutex.withLock {
            if (wordDao.countBySource(WordSource.DECK) > 0) return
            val today = timeProvider.today().toEpochDay()
            wordDao.insertAll(deckSource.load().map { it.toEntity(today) })
        }
    }

    override suspend fun todaysSession(dailyGoal: Int, difficulty: Difficulty): List<Flashcard> {
        val today = timeProvider.today().toEpochDay()
        val reviewedToday = studyDao.reviewCountOn(today)
        val remaining = (dailyGoal - reviewedToday).coerceAtLeast(0)
        if (remaining == 0) return emptyList()

        val due = wordDao.getDueCards(today, difficulty.maxLevel, remaining).map { it.toModel() }
        val new = wordDao.getNewCards(difficulty.maxLevel, remaining).map { it.toModel() }
        return SessionPlanner.plan(due, new, dailyGoal, reviewedToday)
    }

    override suspend fun recordAnswer(card: Flashcard, correct: Boolean): CardProgress {
        val today = timeProvider.today()
        val current = studyDao.getProgress(card.word.id)?.toModel()
        val updated = scheduler.review(card.word.id, current, correct, today)
        studyDao.recordReview(
            progress = updated.toEntity(),
            review = ReviewLogEntity(
                wordId = card.word.id,
                epochDay = today.toEpochDay(),
                correct = correct,
                boxAfter = updated.box,
            ),
        )
        return updated
    }

    override fun observeReviewedToday(): Flow<Int> =
        studyDao.observeReviewCountOn(timeProvider.today().toEpochDay())

    override fun observeStats(): Flow<LearningStats> {
        val today = timeProvider.today()
        return combine(
            wordDao.observeWordCount(),
            statsDao.observeBoxCounts(),
            statsDao.observeTotals(),
            statsDao.observeDailyCounts(fromEpochDay = today.minusDays(6).toEpochDay()),
            statsDao.observeStudyDays(),
        ) { wordCount, boxCounts, totals, dailyCounts, studyDays ->
            statsCalculator.calculate(
                totalWords = wordCount,
                boxCounts = boxCounts.associate { it.box to it.count },
                totalReviews = totals.total,
                correctReviews = totals.correct,
                dailyCounts = dailyCounts.map { DailyReviewCount(it.epochDay, it.count) },
                studyDays = studyDays.map(LocalDate::ofEpochDay).toSet(),
                today = today,
            )
        }
    }
}
