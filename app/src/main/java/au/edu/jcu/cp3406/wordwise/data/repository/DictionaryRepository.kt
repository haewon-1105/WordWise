package au.edu.jcu.cp3406.wordwise.data.repository

import au.edu.jcu.cp3406.wordwise.data.local.dao.WordDao
import au.edu.jcu.cp3406.wordwise.data.local.entity.WordEntity
import au.edu.jcu.cp3406.wordwise.data.remote.DictionaryApi
import au.edu.jcu.cp3406.wordwise.model.Word
import au.edu.jcu.cp3406.wordwise.model.WordSource
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryDefinition
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryEntry
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryMeaning
import au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryResponseMapper
import au.edu.jcu.cp3406.wordwise.model.dictionary.LookupResult
import au.edu.jcu.cp3406.wordwise.model.dictionary.WordInputValidator
import au.edu.jcu.cp3406.wordwise.util.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AddWordResult {
    data class Added(val wordId: Long) : AddWordResult
    data object AlreadyInCollection : AddWordResult
}

/** Looks words up online and adds the learner's chosen meaning to their collection. */
interface DictionaryRepository {

    suspend fun lookUp(rawInput: String): LookupResult

    suspend fun addToCollection(
        entry: DictionaryEntry,
        meaning: DictionaryMeaning,
        definition: DictionaryDefinition,
    ): AddWordResult

    /**
     * Returns [word] with its phonetic spelling and audio link, fetching them once from the
     * dictionary and caching them in Room. Offline or on failure the word is returned as it is.
     */
    suspend fun withPronunciation(word: Word): Word
}

@Singleton
class NetworkDictionaryRepository @Inject constructor(
    private val api: DictionaryApi,
    private val wordDao: WordDao,
    private val timeProvider: TimeProvider,
) : DictionaryRepository {

    override suspend fun lookUp(rawInput: String): LookupResult {
        val term = WordInputValidator.normalize(rawInput) ?: return LookupResult.InvalidInput
        return try {
            withContext(Dispatchers.IO) {
                val response = api.lookUp(term)
                val body = if (response.isSuccessful) {
                    response.body()?.use { it.string() }
                } else {
                    response.errorBody()?.close()
                    null
                }
                DictionaryResponseMapper.map(response.code(), body)
            }
        } catch (e: IOException) {
            // No connection, timeout or interrupted download.
            LookupResult.NoConnection
        }
    }

    override suspend fun addToCollection(
        entry: DictionaryEntry,
        meaning: DictionaryMeaning,
        definition: DictionaryDefinition,
    ): AddWordResult {
        if (wordDao.findByTerm(entry.term) != null) return AddWordResult.AlreadyInCollection

        val id = wordDao.insert(
            WordEntity(
                term = entry.term,
                partOfSpeech = meaning.partOfSpeech,
                definition = definition.definition,
                example = definition.example,
                // The learner's own words are always included, whatever difficulty is set.
                level = 1,
                source = WordSource.USER,
                phonetic = entry.phonetic,
                audioUrl = entry.audioUrl,
                createdEpochDay = timeProvider.today().toEpochDay(),
            ),
        )
        // Room returns -1 when the unique term index made it ignore the insert.
        return if (id == -1L) AddWordResult.AlreadyInCollection else AddWordResult.Added(id)
    }

    override suspend fun withPronunciation(word: Word): Word {
        if (word.phonetic != null || word.audioUrl != null) return word

        val result = lookUp(word.term) as? LookupResult.Found ?: return word
        val entry = result.entry
        if (entry.phonetic == null && entry.audioUrl == null) return word

        wordDao.updatePronunciation(word.id, entry.phonetic, entry.audioUrl)
        return word.copy(phonetic = entry.phonetic, audioUrl = entry.audioUrl)
    }
}
