package com.wavely.music.online

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String,
    val streamUrl: String,
)

/** Implement this for other providers (e.g. Audius) later. */
interface MusicSource {
    suspend fun search(query: String, page: Int = 0): List<OnlineTrack>
    suspend fun trending(): List<OnlineTrack>
}

fun OnlineTrack.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId("jamendo:$id")
        .setUri(streamUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(coverUrl.takeIf { it.isNotBlank() }?.toUri())
                .build()
        )
        .build()

class JamendoSource(
    private val clientId: String,
    private val api: JamendoApi = JamendoApi.create(),
    private val pageSize: Int = 30,
) : MusicSource {

    override suspend fun search(query: String, page: Int): List<OnlineTrack> =
        api.searchTracks(clientId = clientId, query = query.trim(), limit = pageSize, offset = page * pageSize)
            .results.mapNotNull { it.toDomain() }

    override suspend fun trending(): List<OnlineTrack> =
        api.popularTracks(clientId = clientId, limit = pageSize)
            .results.mapNotNull { it.toDomain() }

    private fun JamendoTrack.toDomain(): OnlineTrack? {
        if (audio.isBlank()) return null
        return OnlineTrack(
            id = id,
            title = name,
            artist = artistName,
            album = albumName,
            durationMs = duration * 1000L,
            coverUrl = image,
            streamUrl = audio,
        )
    }
}
