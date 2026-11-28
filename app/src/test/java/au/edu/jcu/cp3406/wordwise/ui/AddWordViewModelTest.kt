package au.edu.jcu.cp3406.wordwise.ui

import au.edu.jcu.cp3406.wordwise.data.repository.AddWordResult
import au.edu.jcu.cp3406.wordwise.model.dictionary.LookupResult
import au.edu.jcu.cp3406.wordwise.ui.addword.AddWordMessage
import au.edu.jcu.cp3406.wordwise.ui.addword.AddWordViewModel
import au.edu.jcu.cp3406.wordwise.ui.addword.LookupStatus
import au.edu.jcu.cp3406.wordwise.util.PlaybackSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddWordViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeDictionaryRepository()
    private val player = FakePronunciationPlayer()
    private val vm = AddWordViewModel(repository, player)

    private fun searchFor(query: String) {
        vm.onQueryChange(query)
        vm.search()
    }

    @Test
    fun foundWordIsShown() = runTest {
        repository.lookupResult = LookupResult.Found(sampleEntry())

        searchFor("significant")

        assertEquals(listOf("significant"), repository.lookups)
        assertEquals(LookupStatus.Found(sampleEntry()), vm.uiState.value.status)
        assertFalse(vm.uiState.value.isInCollection)
    }

    @Test
    fun everyFailureHasItsOwnState() = runTest {
        val cases = mapOf(
            LookupResult.NotFound to LookupStatus.NotFound,
            LookupResult.InvalidInput to LookupStatus.InvalidInput,
            LookupResult.NoConnection to LookupStatus.NoConnection,
            LookupResult.ServiceError(503) to LookupStatus.ServiceError,
        )
        cases.forEach { (result, expected) ->
            repository.lookupResult = result
            searchFor("anything")
            assertEquals(expected, vm.uiState.value.status)
        }
    }

    @Test
    fun addingTheChosenMeaningConfirmsIt() = runTest {
        repository.lookupResult = LookupResult.Found(sampleEntry())
        searchFor("significant")

        vm.addToCollection(sampleMeaning, sampleDefinition)

        assertEquals(listOf(sampleDefinition.definition), repository.added)
        assertTrue(vm.uiState.value.isInCollection)
        assertEquals(AddWordMessage.ADDED, vm.uiState.value.message)

        vm.onMessageShown()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun duplicateWordIsReportedNotAddedTwice() = runTest {
        repository.lookupResult = LookupResult.Found(sampleEntry())
        repository.addResult = AddWordResult.AlreadyInCollection
        searchFor("significant")

        vm.addToCollection(sampleMeaning, sampleDefinition)

        assertEquals(AddWordMessage.ALREADY_IN_COLLECTION, vm.uiState.value.message)
    }

    @Test
    fun addingWithoutAResultDoesNothing() = runTest {
        vm.addToCollection(sampleMeaning, sampleDefinition)

        assertTrue(repository.added.isEmpty())
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun learnerIsToldWhenTheDeviceVoiceReplacesARecording() = runTest {
        repository.lookupResult = LookupResult.Found(sampleEntry())
        searchFor("significant")
        player.result = PlaybackSource.DEVICE_VOICE

        vm.playPronunciation()

        assertEquals(AddWordMessage.DEVICE_VOICE_USED, vm.uiState.value.message)
    }

    @Test
    fun recordingThatPlaysNeedsNoMessage() = runTest {
        repository.lookupResult = LookupResult.Found(sampleEntry())
        searchFor("significant")

        vm.playPronunciation()

        assertEquals("significant", player.played.single().first)
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun failedPlaybackIsExplained() = runTest {
        repository.lookupResult = LookupResult.Found(sampleEntry(audioUrl = null))
        searchFor("significant")
        player.result = null

        vm.playPronunciation()

        assertEquals(AddWordMessage.AUDIO_FAILED, vm.uiState.value.message)
    }
}
