package au.edu.jcu.cp3406.wordwise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeckParserTest {

    @Test
    fun validEntriesAreParsed() {
        val json = """
            {"name": "Test", "words": [
              {"term": "analyse", "partOfSpeech": "verb", "definition": "examine in detail",
               "example": "We analyse the results.", "level": 3}
            ]}
        """.trimIndent()

        val entries = DeckParser.parse(json)

        assertEquals(1, entries.size)
        with(entries.first()) {
            assertEquals("analyse", term)
            assertEquals("verb", partOfSpeech)
            assertEquals("examine in detail", definition)
            assertEquals("We analyse the results.", example)
            assertEquals(3, level)
        }
    }

    @Test
    fun badEntriesAreSkippedInsteadOfCrashing() {
        val json = """
            {"words": [
              {"term": "", "definition": "no term", "level": 1},
              {"term": "nodef", "definition": "  ", "level": 1},
              {"term": "badlevel", "definition": "level too high", "level": 9},
              {"term": "nolevel", "definition": "missing level"},
              {"term": "textlevel", "definition": "level is text", "level": "two"},
              "not an object",
              {"term": "ok", "definition": "kept", "level": 2}
            ]}
        """.trimIndent()

        val entries = DeckParser.parse(json)

        assertEquals(listOf("ok"), entries.map { it.term })
    }

    @Test
    fun blankExampleBecomesNullAndTextIsTrimmed() {
        val json = """{"words": [{"term": " area ", "definition": " a region ", "example": " ", "level": 1}]}"""

        val entry = DeckParser.parse(json).single()

        assertEquals("area", entry.term)
        assertEquals("a region", entry.definition)
        assertNull(entry.example)
        assertEquals("", entry.partOfSpeech)
    }

    @Test
    fun duplicateTermsKeepTheFirstEntry() {
        val json = """
            {"words": [
              {"term": "Data", "definition": "first", "level": 1},
              {"term": "data", "definition": "second", "level": 2}
            ]}
        """.trimIndent()

        val entries = DeckParser.parse(json)

        assertEquals(1, entries.size)
        assertEquals("first", entries.single().definition)
    }

    @Test
    fun missingWordsArrayGivesAnEmptyDeck() {
        assertTrue(DeckParser.parse("""{"name": "empty"}""").isEmpty())
    }

    @Test
    fun bundledDeckIsValid() {
        // Unit tests run with the module directory as the working directory.
        val file = File("src/main/assets/academic_deck.json")
        val entries = DeckParser.parse(file.readText())

        assertEquals(60, entries.size)
        assertEquals(setOf(1, 2, 3), entries.map { it.level }.toSet())
        assertTrue(entries.all { it.example != null && it.partOfSpeech.isNotEmpty() })
    }
}
