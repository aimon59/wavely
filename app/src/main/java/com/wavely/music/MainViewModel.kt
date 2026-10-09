package com.wavely.music

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.wavely.music.online.JamendoSource
import com.wavely.music.online.MusicSource
import com.wavely.music.online.OnlineTrack
import com.wavely.music.online.toMediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<OnlineTrack> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

data class NowPlaying(
    val hasMedia: Boolean = false,
    val mediaId: String = "",
    val title: String = "",
    val artist: String = "",
    val artwork: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

@OptIn(FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val source: MusicSource = JamendoSource(BuildConfig.JAMENDO_CLIENT_ID)

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    private val _now = MutableStateFlow(NowPlaying())
    val now: StateFlow<NowPlaying> = _now.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateNow()
        override fun onPlayerError(error: PlaybackException) {
            _search.update { it.copy(error = "Couldn't play this track.") }
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val f = MediaController.Builder(app, token).buildAsync()
        future = f
        f.addListener({
            try {
                controller = f.get().also { it.addListener(listener) }
                updateNow()
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            queryFlow.debounce(400).collectLatest { load(it) }
        }
        viewModelScope.launch {
            while (true) {
                delay(500)
                if (controller?.playWhenReady == true) updateNow()
            }
        }
    }

    fun onQueryChange(q: String) {
        _search.update { it.copy(query = q) }
        queryFlow.value = q
    }

    private suspend fun load(q: String) {
        if (BuildConfig.JAMENDO_CLIENT_ID.isBlank()) {
            _search.update { it.copy(error = "Missing Jamendo client ID (see README).", loading = false) }
            return
        }
        _search.update { it.copy(loading = true, error = null) }
        try {
            val list = if (q.isBlank()) source.trending() else source.search(q)
            _search.update { it.copy(results = list, loading = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _search.update { it.copy(loading = false, error = "Couldn't load songs. Check your connection.") }
        }
    }

    fun playFrom(index: Int) {
        val c = controller ?: return
        val tracks = _search.value.results
        if (index !in tracks.indices) return
        c.setMediaItems(tracks.map { it.toMediaItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        controller?.let { if (it.playWhenReady) it.pause() else it.play() }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    private fun updateNow() {
        val c = controller ?: return
        val item = c.currentMediaItem
        val md = item?.mediaMetadata
        _now.value = NowPlaying(
            hasMedia = item != null,
            mediaId = item?.mediaId.orEmpty(),
            title = md?.title?.toString().orEmpty(),
            artist = md?.artist?.toString().orEmpty(),
            artwork = md?.artworkUri?.toString(),
            isPlaying = c.playWhenReady && c.playbackState != Player.STATE_ENDED,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.coerceAtLeast(0L),
        )
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
