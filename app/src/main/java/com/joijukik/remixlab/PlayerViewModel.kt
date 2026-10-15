package com.joijukik.remixlab

import android.app.Application
import android.media.audiofx.PresetReverb
import android.media.projection.MediaProjection
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(50000, 50000, 2500, 5000)
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(application)
        .setLoadControl(loadControl)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var effectsManager: AudioEffectsManager? = null
    private var dynamicsManager: DynamicsProcessorManager? = null
    private val audioExporter = AudioExporter(application)
    private val ambientManager = AmbientSoundManager()

    // ─── State های اصلی ───
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

    private val _currentSpeed = MutableStateFlow(1.0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed

    private val _currentPitch = MutableStateFlow(1.0f)
    val currentPitch: StateFlow<Float> = _currentPitch

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentReverbPreset = MutableStateFlow<Short>(PresetReverb.PRESET_LARGEHALL)
    val currentReverbPreset: StateFlow<Short> = _currentReverbPreset

    // ─── State های پخش (جدید) ───
    private val _songName = MutableStateFlow("هیچ آهنگی انتخاب نشده")
    val songName: StateFlow<String> = _songName

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    // ─── کیفیت MP3 (جدید) ───
    private val _exportQuality = MutableStateFlow(320)
    val exportQuality: StateFlow<Int> = _exportQuality

    // ─── State های ضبط ───
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingSeconds = MutableStateFlow(0L)
    val recordingSeconds: StateFlow<Long> = _recordingSeconds

    private val _lastSavedUri = MutableStateFlow<Uri?>(null)
    val lastSavedUri: StateFlow<Uri?> = _lastSavedUri

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError

    // ─── State های افکت محیطی ───
    private val _activeAmbient = MutableStateFlow<AmbientSoundManager.SoundType?>(null)
    val activeAmbient: StateFlow<AmbientSoundManager.SoundType?> = _activeAmbient

    private val _ambientVolume = MutableStateFlow(0.4f)
    val ambientVolume: StateFlow<Float> = _ambientVolume

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                setupEffects(audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    setupEffects(player.audioSessionId)
                    _duration.value = player.duration.coerceAtLeast(0)
                }
            }
        })

        // ─── به‌روزرسانی موقعیت پخش هر ۵۰۰ میلی‌ثانیه ───
        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    _currentPosition.value = player.currentPosition
                    _duration.value = player.duration.coerceAtLeast(0)
                }
                delay(500)
            }
        }

        // کالبک‌های ضبط
        audioExporter.onProgress = { bytes ->
            _recordingSeconds.value = bytes / (48000L * 4L)
        }
        audioExporter.onFinish = { uri ->
            _lastSavedUri.value = uri
            _isRecording.value = false
        }
        audioExporter.onError = { msg ->
            _recordingError.value = msg
            _isRecording.value = false
        }
    }

    private fun setupEffects(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

        effectsManager?.release()
        effectsManager = try {
            AudioEffectsManager(audioSessionId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        effectsManager?.setReverbEnabled(_isReverbEnabled.value, _currentReverbPreset.value)
        effectsManager?.setBassBoostEnabled(_isBassBoostEnabled.value)
        effectsManager?.setVirtualizerEnabled(_is8DEnabled.value)

        dynamicsManager?.release()
        dynamicsManager = try {
            DynamicsProcessorManager(audioSessionId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        dynamicsManager?.setEnabled(_isDynamicsEnabled.value)
    }

    // ─── بارگذاری آهنگ ───
    fun loadSong(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
        _songName.value = getFileName(uri)
    }

    // ─── گرفتن اسم فایل ───
    private fun getFileName(uri: Uri): String {
        var name = "آهنگ ناشناخته"
        try {
            getApplication<Application>().contentResolver.query(
                uri, null, null, null, null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }

    // ─── پرش به موقعیت (Seek) ───
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    // ─── تنظیم کیفیت MP3 ───
    fun setExportQuality(kbps: Int) {
        _exportQuality.value = kbps
    }

    // ─── Slowed ───
    fun toggleSlowed() {
        _isSlowed.value = !_isSlowed.value
        if (_isSlowed.value) setSpeedInternal(0.8f) else setSpeedInternal(1.0f)
    }

    fun setCustomSpeed(speed: Float) {
        setSpeedInternal(speed)
    }

    private fun setSpeedInternal(speed: Float) {
        _currentSpeed.value = speed
        _isSlowed.value = speed < 1.0f
        applyPlaybackParameters()
    }

    fun setPitch(pitch: Float) {
        _currentPitch.value = pitch
        applyPlaybackParameters()
    }

    private fun applyPlaybackParameters() {
        player.playbackParameters = PlaybackParameters(_currentSpeed.value, _currentPitch.value)
    }

    // ─── Reverb ───
    fun toggleReverb() {
        _isReverbEnabled.value = !_isReverbEnabled.value
        effectsManager?.setReverbEnabled(_isReverbEnabled.value, _currentReverbPreset.value)
    }

    fun setReverbPreset(preset: Short) {
        _currentReverbPreset.value = preset
        if (_isReverbEnabled.value) {
            effectsManager?.setReverbEnabled(true, preset)
        }
    }

    // ─── Bass Boost ───
    fun toggleBassBoost() {
        _isBassBoostEnabled.value = !_isBassBoostEnabled.value
        effectsManager?.setBassBoostEnabled(_isBassBoostEnabled.value)
    }

    // ─── 8D Audio ───
    fun toggle8D() {
        _is8DEnabled.value = !_is8DEnabled.value
        effectsManager?.setVirtualizerEnabled(_is8DEnabled.value)
    }

    // ─── Dynamics ───
    fun toggleDynamics() {
        _isDynamicsEnabled.value = !_isDynamicsEnabled.value
        dynamicsManager?.setEnabled(_isDynamicsEnabled.value)
    }

    // ─── اعمال پریست COMA ───
    fun applyComaPreset(preset: ComaPreset) {
        _currentSpeed.value = preset.speed
        _isSlowed.value = preset.speed < 1.0f
        _currentPitch.value = preset.pitch
        applyPlaybackParameters()

        if (preset.reverbPreset != null) {
            _isReverbEnabled.value = true
            _currentReverbPreset.value = preset.reverbPreset
            effectsManager?.setReverbEnabled(true, preset.reverbPreset)
        } else {
            _isReverbEnabled.value = false
            effectsManager?.setReverbEnabled(false)
        }

        _isBassBoostEnabled.value = preset.bassBoost
        effectsManager?.setBassBoostEnabled(preset.bassBoost)

        _is8DEnabled.value = preset.virtualizer
        effectsManager?.setVirtualizerEnabled(preset.virtualizer)

        ambientManager.stop()
        if (preset.ambientSound != null) {
            _ambientVolume.value = preset.ambientVolume
            ambientManager.play(preset.ambientSound, preset.ambientVolume)
            _activeAmbient.value = preset.ambientSound
        } else {
            _activeAmbient.value = null
        }
    }

    // ─── اعمال تنظیمات AI ───
    fun applyAiTuning(result: AiTuningAssistant.TuningResult) {
        _currentSpeed.value = result.speed
        _isSlowed.value = result.speed < 1.0f
        _currentPitch.value = result.pitch
        applyPlaybackParameters()

        if (result.reverb && result.reverbPreset != null) {
            _isReverbEnabled.value = true
            _currentReverbPreset.value = result.reverbPreset
            effectsManager?.setReverbEnabled(true, result.reverbPreset)
        } else {
            _isReverbEnabled.value = false
            effectsManager?.setReverbEnabled(false)
        }

        _isBassBoostEnabled.value = result.bassBoost
        effectsManager?.setBassBoostEnabled(result.bassBoost)

        _is8DEnabled.value = result.virtualizer
        effectsManager?.setVirtualizerEnabled(result.virtualizer)

        _isDynamicsEnabled.value = result.dynamics
        dynamicsManager?.setEnabled(result.dynamics)

        ambientManager.stop()
        if (result.ambientSound != null) {
            _ambientVolume.value = result.ambientVolume
            ambientManager.play(result.ambientSound, result.ambientVolume)
            _activeAmbient.value = result.ambientSound
        } else {
            _activeAmbient.value = null
        }
    }

    // ─── افکت‌های محیطی ───
    fun toggleAmbient(type: AmbientSoundManager.SoundType) {
        if (_activeAmbient.value == type) {
            ambientManager.stop()
            _activeAmbient.value = null
        } else {
            ambientManager.play(type, _ambientVolume.value)
            _activeAmbient.value = type
        }
    }

    fun setAmbientVolume(volume: Float) {
        _ambientVolume.value = volume
        ambientManager.setVolume(volume)
    }

    fun stopAllAmbient() {
        ambientManager.stop()
        _activeAmbient.value = null
    }

    // ─── پخش/توقف ───
    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    // ─── ضبط ───
    fun startRecording(mediaProjection: MediaProjection) {
        _recordingError.value = null
        _lastSavedUri.value = null
        _recordingSeconds.value = 0L

        if (!_isDynamicsEnabled.value) {
            _isDynamicsEnabled.value = true
            dynamicsManager?.setEnabled(true)
        }

        val started = audioExporter.startRecording(
            mediaProjection,
            bitRate = _exportQuality.value * 1024
        )
        if (started) {
            _isRecording.value = true
            if (!player.isPlaying) player.play()
        } else {
            _isRecording.value = false
        }
    }

    fun stopRecording() {
        audioExporter.stopRecording()
        _isRecording.value = false
    }

    fun clearRecordingError() {
        _recordingError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) audioExporter.stopRecording()
        ambientManager.stop()
        dynamicsManager?.release()
        effectsManager?.release()
        player.release()
    }
}
