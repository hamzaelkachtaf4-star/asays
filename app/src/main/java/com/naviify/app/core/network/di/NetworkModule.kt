package com.naviify.app.core.network.di

import android.util.Log
import com.naviify.app.BuildConfig
import com.naviify.app.core.network.DataStoreCookieJar
import com.naviify.app.core.network.DynamicServerInterceptor
import com.naviify.app.core.network.SubsonicAuthInterceptor
import com.naviify.app.core.network.SubsonicService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: DataStoreCookieJar,
        authInterceptor: SubsonicAuthInterceptor,
        serverInterceptor: DynamicServerInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        // The server interceptor rewrites the throwaway Retrofit base URL onto
        // the configured Navidrome host. Running it first means the auth
        // interceptor always sees the real destination host and can refuse to
        // attach credentials to anything else.
        .addInterceptor(serverInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(SanitizingLogInterceptor())
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            // Throwaway base URL; DynamicServerInterceptor rewrites it per request.
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideSubsonicService(retrofit: Retrofit): SubsonicService =
        retrofit.create(SubsonicService::class.java)

    /**
     * Debug-only request logging that strips the auth token/salt from the URL
     * so credentials never land in logcat.
     */
    private class SanitizingLogInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "-> ${request.method} ${sanitize(request.url)}")
            }
            val response = chain.proceed(request)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "<- ${response.code} ${sanitize(request.url)}")
            }
            return response
        }
    }

    private const val TAG = "Naviify"

    private fun sanitize(url: HttpUrl): String {
        val builder = url.newBuilder()
        builder.removeAllQueryParameters("t")
        builder.removeAllQueryParameters("s")
        return builder.build().toString()
    }
}
