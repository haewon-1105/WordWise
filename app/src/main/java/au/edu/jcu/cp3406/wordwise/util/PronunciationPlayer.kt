package au.edu.jcu.cp3406.wordwise.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** How a pronunciation was played, so the UI can be honest about it. */
enum class PlaybackSource {
    /** A human recording from the dictionary. */
    RECORDING,

    /** The device's built-in text-to-speech voice, used when no recording is available. */
    DEVICE_VOICE,
}

/** Plays a word's pronunciation. An interface so ViewModel tests can use a fake. */
interface PronunciationPlayer {
    /** Returns how the word was played, or null if neither a recording nor the device voice worked. */
    suspend fun play(term: String, recordingUrl: String?): PlaybackSource?
    fun release()
}

/**
 * Plays the dictionary's recording when there is one, and otherwise falls back to the
 * device's own text-to-speech voice.
 *
 * Recordings are downloaded with the app's OkHttp client, kept in the cache folder and played
 * from the local file, so a clip heard once replays offline. The Free Dictionary API sometimes
 * lists recordings that no longer exist (HTTP 404); those are remembered for the session and
 * the device voice is used instead. Text-to-speech runs on the device, so nothing extra is sent
 * anywhere. Audio only plays when the learner taps the play button.
 *
 * One shared instance for the whole app, so only one speech engine is ever started.
 */
@Singleton
class DefaultPronunciationPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) : PronunciationPlayer {

    private var player: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private val unavailableRecordings = mutableSetOf<String>()
    private val engineLock = Mutex()

    override suspend fun play(term: String, recordingUrl: String?): PlaybackSource? {
        if (recordingUrl != null && recordingUrl !in unavailableRecordings) {
            if (playRecording(recordingUrl)) return PlaybackSource.RECORDING
            unavailableRecordings += recordingUrl
        }
        return if (speak(term)) PlaybackSource.DEVICE_VOICE else null
    }

    override fun release() {
        player?.release()
        player = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    // --- Dictionary recording -------------------------------------------------------------

    private suspend fun playRecording(url: String): Boolean = try {
        val file = withContext(Dispatchers.IO) {
            cacheFileFor(url).also { if (!it.exists()) download(url, it) }
        }
        withContext(Dispatchers.Main) { startPlayback(file) }
        true
    } catch (e: IOException) {
        Log.w(TAG, "Recording unavailable at $url", e)
        false
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Invalid recording source $url", e)
        false
    } catch (e: IllegalStateException) {
        Log.w(TAG, "MediaPlayer was in an invalid state", e)
        false
    }

    private fun cacheFileFor(url: String): File {
        val folder = File(context.cacheDir, CACHE_FOLDER).apply { mkdirs() }
        val name = url.substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(MAX_NAME_LENGTH)
        // The hash keeps names unique when two URLs end in the same file name.
        return File(folder, "${url.hashCode().toUInt()}_$name")
    }

    private fun download(url: String, target: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response")
            val partial = File(target.path + ".part")
            partial.outputStream().use { output -> body.byteStream().copyTo(output) }
            // Rename only when complete, so an interrupted download is never played.
            if (!partial.renameTo(target)) throw IOException("Could not save the clip")
        }
    }

    private fun startPlayback(file: File) {
        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            setOnCompletionListener { finished ->
                finished.release()
                if (player === finished) player = null
            }
            setDataSource(file.absolutePath)
            prepare() // A short local file, so preparing synchronously is quick.
            start()
        }
    }

    // --- Device voice ------------------------------------------------------------------------

    private suspend fun speak(term: String): Boolean {
        // The lock stops two quick taps from starting two engines at once.
        val engine = engineLock.withLock {
            textToSpeech ?: withContext(Dispatchers.Main) { createTextToSpeech() }
        } ?: return false
        return withContext(Dispatchers.Main) {
            val result = engine.speak(term, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
            Log.d(TAG, "Device voice asked to say \"$term\" (result $result)")
            result == TextToSpeech.SUCCESS
        }
    }

    /** Starts the device's speech engine with an English voice, or returns null if there is none. */
    private suspend fun createTextToSpeech(): TextToSpeech? = suspendCancellableCoroutine { continuation ->
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            val locale = if (status == TextToSpeech.SUCCESS) {
                PREFERRED_LOCALES.firstOrNull {
                    engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE
                }
            } else {
                null
            }
            if (locale == null) {
                Log.w(TAG, "No English text-to-speech voice available (status $status)")
                engine.shutdown()
                continuation.resume(null)
            } else {
                engine.language = locale
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build(),
                )
                engine.setOnUtteranceProgressListener(SpeechLogger)
                val features = engine.voice?.features.orEmpty()
                if (TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED in features) {
                    Log.w(TAG, "English voice data for $locale is not downloaded on this device yet")
                }
                Log.d(TAG, "Device voice ready: $locale, voice ${engine.voice?.name}")
                textToSpeech = engine
                continuation.resume(engine)
            }
        }
    }

    /** Logs what the speech engine actually did, so silent failures show up in Logcat. */
    private object SpeechLogger : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            Log.d(TAG, "Device voice started speaking")
        }

        override fun onDone(utteranceId: String?) {
            Log.d(TAG, "Device voice finished")
        }

        @Deprecated("Required override for older API levels")
        override fun onError(utteranceId: String?) {
            Log.w(TAG, "Device voice failed")
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            Log.w(TAG, "Device voice failed with error $errorCode")
        }
    }

    private companion object {
        const val TAG = "PronunciationPlayer"
        const val CACHE_FOLDER = "pronunciations"
        const val MAX_NAME_LENGTH = 80
        const val UTTERANCE_ID = "wordwise-pronunciation"

        // Australian English first for JCU learners, then other common English voices.
        val PREFERRED_LOCALES = listOf(Locale("en", "AU"), Locale.UK, Locale.US)
    }
}
