package au.edu.jcu.cp3406.wordwise.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for the Free Dictionary API (https://dictionaryapi.dev/), a free,
 * key-less service whose definitions come from Wiktionary.
 *
 * The raw body is returned so [au.edu.jcu.cp3406.wordwise.model.dictionary.DictionaryResponseParser]
 * can handle the API's irregular JSON and every HTTP status in plain, unit-tested Kotlin.
 * Only the looked-up word is sent: no account, identifier or learning history.
 */
interface DictionaryApi {

    @GET("api/v2/entries/en/{word}")
    suspend fun lookUp(@Path("word") word: String): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://api.dictionaryapi.dev/"
    }
}
