package au.edu.jcu.cp3406.wordwise.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import au.edu.jcu.cp3406.wordwise.data.local.dao.StatsDao
import au.edu.jcu.cp3406.wordwise.data.local.dao.StudyDao
import au.edu.jcu.cp3406.wordwise.data.local.dao.WordDao
import au.edu.jcu.cp3406.wordwise.data.local.entity.CardProgressEntity
import au.edu.jcu.cp3406.wordwise.data.local.entity.ReviewLogEntity
import au.edu.jcu.cp3406.wordwise.data.local.entity.WordEntity
import au.edu.jcu.cp3406.wordwise.model.WordSource

@Database(
    entities = [WordEntity::class, CardProgressEntity::class, ReviewLogEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WordWiseDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun studyDao(): StudyDao
    abstract fun statsDao(): StatsDao

    companion object {
        const val NAME = "wordwise.db"
    }
}

/** Stores enums by name so the database stays readable and safe if the enum order changes. */
class Converters {
    @TypeConverter
    fun fromWordSource(source: WordSource): String = source.name

    @TypeConverter
    fun toWordSource(value: String): WordSource = WordSource.valueOf(value)
}
