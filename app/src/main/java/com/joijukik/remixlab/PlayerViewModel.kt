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
    private val aiAssistant = AiTuningAssistant()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _songName = MutableStateFlow("هیچ آهنگی انتخاب نشده")
    val songName: StateFlow<String> = _songName

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

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

    private val _activeAmbient = MutableStateFlow<AmbientType?>(null)
    val activeAmbient: StateFlow<AmbientType?> = _activeAmbient

    private val _ambientVolume = MutableStateFlow(0.4f)
    val ambientVolume: StateFlow<Float> = _ambientVolume

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<SongInfo>>(emptyList())
    val searchResults: StateFlow<List<SongInfo>> = _searchResults

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading

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

        audioExporter.onProgress = { sec -> _recordingSeconds.value = sec }
        audioExporter.onFinish = { path ->
            _isRecording.value = false
            _lastSavedUri.value = path
        }
        audioExporter.onError = { err ->
            _isRecording.value = false
            _recordingError.value = err
        }
    }

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

    fun toggleReverb() {
        _isReverbEnabled.value = !_isReverbEnabled.value
        effectsManager?.setReverbEnabled(
            _isReverbEnabled.value,
            PresetReverb.PRESET_LARGEROOM
        )
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

    fun setAmbient(type: AmbientType?) {
        if (type == null) {
            ambientManager.stop()
            _activeAmbient.value = null
        } else {
            ambientManager.play(type, _ambientVolume.value)
            _activeAmbient.value = type
        }
    }

    fun setAmbientVolume(v: Float) {
        _ambientVolume.value = v
        ambientManager.setVolume(v)
    }

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

        _isDynamicsEnabled.value = preset.dynamics
        dynamicsManager?.setEnabled(preset.dynamics)

        if (preset.ambientSound != null) {
            setAmbientVolume(preset.ambientVolume)
            setAmbient(preset.ambientSound)
        } else {
            setAmbient(null)
        }
    }

    fun saveCurrentAsPreset(name: String, icon: String = "⭐") {
        val id = "custom_${System.currentTimeMillis()}"
        val preset = ComaPreset(
            id = id,
            name = name,
            description = "پریست سفارشی",
            speed = _currentSpeed.value,
            pitch = _currentPitch.value,
            reverbPreset = if (_isReverbEnabled.value) PresetReverb.PRESET_LARGEROOM else null,
            bassBoost = _isBassBoostEnabled.value,
            virtualizer = _is8DEnabled.value,
            dynamics = _isDynamicsEnabled.value,
            ambientSound = _activeAmbient.value,
            ambientVolume = _ambientVolume.value,
            icon = icon
        )
        presetStore.savePreset(preset)
    }

    fun applyAiTuning(result: Any) {
        when (result) {
            is String -> applyAiFromText(result)
            else -> applyAiFromReflection(result)
        }
    }

    private fun applyAiFromText(prompt: String) {
        val lower = prompt.lowercase()
        when {
            lower.contains("بارون") || lower.contains("rain") -> {
                setCustomSpeed(0.85f)
                setPitch(0.95f)
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
                setCustomSpeed(1.25f)
                setPitch(1.15f)
            }
            lower.contains("غرق") || lower.contains("deep") -> {
                setCustomSpeed(0.7f)
                _isBassBoostEnabled.value = true
                _isDynamicsEnabled.value = true
                effectsManager?.setBassBoostEnabled(true)
                dynamicsManager?.setEnabled(true)
            }
        }
        _aiExplanation.value = "تنظیمات بر اساس: $prompt"
    }

    private fun applyAiFromReflection(result: Any) {
        try {
            val cls = result.javaClass
            val speed = cls.getMethod("getSpeed").invoke(result) as? Float ?: 1.0f
            val pitch = cls.getMethod("getPitch").invoke(result) as? Float ?: 1.0f
            val bassBoost = cls.getMethod("getBassBoost").invoke(result) as? Boolean ?: false
            val virtualizer = cls.getMethod("getVirtualizer").invoke(result) as? Boolean ?: false
            val reverb = cls.getMethod("getReverb").invoke(result) as? Boolean ?: false
            val reverbPreset = cls.getMethod("getReverbPreset").invoke(result) as? Int ?: 3
            val dynamics = cls.getMethod("getDynamics").invoke(result) as? Boolean ?: false
            val explanation = cls.getMethod("getExplanation").invoke(result) as? String ?: ""

            setCustomSpeed(speed)
            setPitch(pitch)

            _isBassBoostEnabled.value = bassBoost
            effectsManager?.setBassBoostEnabled(bassBoost)

            _is8DEnabled.value = virtualizer
            effectsManager?.setVirtualizerEnabled(virtualizer)

            _isReverbEnabled.value = reverb
            if (reverb) effectsManager?.setReverbEnabled(true, reverbPreset.toShort())
            else effectsManager?.setReverbEnabled(false)

            _isDynamicsEnabled.value = dynamics
            dynamicsManager?.setEnabled(dynamics)

            _aiExplanation.value = explanation
        } catch (e: Exception) {
            _aiExplanation.value = "خطا: ${e.message}"
        }
    }

    fun clearAiExplanation() { _aiExplanation.value = null }

    fun setExportQuality(quality: Int) { _exportQuality.value = quality }
    fun setOutputFileName(name: String) { _outputFileName.value = name }
    fun setOutputTitle(title: String) { _outputTitle.value = title }
    fun setOutputArtist(artist: String) { _outputArtist.value = artist }

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
    fun clearLastSaved() { _lastSavedUri.value = null }

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        player.release()
        effectsManager?.release()
        dynamicsManager?.release()
        ambientManager.release()
        audioExporter.release()
    }
}

data class SongInfo(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long
)
