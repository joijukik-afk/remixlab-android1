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
import com.joijukik.remixlab.data.CustomPresetStore
import com.joijukik.remixlab.data.MixManager
import com.joijukik.remixlab.data.SeparationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    val player: ExoPlayer = ExoPlayer.Builder(app).build()
    private var effectsManager: AudioEffectsManager? = null
    private var dynamicsManager: DynamicsProcessorManager? = null
    private val ambientManager = AmbientSoundManager()
    val audioExporter = AudioExporter(app)
    private val presetStore = CustomPresetStore(app)
    val mixManager = MixManager()
    val separationManager = SeparationManager(app)

    // ========== Playback State ==========
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _songName = MutableStateFlow("هیچ آهنگی انتخاب نشده")
    val songName: StateFlow<String> = _songName

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    // ========== Effects State ==========
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

    // ========== Export State ==========
    private val _exportQuality = MutableStateFlow(320)
    val exportQuality: StateFlow<Int> = _exportQuality

    private val _outputFileName = MutableStateFlow("remix_output")
    val outputFileName: StateFlow<String> = _outputFileName

    private val _outputTitle = MutableStateFlow("RemixLab Track")
    val outputTitle: StateFlow<String> = _outputTitle

    private val _outputArtist = MutableStateFlow("RemixLab")
    val outputArtist: StateFlow<String> = _outputArtist

    // ========== Recording State ==========
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

    // ========== Search State ==========
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<SongInfo>>(emptyList())
    val searchResults: StateFlow<List<SongInfo>> = _searchResults

    // ========== AI ==========
    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation

    // ========== Mix State ==========
    private val _mixTracks = MutableStateFlow<List<MixManager.MixTrack>>(emptyList())
    val mixTracks: StateFlow<List<MixManager.MixTrack>> = _mixTracks

    // ========== Separation State ==========
    private val _isSeparating = MutableStateFlow(false)
    val isSeparating: StateFlow<Boolean> = _isSeparating

    private val _separationProgress = MutableStateFlow(0f)
    val separationProgress: StateFlow<Float> = _separationProgress

    private val _activeStemFilter = MutableStateFlow(StemFilter.ALL)
    val activeStemFilter: StateFlow<StemFilter> = _activeStemFilter

    enum class StemFilter { ALL, VOCALS_ONLY, INSTRUMENTAL, NO_DRUMS, NO_BASS }

    private var progressJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                effectsManager?.release()
                dynamicsManager?.release()
                effectsManager = AudioEffectsManager(audioSessionId)
                dynamicsManager = DynamicsProcessorManager(audioSessionId)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                }
                if (state == Player.STATE_ENDED) _isPlaying.value = false
            }
        })

        progressJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = player.currentPosition
                _duration.value = player.duration.coerceAtLeast(0L)
                delay(400)
            }
        }

        audioExporter.onProgress = { sec -> _recordingSeconds.value = sec.toInt() }
        audioExporter.onFinish = { uri ->
            _isRecording.value = false
            _lastSavedUri.value = uri.toString()
        }
        audioExporter.onError = { err ->
            _isRecording.value = false
            _recordingError.value = err
        }
    }

    // ========== Playback ==========
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

    // ========== Speed & Pitch ==========
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

    // ========== Effects ==========
    fun toggleReverb() {
        _isReverbEnabled.value = !_isReverbEnabled.value
        effectsManager?.setReverbEnabled(_isReverbEnabled.value, PresetReverb.PRESET_LARGEROOM)
    }

    fun toggleBassBoost() {
        _isBassBoostEnabled.value = !_isBassBoostEnabled.value
        effectsManager?.setBassBoostEnabled(_isBassBoostEnabled.value)
    }

    fun toggle8D() {
        _is8DEnabled.value = !_is8DEnabled.value
        effectsManager?.setVirtualizerEnabled(_is8DEnabled.value)
    }

    fun toggleDynamics() {
        _isDynamicsEnabled.value = !_isDynamicsEnabled.value
        dynamicsManager?.setEnabled(_isDynamicsEnabled.value)
    }

    // ========== Presets ==========
    fun applyComaPreset(preset: ComaPreset) {
        setCustomSpeed(preset.speed)
        setPitch(preset.pitch)

        _isReverbEnabled.value = preset.reverbPreset != null
        preset.reverbPreset?.let {
            effectsManager?.setReverbEnabled(true, it)
        } ?: effectsManager?.setReverbEnabled(false)

        _isBassBoostEnabled.value = preset.bassBoost
        effectsManager?.setBassBoostEnabled(preset.bassBoost)

        _is8DEnabled.value = preset.virtualizer
        effectsManager?.setVirtualizerEnabled(preset.virtualizer)
    }

    fun saveCurrentAsPreset(name: String) {
        val id = "custom_${System.currentTimeMillis()}"
        val data = "speed=${_currentSpeed.value};pitch=${_currentPitch.value}"
        presetStore.savePreset(id, name, data)
    }

    // ========== AI ==========
    fun applyAiTuning(prompt: String) {
        val lower = prompt.lowercase()
        when {
            lower.contains("بارون") || lower.contains("rain") -> {
                setCustomSpeed(0.85f); setPitch(0.95f)
                _isReverbEnabled.value = true
                effectsManager?.setReverbEnabled(true, PresetReverb.PRESET_PLATE)
            }
            lower.contains("شب") || lower.contains("night") -> {
                setCustomSpeed(0.8f)
                _isReverbEnabled.value = true
                _isBassBoostEnabled.value = true
                effectsManager?.setReverbEnabled(true, PresetReverb.PRESET_LARGEROOM)
                effectsManager?.setBassBoostEnabled(true)
            }
            lower.contains("تند") || lower.contains("nightcore") -> {
                setCustomSpeed(1.25f); setPitch(1.15f)
            }
            lower.contains("غرق") || lower.contains("deep") -> {
                setCustomSpeed(0.7f)
                _isBassBoostEnabled.value = true
                effectsManager?.setBassBoostEnabled(true)
            }
        }
        _aiExplanation.value = "تنظیمات اعمال شد: $prompt"
    }

    fun clearAiExplanation() { _aiExplanation.value = null }

    // ========== Export Settings ==========
    fun setExportQuality(q: Int) { _exportQuality.value = q }
    fun setOutputFileName(n: String) { _outputFileName.value = n }
    fun setOutputTitle(t: String) { _outputTitle.value = t }
    fun setOutputArtist(a: String) { _outputArtist.value = a }

    // ========== Recording ==========
    fun startRecording(projection: MediaProjection) {
        _lastSavedUri.value = null
        _recordingError.value = null
        val ok = audioExporter.startRecording(projection)
        _isRecording.value = ok
        _isRecordingPaused.value = false
        _recordingSeconds.value = 0
    }

    fun pauseRecording() { _isRecordingPaused.value = true }
    fun resumeRecording() { _isRecordingPaused.value = false }
    fun stopRecording() {
        audioExporter.stopRecording()
        _isRecording.value = false
        _isRecordingPaused.value = false
    }
    fun clearRecordingError() { _recordingError.value = null }

    // ========== Search ==========
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
        val selection = "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?"
        val args = arrayOf("%$query%", "%$query%")
        try {
            getApplication<Application>().contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, args, null
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val tCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val aCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val dCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext() && list.size < 100) {
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    list += SongInfo(
                        uri = uri,
                        title = c.getString(tCol) ?: "ناشناخته",
                        artist = c.getString(aCol) ?: "نامشخص",
                        durationMs = c.getLong(dCol)
                    )
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    // ========== Mix ==========
    fun addTrackToMix(uri: Uri, trackId: String) {
        val mediaItem = MediaItem.fromUri(uri)
        mixManager.addTrack(mediaItem, trackId)
        _mixTracks.value = mixManager.getTracks()
        rebuildMixComposition()
    }

    fun removeTrackFromMix(trackId: String) {
        mixManager.removeTrack(trackId)
        _mixTracks.value = mixManager.getTracks()
        rebuildMixComposition()
    }

    fun setTrackVolume(trackId: String, volume: Float) {
        mixManager.setTrackVolume(trackId, volume)
        _mixTracks.value = mixManager.getTracks()
    }

    private fun rebuildMixComposition() {
        val composition = mixManager.buildComposition()
        // برای سادگی، فعلاً فقط track list رو ذخیره می‌کنیم
        // (پیاده‌سازی کامل CompositionPlayer نیاز به تغییر type player دارد)
    }

    // ========== Separation (شبیه‌سازی) ==========
    fun separateCurrentSong() {
        viewModelScope.launch {
            _isSeparating.value = true
            _separationProgress.value = 0f
            delay(500); _separationProgress.value = 0.3f
            delay(500); _separationProgress.value = 0.6f
            delay(500); _separationProgress.value = 0.9f
            delay(300); _separationProgress.value = 1f
            _isSeparating.value = false
        }
    }

    fun applyStemFilter(filter: StemFilter) {
        _activeStemFilter.value = filter
    }

    fun clearSeparation() {
        _activeStemFilter.value = StemFilter.ALL
    }

    // ========== Advanced Mix ==========
    fun setEqBand(band: Int, gainDb: Float) { effectsManager?.setPreEqBandGain(band, gainDb) }
    fun setCompressorThreshold(t: Float) { effectsManager?.setCompressorThreshold(0, t) }
    fun setCompressorRatio(r: Float) { effectsManager?.setCompressorRatio(0, r) }
    fun setLimiterEnabled(e: Boolean) { effectsManager?.setLimiterEnabled(e) }
    fun setLimiterThreshold(t: Float) { effectsManager?.setLimiterThreshold(t) }
    fun setInputGain(g: Float) { effectsManager?.setInputGain(g) }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        player.release()
        effectsManager?.release()
        dynamicsManager?.release()
        ambientManager.release()
        audioExporter.release()
        separationManager.release()
        mixManager.clear()
    }
}

data class SongInfo(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long
)
