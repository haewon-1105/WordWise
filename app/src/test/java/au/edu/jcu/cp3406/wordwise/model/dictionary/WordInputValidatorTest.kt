package au.edu.jcu.cp3406.wordwise.model.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordInputValidatorTest {

    @Test
    fun wordIsTrimmedAndLowercased() {
        assertEquals("hypothesis", WordInputValidator.normalize("  Hypothesis "))
    }

    @Test
    fun shortPhrasesHyphensAndApostrophesAreAllowed() {
        assertEquals("well-being", WordInputValidator.normalize("Well-being"))
        assertEquals("ad hoc", WordInputValidator.normalize("ad   hoc"))
        assertEquals("o'clock", WordInputValidator.normalize("o’clock"))
    }

    @Test
    fun emptyOrBlankInputIsRejected() {
        assertNull(WordInputValidator.normalize(""))
        assertNull(WordInputValidator.normalize("   "))
    }

    @Test
    fun digitsSymbolsAndEmailsAreRejected() {
        assertNull(WordInputValidator.normalize("word2"))
        assertNull(WordInputValidator.normalize("haewon@example.com"))
        assertNull(WordInputValidator.normalize("what?"))
        assertNull(WordInputValidator.normalize("-start"))
        assertNull(WordInputValidator.normalize("end-"))
        assertNull(WordInputValidator.normalize("double--hyphen"))
    }

    @Test
    fun inputLongerThanTheLimitIsRejected() {
        val tooLong = "a".repeat(WordInputValidator.MAX_LENGTH + 1)

        assertNull(WordInputValidator.normalize(tooLong))
        assertEquals("a".repeat(40), WordInputValidator.normalize("a".repeat(40)))
    }
}
