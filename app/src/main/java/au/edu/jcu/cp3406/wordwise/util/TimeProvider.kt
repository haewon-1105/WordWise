package au.edu.jcu.cp3406.wordwise.util

import java.time.LocalDate
import javax.inject.Inject

/** Source of "today", injected so tests can fix the date instead of depending on the clock. */
interface TimeProvider {
    fun today(): LocalDate
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun today(): LocalDate = LocalDate.now()
}
