package com.remixlab.app

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
import com.remixlab.app.ai.AiTuningAssistant
import com.remixlab.app.ai.TuningResult
import com.remixlab.app.audio.AmbientSoundManager
import com.remixlab.app.audio.AudioEffectsManager
import com.remixlab.app.audio.AudioExporter
import com.remixlab.app.audio.DynamicsProcessorManager
import com.remixlab.app.data.CustomPresetStore
import com.remixlab.app.model.AmbientType
import com.remixlab.app.model.ComaPreset
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

    // ============ State ============
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

    private val _currentReverbPreset = MutableStateFlow<Short>(PresetReverb.PRESET_LARGEROOM)
    val currentReverbPreset: StateFlow<Short> = _currentReverbPreset

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

    private val _currentSongTitle = MutableStateFlow("هیچ آهنگی انتخاب نشده")
    val currentSongTitle: StateFlow<String> = _currentSongTitle

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds

    private val _lastSavedPath = MutableStateFlow<String?>(null)
    val lastSavedPath: StateFlow<String?> = _lastSavedPath

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<SongInfo>>(emptyList())
    val searchResults: StateFlow<List<SongInfo>> = _searchResults

    private val _customPresets = MutableStateFlow<List<ComaPreset>>(emptyList())
    val customPresets: StateFlow<List<ComaPreset>> = _customPresets

    private val _aiResult = MutableStateFlow<TuningResult?>(null)
    val aiResult: StateFlow<TuningResult?> = _aiResult

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

        // آپدیت مداوم موقعیت پخش
        progressJob = viewModelScope.launch {
            while (true) {
                _currentPosition.value = player.currentPosition
                _duration.value = player.duration.coerceAtLeast(0)
                delay(400)
            }
        }

        // تنظیمات ضبط
        audioExporter.onProgress = { sec -> _recordingSeconds.value = sec }
        audioExporter.onFinish = { path ->
            _isRecording.value = false
            _lastSavedPath.value = path
        }
        audioExporter.onError = { err ->
            _isRecording.value = false
            _recordingError.value = err
        }

        reloadCustomPresets()
    }

    // ============ پخش ============
    fun loadSong(uri: Uri, title: String? = null) {
        val item = MediaItem.fromUri(uri)
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
        _currentSongTitle.value = title ?: uri.lastPathSegment ?: "آهنگ ناشناخته"
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.mediaItemCount == 0) return
            player.play()
        }
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

    // ============ افکت‌ها ============
    fun toggleReverb() {
        _isReverbEnabled.value = !_isReverbEnabled.value
        effectsManager?.setReverbEnabled(
            _isReverbEnabled.value,
            _currentReverbPreset.value
        )
    }

    fun setReverbPreset(preset: Short) {
        _currentReverbPreset.value = preset
        if (_isReverbEnabled.value) {
            effectsManager?.setReverbEnabled(true, preset)
        }
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

    // ============ صدای محیطی ============
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

    // ============ پریست‌ها ============
    fun applyComaPreset(preset: ComaPreset) {
        setCustomSpeed(preset.speed)
        setPitch(preset.pitch)

        _isReverbEnabled.value = preset.reverbPreset != null
        preset.reverbPreset?.let {
            _currentReverbPreset.value = it
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
            reverbPreset = if (_isReverbEnabled.value) _currentReverbPreset.value else null,
            bassBoost = _isBassBoostEnabled.value,
            virtualizer = _is8DEnabled.value,
            dynamics = _isDynamicsEnabled.value,
            ambientSound = _activeAmbient.value,
            ambientVolume = _ambientVolume.value,
            icon = icon
        )
        presetStore.savePreset(preset)
        reloadCustomPresets()
    }

    fun deleteCustomPreset(id: String) {
        presetStore.deletePreset(id)
        reloadCustomPresets()
    }

    private fun reloadCustomPresets() {
        _customPresets.value = presetStore.getAll()
    }

    // ============ ضبط ============
    fun startRecording(projection: MediaProjection) {
        _lastSavedPath.value = null
        _recordingError.value = null
        val ok = audioExporter.startRecording(projection)
        _isRecording.value = ok
        _recordingSeconds.value = 0
    }

    fun stopRecording() {
        audioExporter.stopRecording()
        _isRecording.value = false
    }

    fun clearRecordingError() { _recordingError.value = null }
    fun clearLastSaved() { _lastSavedPath.value = null }

    // ============ جستجو ============
    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
        if (q.length >= 2) searchSongs(q)
        else _searchResults.value = emptyList()
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
                "${MediaStore.Audio.Media.ARTIST} LIKE ? OR " +
                "${MediaStore.Audio.Media.ALBUM} LIKE ?"
        val args = arrayOf("%$query%", "%$query%", "%$query%")

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

    // ============ AI ============
    fun askAi(prompt: String) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiResult.value = null
            val result = aiAssistant.analyzeRequest(prompt)
            _aiResult.value = result
            _aiLoading.value = false

            if (result.explanation.isNotBlank() && !result.explanation.startsWith("خطا")) {
                setCustomSpeed(result.speed)
                setPitch(result.pitch)
                _isBassBoostEnabled.value = result.bassBoost
                effectsManager?.setBassBoostEnabled(result.bassBoost)
                _is8DEnabled.value = result.virtualizer
                effectsManager?.setVirtualizerEnabled(result.virtualizer)
                _isReverbEnabled.value = result.reverb
                if (result.reverb) {
                    _currentReverbPreset.value = result.reverbPreset.toShort()
                    effectsManager?.setReverbEnabled(true, result.reverbPreset.toShort())
                } else effectsManager?.setReverbEnabled(false)
                _isDynamicsEnabled.value = result.dynamics
                dynamicsManager?.setEnabled(result.dynamics)
            }
        }
    }

    fun clearAiResult() { _aiResult.value = null }

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
