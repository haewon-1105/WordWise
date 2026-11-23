package au.edu.jcu.cp3406.wordwise.model.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryResponseParserTest {

    // Shaped like a real Free Dictionary API response, including its quirks:
    // two entries, an empty audio link, a "//" audio link and a blank definition.
    private val sampleResponse = """
        [
          {
            "word": "analyse",
            "phonetics": [
              {"text": "/ˈænəlaɪz/", "audio": ""},
              {"text": "/ˈænəˌlaɪz/", "audio": "//api.dictionaryapi.dev/media/pronunciations/en/analyse-uk.mp3"}
            ],
            "meanings": [
              {
                "partOfSpeech": "verb",
                "definitions": [
                  {"definition": "To subject to analysis.", "example": "We analysed the samples."},
                  {"definition": "   "},
                  {"definition": "To examine in detail."}
                ]
              }
            ],
            "sourceUrls": ["https://en.wiktionary.org/wiki/analyse"]
          },
          {
            "word": "analyse",
            "phonetic": "/ˈæn.ə.laɪz/",
            "meanings": [
              {
                "partOfSpeech": "verb",
                "definitions": [
                  {"definition": "To examine in detail."},
                  {"definition": "To psychoanalyse."},
                  {"definition": "A fourth verb sense that should be cut."}
                ]
              },
              {
                "partOfSpeech": "noun",
                "definitions": [{"definition": "An analysis."}]
              }
            ]
          }
        ]
    """.trimIndent()

    @Test
    fun entriesAreMergedIntoOneResult() {
        val entry = DictionaryResponseParser.parse(sampleResponse)!!

        assertEquals("analyse", entry.term)
        assertEquals(listOf("verb", "noun"), entry.meanings.map { it.partOfSpeech })
        assertEquals("https://en.wiktionary.org/wiki/analyse", entry.sourceUrl)
    }

    @Test
    fun definitionsAreDeduplicatedBlankOnesDroppedAndCapped() {
        val verb = DictionaryResponseParser.parse(sampleResponse)!!.meanings.first()

        assertEquals(
            listOf("To subject to analysis.", "To examine in detail.", "To psychoanalyse."),
            verb.definitions.map { it.definition },
        )
        assertEquals("We analysed the samples.", verb.definitions.first().example)
        assertNull(verb.definitions[1].example)
    }

    @Test
    fun phoneticRecordedWithTheAudioIsPreferred() {
        val entry = DictionaryResponseParser.parse(sampleResponse)!!

        // Not the entry-level "/ˈæn.ə.laɪz/" and not the first, audio-less "/ˈænəlaɪz/".
        assertEquals("/ˈænəˌlaɪz/", entry.phonetic)
    }

    @Test
    fun partialPronunciationFragmentsAreSkipped() {
        // Shaped like the real response for "hypothesis", whose entry-level phonetic is a
        // fragment for the plural ending.
        val json = """
            [{"word": "hypothesis", "phonetic": "/-əsəs/",
              "phonetics": [{"text": "/-əsəs/", "audio": ""},
                            {"text": "/haɪˈpɒθəsɪs/", "audio": ""}],
              "meanings": [{"partOfSpeech": "noun", "definitions": [{"definition": "A guess."}]}]}]
        """.trimIndent()

        val entry = DictionaryResponseParser.parse(json)!!

        assertEquals("/haɪˈpɒθəsɪs/", entry.phonetic)
        assertFalse(DictionaryResponseParser.isFullPronunciation("-ses"))
        assertTrue(DictionaryResponseParser.isFullPronunciation("/haɪˈpɒθəsɪs/"))
    }

    @Test
    fun noUsablePhoneticGivesNull() {
        val json = """
            [{"word": "w", "phonetic": "/-əz/",
              "meanings": [{"partOfSpeech": "noun", "definitions": [{"definition": "d"}]}]}]
        """.trimIndent()

        assertNull(DictionaryResponseParser.parse(json)!!.phonetic)
    }

    @Test
    fun firstUsableAudioIsKeptAndProtocolRelativeLinkFixed() {
        val entry = DictionaryResponseParser.parse(sampleResponse)!!

        assertEquals(
            "https://api.dictionaryapi.dev/media/pronunciations/en/analyse-uk.mp3",
            entry.audioUrl,
        )
    }

    @Test
    fun phoneticFallsBackToPhoneticsTextAndMissingAudioIsNull() {
        val json = """
            [{"word": "data",
              "phonetics": [{"text": "/ˈdeɪtə/"}],
              "meanings": [{"partOfSpeech": "noun", "definitions": [{"definition": "Information."}]}]}]
        """.trimIndent()

        val entry = DictionaryResponseParser.parse(json)!!

        assertEquals("/ˈdeɪtə/", entry.phonetic)
        assertNull(entry.audioUrl)
        assertNull(entry.sourceUrl)
    }

    @Test
    fun responseWithoutDefinitionsGivesNull() {
        assertNull(DictionaryResponseParser.parse("""[{"word": "x", "meanings": []}]"""))
        assertNull(DictionaryResponseParser.parse("[]"))
        assertNull(DictionaryResponseParser.parse("""{"title": "No Definitions Found"}"""))
    }

    @Test
    fun meaningsAreCapped() {
        val meanings = (1..6).joinToString(",") {
            """{"partOfSpeech": "pos$it", "definitions": [{"definition": "d$it"}]}"""
        }
        val entry = DictionaryResponseParser.parse("""[{"word": "w", "meanings": [$meanings]}]""")!!

        assertEquals(DictionaryResponseParser.MAX_MEANINGS, entry.meanings.size)
    }

    @Test
    fun onlyHttpsLinksAreAccepted() {
        assertEquals("https://a.mp3", DictionaryResponseParser.normalizeUrl("//a.mp3"))
        assertEquals("https://a.mp3", DictionaryResponseParser.normalizeUrl("https://a.mp3"))
        assertNull(DictionaryResponseParser.normalizeUrl("http://a.mp3"))
        assertNull(DictionaryResponseParser.normalizeUrl("file:///a.mp3"))
    }

    @Test
    fun mapperTurnsStatusCodesIntoResults() {
        assertTrue(DictionaryResponseMapper.map(200, sampleResponse) is LookupResult.Found)
        assertEquals(LookupResult.NotFound, DictionaryResponseMapper.map(200, "[]"))
        assertEquals(LookupResult.NotFound, DictionaryResponseMapper.map(200, null))
        assertEquals(LookupResult.NotFound, DictionaryResponseMapper.map(404, null))
        assertEquals(LookupResult.ServiceError(429), DictionaryResponseMapper.map(429, null))
        assertEquals(LookupResult.ServiceError(503), DictionaryResponseMapper.map(503, null))
    }

    @Test
    fun malformedJsonIsAServiceErrorNotACrash() {
        assertEquals(LookupResult.ServiceError(200), DictionaryResponseMapper.map(200, "{not json"))
    }
}
