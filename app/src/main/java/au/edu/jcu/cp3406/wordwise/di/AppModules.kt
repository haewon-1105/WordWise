package au.edu.jcu.cp3406.wordwise.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import au.edu.jcu.cp3406.wordwise.data.deck.AssetDeckSource
import au.edu.jcu.cp3406.wordwise.data.deck.DeckSource
import au.edu.jcu.cp3406.wordwise.data.local.WordWiseDatabase
import au.edu.jcu.cp3406.wordwise.data.local.dao.StatsDao
import au.edu.jcu.cp3406.wordwise.data.local.dao.StudyDao
import au.edu.jcu.cp3406.wordwise.data.local.dao.WordDao
import au.edu.jcu.cp3406.wordwise.data.repository.DataStoreSettingsRepository
import au.edu.jcu.cp3406.wordwise.data.repository.DictionaryRepository
import au.edu.jcu.cp3406.wordwise.data.repository.NetworkDictionaryRepository
import au.edu.jcu.cp3406.wordwise.data.repository.RoomStudyRepository
import au.edu.jcu.cp3406.wordwise.data.repository.SettingsRepository
import au.edu.jcu.cp3406.wordwise.data.repository.StudyRepository
import au.edu.jcu.cp3406.wordwise.model.LearningStatsCalculator
import au.edu.jcu.cp3406.wordwise.model.LeitnerScheduler
import au.edu.jcu.cp3406.wordwise.util.DefaultPronunciationPlayer
import au.edu.jcu.cp3406.wordwise.util.PronunciationPlayer
import au.edu.jcu.cp3406.wordwise.util.SystemTimeProvider
import au.edu.jcu.cp3406.wordwise.util.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Room database and its DAOs. One database instance for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WordWiseDatabase =
        Room.databaseBuilder(context, WordWiseDatabase::class.java, WordWiseDatabase.NAME).build()

    @Provides
    fun provideWordDao(database: WordWiseDatabase): WordDao = database.wordDao()

    @Provides
    fun provideStudyDao(database: WordWiseDatabase): StudyDao = database.studyDao()

    @Provides
    fun provideStatsDao(database: WordWiseDatabase): StatsDao = database.statsDao()

    /** Preferences file for Settings; one instance per process, as DataStore requires. */
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
}

/** Binds interfaces to their production implementations. Tests can swap these out. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindStudyRepository(impl: RoomStudyRepository): StudyRepository

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindDictionaryRepository(impl: NetworkDictionaryRepository): DictionaryRepository

    @Binds
    abstract fun bindDeckSource(impl: AssetDeckSource): DeckSource

    @Binds
    abstract fun bindPronunciationPlayer(impl: DefaultPronunciationPlayer): PronunciationPlayer

    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}

/** Pure Kotlin model classes, provided here so the model package stays free of Android and DI. */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    fun provideLeitnerScheduler(): LeitnerScheduler = LeitnerScheduler()

    @Provides
    fun provideLearningStatsCalculator(): LearningStatsCalculator = LearningStatsCalculator()
}
