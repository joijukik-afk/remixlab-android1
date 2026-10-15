package com.joijukik.remixlab

import android.app.Application
import android.content.ContentUris
import android.media.audiofx.PresetReverb
import android.media.projection.MediaProjection
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    val player: ExoPlayer = ExoPlayer.Builder(app).build()

    // ============ STATE ============
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSpeed = MutableStateFlow(1.0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed

    private val _currentPitch = MutableStateFlow(1.0f)
    val currentPitch: StateFlow<Float> = _currentPitch

    private val _isSlowed = MutableStateFlow(false)
    val isSlowed: StateFlow<Boolean> = _isSlowed

    private val _isReverbEnabled = MutableStateFlow(false)
    val isReverbEnabled: StateFlow<Boolean> = _isReverbEnabled

    private val _isBassBoostEnabled = MutableStateFlow(false)
    val isBassBoostEnabled: StateFlow<Boolean> = _isBassBoostEnabled

    private val _is8DEnabled = MutableStateFlow(false)
    val is8DEnabled: StateFlow<Boolean> = _is8DEnabled

    private val _isDynamicsEnabled = MutableStateFlow(false)
    val isDynamicsEnabled: StateFlow<Boolean> = _isDynamicsEnabled

    private val _songName = MutableStateFlow("هیچ آهنگی انتخاب نشده")
    val songName: StateFlow<String> = _songName

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    // Export / Recording
    private val _exportQuality = MutableStateFlow(320)
    val exportQuality: StateFlow<Int> = _exportQuality

    private val _outputFileName = MutableStateFlow("remix_output")
    val outputFileName: StateFlow<String> = _outputFileName

    private val _outputTitle = MutableStateFlow("RemixLab Track")
    val outputTitle: StateFlow<String> = _outputTitle

    private val _outputArtist = MutableStateFlow("RemixLab")
    val outputArtist: StateFlow<String> = _outputArtist

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isRecordingPaused = MutableStateFlow(false)
    val isRecordingPaused: StateFlow<Boolean> = _isRecordingPaused

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds

    private val _lastSavedUri = MutableStateFlow<String?>(null)
    val lastSavedUri: StateFlow<String?> = _lastSavedUri

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<SongInfo>>(emptyList())
    val searchResults: StateFlow<List<SongInfo>> = _searchResults

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation

    private var progressJob: Job? = null
    private var recordJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0)
                }
                if (state == Player.STATE_ENDED) {
                    _isPlaying.value = false
                }
            }
        })

        progressJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = player.currentPosition
                _duration.value = player.duration.coerceAtLeast(0)
                delay(400)
            }
        }
    }

    // ============ پخش ============
    fun loadSong(uri: Uri, title: String? = null) {
        val item = MediaItem.fromUri(uri)
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
        _songName.value = title ?: uri.lastPathSegment ?: "آهنگ ناشناخته"
    }

    fun playPause() {
        if (player.isPlaying) player.pause()
        else if (player.mediaItemCount > 0) player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    // ============ سرعت و پیچ ============
    fun setCustomSpeed(speed: Float) {
        _currentSpeed.value = speed
        player.playbackParameters = PlaybackParameters(speed, _currentPitch.value)
        _isSlowed.value = speed < 0.95f
    }

    fun setPitch(pitch: Float) {
        _currentPitch.value = pitch
        player.playbackParameters = PlaybackParameters(_currentSpeed.value, pitch)
    }

    fun toggleSlowed() {
        _isSlowed.value = !_isSlowed.value
        setCustomSpeed(if (_isSlowed.value) 0.8f else 1.0f)
    }

    // ============ افکت‌ها (فعلاً فقط state) ============
    fun toggleReverb() { _isReverbEnabled.value = !_isReverbEnabled.value }
    fun toggleBassBoost() { _isBassBoostEnabled.value = !_isBassBoostEnabled.value }
    fun toggle8D() { _is8DEnabled.value = !_is8DEnabled.value }
    fun toggleDynamics() { _isDynamicsEnabled.value = !_isDynamicsEnabled.value }

    // ============ پریست‌ها ============
    fun applyComaPreset(preset: com.joijukik.remixlab.model.ComaPreset) {
        setCustomSpeed(preset.speed)
        setPitch(preset.pitch)
        _isReverbEnabled.value = preset.reverbPreset != null
        _isBassBoostEnabled.value = preset.bassBoost
        _is8DEnabled.value = preset.virtualizer
        _isDynamicsEnabled.value = preset.dynamics
    }

    // ============ AI (ساده و بدون API) ============
    fun applyAiTuning(prompt: String) {
        // تحلیل ساده‌ی متن ورودی
        val lower = prompt.lowercase()
        if (lower.contains("بارون") || lower.contains("rain")) {
            setCustomSpeed(0.85f)
            setPitch(0.95f)
            _isReverbEnabled.value = true
        } else if (lower.contains("شب") || lower.contains("night")) {
            setCustomSpeed(0.8f)
            _isReverbEnabled.value = true
            _isBassBoostEnabled.value = true
        } else if (lower.contains("تند") || lower.contains("nightcore")) {
            setCustomSpeed(1.25f)
            setPitch(1.15f)
        } else if (lower.contains("غرق") || lower.contains("deep")) {
            setCustomSpeed(0.7f)
            _isBassBoostEnabled.value = true
            _isDynamicsEnabled.value = true
        }
        _aiExplanation.value = "تنظیمات بر اساس: $prompt"
    }

    fun clearAiExplanation() { _aiExplanation.value = null }

    // ============ ضبط ============
    fun startRecording(projection: MediaProjection) {
        _isRecording.value = true
        _isRecordingPaused.value = false
        _recordingSeconds.value = 0
        _lastSavedUri.value = null
        _recordingError.value = null

        recordJob = viewModelScope.launch {
            var sec = 0
            while (_isRecording.value) {
                delay(1000)
                if (!_isRecordingPaused.value) {
                    sec++
                    _recordingSeconds.value = sec
                }
            }
        }
    }

    fun pauseRecording() { _isRecordingPaused.value = true }

    fun resumeRecording() { _isRecordingPaused.value = false }

    fun stopRecording() {
        _isRecording.value = false
        _isRecordingPaused.value = false
        recordJob?.cancel()
        // TODO: اینجا اکسپورت واقعی وصل شود
        _lastSavedUri.value = "/storage/emulated/0/Music/RemixLab/output.mp3"
    }

    fun clearRecordingError() { _recordingError.value = null }

    // ============ Export Settings ============
    fun setExportQuality(quality: Int) { _exportQuality.value = quality }
    fun setOutputFileName(name: String) { _outputFileName.value = name }
    fun setOutputTitle(title: String) { _outputTitle.value = title }
    fun setOutputArtist(artist: String) { _outputArtist.value = artist }

    // ============ جستجو ============
    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
        if (q.length >= 2) searchSongs(q) else _searchResults.value = emptyList()
    }

    private fun searchSongs(query: String) {
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) { queryMediaStore(query) }
            _searchResults.value = results
        }
    }

    private fun queryMediaStore(query: String): List<SongInfo> {
        val list = mutableListOf<SongInfo>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.TITLE} LIKE ? OR " +
                "${MediaStore.Audio.Media.ARTIST} LIKE ?"
        val args = arrayOf("%$query%", "%$query%")

        try {
            getApplication<Application>().contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, args,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext() && list.size < 100) {
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    list += SongInfo(
                        uri = uri,
                        title = c.getString(titleCol) ?: "ناشناخته",
                        artist = c.getString(artistCol) ?: "نامشخص",
                        durationMs = c.getLong(durCol)
                    )
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        recordJob?.cancel()
        player.release()
    }
}

data class SongInfo(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long
)
