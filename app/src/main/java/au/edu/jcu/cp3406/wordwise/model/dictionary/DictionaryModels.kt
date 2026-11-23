package au.edu.jcu.cp3406.wordwise.model.dictionary

/* Results from the Free Dictionary API, reduced to what the app shows. Pure Kotlin. */

data class DictionaryDefinition(
    val definition: String,
    val example: String?,
)

data class DictionaryMeaning(
    val partOfSpeech: String,
    val definitions: List<DictionaryDefinition>,
)

data class DictionaryEntry(
    val term: String,
    val phonetic: String?,
    val audioUrl: String?,
    val meanings: List<DictionaryMeaning>,
    /** Wiktionary page the definitions come from, shown as attribution. */
    val sourceUrl: String?,
)

/** Every outcome of a lookup, so the UI can explain each case instead of failing silently. */
sealed interface LookupResult {
    data class Found(val entry: DictionaryEntry) : LookupResult
    data object NotFound : LookupResult
    data object InvalidInput : LookupResult
    data object NoConnection : LookupResult
    data class ServiceError(val httpCode: Int) : LookupResult
}
