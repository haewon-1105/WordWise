package au.edu.jcu.cp3406.wordwise.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import au.edu.jcu.cp3406.wordwise.model.WordSource

/** A word in the learner's collection, from the bundled deck or added by the learner. */
@Entity(
    tableName = "words",
    indices = [
        Index(value = ["term"], unique = true),
        Index(value = ["level"]),
    ],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    @ColumnInfo(name = "part_of_speech") val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val level: Int,
    val source: WordSource,
    // Filled from the Free Dictionary API in Step 3; included now to avoid a migration.
    val phonetic: String? = null,
    @ColumnInfo(name = "audio_url") val audioUrl: String? = null,
    @ColumnInfo(name = "created_epoch_day") val createdEpochDay: Long,
)

/** Leitner state of one word. A row exists only once the word has been studied. */
@Entity(
    tableName = "card_progress",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["due_epoch_day"])],
)
data class CardProgressEntity(
    @PrimaryKey @ColumnInfo(name = "word_id") val wordId: Long,
    val box: Int,
    @ColumnInfo(name = "due_epoch_day") val dueEpochDay: Long,
    @ColumnInfo(name = "correct_count") val correctCount: Int,
    @ColumnInfo(name = "incorrect_count") val incorrectCount: Int,
    @ColumnInfo(name = "last_reviewed_epoch_day") val lastReviewedEpochDay: Long?,
)

/**
 * One answer given in a study session. The log is the source of the Stats screen's history,
 * accuracy and streak, and it never leaves the device.
 */
@Entity(
    tableName = "review_log",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["word_id"]), Index(value = ["epoch_day"])],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    val correct: Boolean,
    @ColumnInfo(name = "box_after") val boxAfter: Int,
)

/** A word together with its progress row, if it has one (one-to-one Room relation). */
data class WordWithProgress(
    @Embedded val word: WordEntity,
    @Relation(parentColumn = "id", entityColumn = "word_id")
    val progress: CardProgressEntity?,
)

/** Result rows for aggregate statistics queries. */
data class BoxCount(val box: Int, val count: Int)

data class ReviewTotals(val total: Int, val correct: Int)

data class DayCount(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    val count: Int,
)
