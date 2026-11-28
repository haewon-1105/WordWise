package au.edu.jcu.cp3406.wordwise.ui

import au.edu.jcu.cp3406.wordwise.data.repository.AddWordResult
import au.edu.jcu.cp3406.wordwise.data.repository.DictionaryRepository
import au.edu.jcu.cp3406.wordwise.data.repository.SettingsRepository
import au.edu.jcu.cp3406.wordwise.data.repository.StudyRepository
import au.edu.jcu.cp3406.wordwise.model.CardProgress
import au.edu.jcu.cp3406.wordwise.model.DailyReviewCount
import au.edu.jcu.cp3406.wordwise.model.Difficulty
import au.edu.jcu.cp3406.wordwise.model.Flashcard
import au.edu.jcu.cp3406.wordwise.model.LearningStats
import au.edu.jcu.cp3406.wordwise.model.UserSettings
import au.edu.jcu.cp3406.wordwise.model.Word
import au.edu.jcu.cp3406.wordwise.model.WordSource
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryDefinition
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryEntry
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryMeaning
import au.edu.jcu.cp3406.wordwise.model.dictionary.LookupResult
import au.edu.jcu.cp3406.wordwise.util.PlaybackSource
import au.edu.jcu.cp3406.wordwise.util.PronunciationPlayer
import au.edu.jcu.cp3406.wordwise.util.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException
import java.time.LocalDate

/*
 * Hand-written fakes for the repository interfaces. ViewModels only depend on these
 * interfaces, so their logic can be tested on the JVM without Room, DataStore or the network.
 */

/** Runs viewModelScope on a test dispatcher, so coroutines finish before assertions. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

val TODAY: LocalDate = LocalDate.of(2026, 11, 28)

class FixedTimeProvider(private val date: LocalDate = TODAY) : TimeProvider {
    override fun today(): LocalDate = date
}

fun testWord(id: Long, term: String = "word$id", audioUrl: String? = null) = Word(
    id = id,
    term = term,
    partOfSpeech = "noun",
    definition = "Definition of $term.",
    example = null,
    level = 1,
    source = WordSource.DECK,
    audioUrl = audioUrl,
)

fun testCard(id: Long) = Flashcard(word = testWord(id), progress = null)

fun emptyStats(totalWords: Int = 0, wordsMastered: Int = 0, reviewsToday: Int = 0) = LearningStats(
    totalWords = totalWords,
    wordsStarted = 0,
    wordsMastered = wordsMastered,
    wordsPerBox = List(5) { 0 },
    totalReviews = 0,
    correctReviews = 0,
    accuracy = null,
    reviewsToday = reviewsToday,
    currentStreakDays = 0,
    lastSevenDays = (6 downTo 0).map { DailyReviewCount(TODAY.minusDays(it.toLong()).toEpochDay(), 0) },
)

class FakeStudyRepository(
    var cards: List<Flashcard> = emptyList(),
    reviewedToday: Int = 0,
) : StudyRepository {
    var failToLoad = false
    var resetCalls = 0
    val answers = mutableListOf<Pair<Long, Boolean>>()
    val sessionRequests = mutableListOf<Pair<Int, Difficulty>>()

    private val reviewed = MutableStateFlow(reviewedToday)
    val stats = MutableStateFlow(emptyStats(reviewsToday = reviewedToday))

    override suspend fun ensureDeckLoaded() {
        if (failToLoad) throw IOException("deck missing")
    }

    override suspend fun todaysSession(dailyGoal: Int, difficulty: Difficulty): List<Flashcard> {
        sessionRequests += dailyGoal to difficulty
        return cards
    }

    /** Mirrors the Leitner rule closely enough for ViewModel tests: up one box or back to 1. */
    override suspend fun recordAnswer(card: Flashcard, correct: Boolean): CardProgress {
        answers += card.word.id to correct
        reviewed.update { it + 1 }
        val box = if (correct) (card.progress?.box ?: 1) + 1 else 1
        return CardProgress(
            wordId = card.word.id,
            box = box,
            dueEpochDay = TODAY.toEpochDay() + if (correct) 2 else 1,
            correctCount = if (correct) 1 else 0,
            incorrectCount = if (correct) 0 else 1,
            lastReviewedEpochDay = TODAY.toEpochDay(),
        )
    }

    override fun observeReviewedToday(): Flow<Int> = reviewed

    override fun observeStats(): Flow<LearningStats> = stats

    override suspend fun resetProgress() {
        resetCalls++
        reviewed.value = 0
    }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state.map { it }

    override suspend fun setDailyGoal(goal: Int) {
        state.update { it.copy(dailyGoal = UserSettings.sanitizeDailyGoal(goal)) }
    }

    override suspend fun setDifficulty(difficulty: Difficulty) {
        state.update { it.copy(difficulty = difficulty) }
    }

    override suspend fun setReadWordsAloud(enabled: Boolean) {
        state.update { it.copy(readWordsAloud = enabled) }
    }
}

class FakeDictionaryRepository : DictionaryRepository {
    var lookupResult: LookupResult = LookupResult.NotFound
    var addResult: AddWordResult = AddWordResult.Added(99)
    var pronunciationUrl: String? = null
    val added = mutableListOf<String>()
    val lookups = mutableListOf<String>()

    override suspend fun lookUp(rawInput: String): LookupResult {
        lookups += rawInput
        return lookupResult
    }

    override suspend fun addToCollection(
        entry: DictionaryEntry,
        meaning: DictionaryMeaning,
        definition: DictionaryDefinition,
    ): AddWordResult {
        added += definition.definition
        return addResult
    }

    override suspend fun withPronunciation(word: Word): Word =
        if (word.audioUrl == null && pronunciationUrl != null) word.copy(audioUrl = pronunciationUrl) else word
}

class FakePronunciationPlayer : PronunciationPlayer {
    var result: PlaybackSource? = PlaybackSource.RECORDING
    val played = mutableListOf<Pair<String, String?>>()
    var released = false

    override suspend fun play(term: String, recordingUrl: String?): PlaybackSource? {
        played += term to recordingUrl
        return result
    }

    override fun release() {
        released = true
    }
}

val sampleDefinition = DictionaryDefinition("Important or noticeable.", "A significant change.")
val sampleMeaning = DictionaryMeaning("adjective", listOf(sampleDefinition))

fun sampleEntry(audioUrl: String? = "https://example.org/significant.mp3") = DictionaryEntry(
    term = "significant",
    phonetic = "/sɪɡˈnɪfɪkənt/",
    audioUrl = audioUrl,
    meanings = listOf(sampleMeaning),
    sourceUrl = "https://en.wiktionary.org/wiki/significant",
)
