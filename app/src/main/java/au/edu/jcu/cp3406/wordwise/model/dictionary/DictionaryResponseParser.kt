package au.edu.jcu.cp3406.wordwise.model.dictionary

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Turns a Free Dictionary API response into a [DictionaryEntry].
 *
 * The API returns an array of entries for one word, and fields such as `phonetic`, `audio`
 * and `example` are often missing or empty. The parser merges the entries, keeps the first
 * usable phonetic and audio file, drops empty definitions and caps how much is shown so a
 * result stays readable on a phone.
 */
object DictionaryResponseParser {

    const val MAX_MEANINGS = 4
    const val MAX_DEFINITIONS_PER_MEANING = 3

    private val json = Json { ignoreUnknownKeys = true }

    /** Returns null when the response has no usable definitions. */
    fun parse(text: String): DictionaryEntry? {
        val entries = (json.parseToJsonElement(text) as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            .orEmpty()
        if (entries.isEmpty()) return null

        val term = entries.firstNotNullOfOrNull { it.string("word") } ?: return null
        val phonetics = entries.flatMap { it.objects("phonetics") }

        // Prefer the spelling recorded with the audio clip, so what the learner reads matches
        // what they hear; fragments such as "/-əsəs/" (a plural ending) are never used.
        val withAudio = phonetics.firstOrNull { p ->
            p.string("audio")?.let(::normalizeUrl) != null
        }
        val audioUrl = withAudio?.string("audio")?.let(::normalizeUrl)
        val phonetic = withAudio?.string("text")?.takeIf(::isFullPronunciation)
            ?: entries.firstNotNullOfOrNull { it.string("phonetic")?.takeIf(::isFullPronunciation) }
            ?: phonetics.firstNotNullOfOrNull { it.string("text")?.takeIf(::isFullPronunciation) }

        val meanings = entries
            .flatMap { it.objects("meanings") }
            .mapNotNull(::toMeaning)
            .groupBy { it.partOfSpeech }
            .map { (partOfSpeech, group) ->
                DictionaryMeaning(
                    partOfSpeech = partOfSpeech,
                    definitions = group.flatMap { it.definitions }
                        .distinctBy { it.definition }
                        .take(MAX_DEFINITIONS_PER_MEANING),
                )
            }
            .take(MAX_MEANINGS)
        if (meanings.isEmpty()) return null

        val sourceUrl = entries
            .flatMap { it.strings("sourceUrls") }
            .firstOrNull()
            ?.let(::normalizeUrl)

        return DictionaryEntry(term, phonetic, audioUrl, meanings, sourceUrl)
    }

    private fun toMeaning(obj: JsonObject): DictionaryMeaning? {
        val partOfSpeech = obj.string("partOfSpeech") ?: return null
        val definitions = obj.objects("definitions").mapNotNull { def ->
            val text = def.string("definition") ?: return@mapNotNull null
            DictionaryDefinition(definition = text, example = def.string("example"))
        }
        return if (definitions.isEmpty()) null else DictionaryMeaning(partOfSpeech, definitions)
    }

    /** False for partial pronunciations such as "/-əsəs/" or "-ses". */
    internal fun isFullPronunciation(text: String): Boolean =
        !text.removePrefix("/").startsWith("-")

    /** Audio links sometimes start with "//"; only https links are kept. */
    internal fun normalizeUrl(url: String): String? {
        val fixed = if (url.startsWith("//")) "https:$url" else url
        return fixed.takeIf { it.startsWith("https://") }
    }

    /** Non-blank string value of [key], trimmed, or null. */
    private fun JsonObject.string(key: String): String? =
        ((this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun JsonObject.objects(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
}

/**
 * Maps an HTTP status and body to a [LookupResult]. Kept separate from Retrofit so every
 * case can be unit tested without a network.
 */
object DictionaryResponseMapper {

    fun map(httpCode: Int, body: String?): LookupResult = when (httpCode) {
        200 -> try {
            body?.let(DictionaryResponseParser::parse)
                ?.let { LookupResult.Found(it) }
                ?: LookupResult.NotFound
        } catch (e: IllegalArgumentException) {
            // Malformed JSON (kotlinx SerializationException is an IllegalArgumentException).
            LookupResult.ServiceError(httpCode)
        }
        404 -> LookupResult.NotFound
        else -> LookupResult.ServiceError(httpCode)
    }
}
