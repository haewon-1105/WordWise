package au.edu.jcu.cp3406.wordwise.model.dictionary

/**
 * Cleans what the learner typed before it is sent to the dictionary.
 *
 * Only a single English word or short phrase is accepted (letters, with single spaces,
 * hyphens or apostrophes between them). Anything else is rejected locally, so typing a
 * sentence or personal details by mistake never leaves the device.
 */
object WordInputValidator {

    const val MAX_LENGTH = 40

    private val allowed = Regex("^[a-z]+([ '\\-][a-z]+)*$")

    /** Returns the normalised term, or null if the input cannot be looked up. */
    fun normalize(raw: String): String? {
        val cleaned = raw
            .trim()
            .replace('’', '\'')
            .replace(Regex("\\s+"), " ")
            .lowercase()
        if (cleaned.isEmpty() || cleaned.length > MAX_LENGTH) return null
        return cleaned.takeIf { allowed.matches(it) }
    }
}
