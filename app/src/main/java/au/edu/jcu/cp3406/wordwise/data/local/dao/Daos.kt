package au.edu.jcu.cp3406.wordwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import au.edu.jcu.cp3406.wordwise.data.local.entity.BoxCount
import au.edu.jcu.cp3406.wordwise.data.local.entity.CardProgressEntity
import au.edu.jcu.cp3406.wordwise.data.local.entity.DayCount
import au.edu.jcu.cp3406.wordwise.data.local.entity.ReviewLogEntity
import au.edu.jcu.cp3406.wordwise.data.local.entity.ReviewTotals
import au.edu.jcu.cp3406.wordwise.data.local.entity.WordEntity
import au.edu.jcu.cp3406.wordwise.data.local.entity.WordWithProgress
import au.edu.jcu.cp3406.wordwise.model.WordSource
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    /** Words whose term already exists are skipped, so seeding twice is harmless. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity): Long

    @Query("SELECT COUNT(*) FROM words WHERE source = :source")
    suspend fun countBySource(source: WordSource): Int

    @Query("SELECT COUNT(*) FROM words")
    fun observeWordCount(): Flow<Int>

    @Query("SELECT * FROM words WHERE term = :term COLLATE NOCASE LIMIT 1")
    suspend fun findByTerm(term: String): WordEntity?

    /** Caches pronunciation fetched from the dictionary so it is downloaded only once. */
    @Query("UPDATE words SET phonetic = :phonetic, audio_url = :audioUrl WHERE id = :id")
    suspend fun updatePronunciation(id: Long, phonetic: String?, audioUrl: String?)

    /** Studied words whose review day has arrived, most overdue and weakest first. */
    @Transaction
    @Query(
        """
        SELECT words.* FROM words
        INNER JOIN card_progress ON card_progress.word_id = words.id
        WHERE card_progress.due_epoch_day <= :today AND words.level <= :maxLevel
        ORDER BY card_progress.due_epoch_day ASC, card_progress.box ASC
        LIMIT :limit
        """
    )
    suspend fun getDueCards(today: Long, maxLevel: Int, limit: Int): List<WordWithProgress>

    /** Words never studied; the learner's own words first, then the deck in level order. */
    @Transaction
    @Query(
        """
        SELECT * FROM words
        WHERE level <= :maxLevel AND id NOT IN (SELECT word_id FROM card_progress)
        ORDER BY CASE source WHEN 'USER' THEN 0 ELSE 1 END, level ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getNewCards(maxLevel: Int, limit: Int): List<WordWithProgress>
}

@Dao
abstract class StudyDao {

    @Query("SELECT * FROM card_progress WHERE word_id = :wordId")
    abstract suspend fun getProgress(wordId: Long): CardProgressEntity?

    @Upsert
    abstract suspend fun upsertProgress(progress: CardProgressEntity)

    @Insert
    abstract suspend fun insertReview(review: ReviewLogEntity): Long

    /** Saves the new Leitner state and the answer log together, or neither. */
    @Transaction
    open suspend fun recordReview(progress: CardProgressEntity, review: ReviewLogEntity) {
        upsertProgress(progress)
        insertReview(review)
    }

    @Query("DELETE FROM card_progress")
    abstract suspend fun deleteAllProgress()

    @Query("DELETE FROM review_log")
    abstract suspend fun deleteAllReviews()

    /** Erases learning history while keeping the learner's word collection. */
    @Transaction
    open suspend fun resetProgress() {
        deleteAllReviews()
        deleteAllProgress()
    }

    @Query("SELECT COUNT(*) FROM review_log WHERE epoch_day = :epochDay")
    abstract suspend fun reviewCountOn(epochDay: Long): Int

    @Query("SELECT COUNT(*) FROM review_log WHERE epoch_day = :epochDay")
    abstract fun observeReviewCountOn(epochDay: Long): Flow<Int>
}

@Dao
interface StatsDao {

    @Query("SELECT box, COUNT(*) AS count FROM card_progress GROUP BY box")
    fun observeBoxCounts(): Flow<List<BoxCount>>

    @Query("SELECT COUNT(*) AS total, COALESCE(SUM(correct), 0) AS correct FROM review_log")
    fun observeTotals(): Flow<ReviewTotals>

    @Query(
        """
        SELECT epoch_day, COUNT(*) AS count FROM review_log
        WHERE epoch_day >= :fromEpochDay
        GROUP BY epoch_day
        """
    )
    fun observeDailyCounts(fromEpochDay: Long): Flow<List<DayCount>>

    @Query("SELECT DISTINCT epoch_day FROM review_log ORDER BY epoch_day DESC")
    fun observeStudyDays(): Flow<List<Long>>
}
