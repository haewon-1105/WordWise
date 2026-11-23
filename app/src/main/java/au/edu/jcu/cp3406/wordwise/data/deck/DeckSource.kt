package au.edu.jcu.cp3406.wordwise.data.deck

import android.content.Context
import au.edu.jcu.cp3406.wordwise.model.DeckEntry
import au.edu.jcu.cp3406.wordwise.model.DeckParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Supplies the built-in word deck. An interface so tests can use a fake deck. */
interface DeckSource {
    suspend fun load(): List<DeckEntry>
}

/** Reads the deck bundled in the APK's assets folder, off the main thread. */
class AssetDeckSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeckSource {

    override suspend fun load(): List<DeckEntry> = withContext(Dispatchers.IO) {
        context.assets.open(FILE_NAME).bufferedReader().use { reader ->
            DeckParser.parse(reader.readText())
        }
    }

    companion object {
        const val FILE_NAME = "academic_deck.json"
    }
}
