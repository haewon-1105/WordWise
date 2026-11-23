package au.edu.jcu.cp3406.wordwise.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One word as written in the bundled deck file, before it gets a database id. */
data class DeckEntry(
    val term: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val level: Int,
)

/**
 * Reads the bundled deck file (assets/academic_deck.json).
 *
 * Expected shape: {"words": [{"term", "partOfSpeech", "definition", "example", "level"}, ...]}.
 * Entries with a missing term or definition, or a level outside 1..3, are skipped rather than
 * crashing the app, and duplicate terms keep their first entry.
 */
object DeckParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<DeckEntry> {
        val root = json.parseToJsonElement(text).jsonObject
        val words = root["words"]?.jsonArray ?: return emptyList()
        return words
            .mapNotNull { element -> (element as? JsonObject)?.let(::toEntry) }
            .distinctBy { it.term.lowercase() }
    }

    private fun toEntry(obj: JsonObject): DeckEntry? {
        val term = obj.string("term")?.trim().orEmpty()
        val definition = obj.string("definition")?.trim().orEmpty()
        val level = runCatching { obj["level"]?.jsonPrimitive?.int }.getOrNull() ?: return null
        if (term.isEmpty() || definition.isEmpty() || level !in 1..3) return null
        return DeckEntry(
            term = term,
            partOfSpeech = obj.string("partOfSpeech")?.trim().orEmpty(),
            definition = definition,
            example = obj.string("example")?.trim()?.takeIf { it.isNotEmpty() },
            level = level,
        )
    }

    private fun JsonObject.string(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()
}
