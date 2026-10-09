package com.wavely.music.online

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class JamendoResponse(val results: List<JamendoTrack> = emptyList())

@Serializable
data class JamendoTrack(
    val id: String,
    val name: String = "",
    @SerialName("artist_name") val artistName: String = "",
    @SerialName("album_name") val albumName: String = "",
    val duration: Int = 0,
    val image: String = "",
    val audio: String = "",
)

interface JamendoApi {
    @GET("tracks/")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("search") query: String,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("imagesize") imageSize: Int = 300,
        @Query("format") format: String = "json",
    ): JamendoResponse

    @GET("tracks/")
    suspend fun popularTracks(
        @Query("client_id") clientId: String,
        @Query("order") order: String = "popularity_week",
        @Query("limit") limit: Int = 30,
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("imagesize") imageSize: Int = 300,
        @Query("format") format: String = "json",
    ): JamendoResponse

    companion object {
        fun create(): JamendoApi {
            val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl("https://api.jamendo.com/v3.0/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(JamendoApi::class.java)
        }
    }
}
