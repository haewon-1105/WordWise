package au.edu.jcu.cp3406.wordwise.di

import au.edu.jcu.cp3406.wordwise.data.remote.DictionaryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * HTTP client and Retrofit service. No logging interceptor and no extra headers are added,
 * so requests carry nothing about the learner beyond the word being looked up.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(DictionaryApi.BASE_URL)
        .client(client)
        .build()

    @Provides
    @Singleton
    fun provideDictionaryApi(retrofit: Retrofit): DictionaryApi =
        retrofit.create(DictionaryApi::class.java)
}
