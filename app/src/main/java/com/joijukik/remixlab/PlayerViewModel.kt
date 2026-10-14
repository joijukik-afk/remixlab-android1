package com.joijukik.remixlab

import android.app.Application
import android.media.audiofx.PresetReverb
import android.media.projection.MediaProjection
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private var effectsManager: AudioEffectsManager? = null
    private val audioExporter = AudioExporter(application)

    // --- State ها ---

    private val _isSlowed = MutableStateFlow(false)
    val isSlowed: StateFlow<Boolean> = _isSlowed

    private val _isReverbEnabled = MutableStateFlow(false)
    val isReverbEnabled: StateFlow<Boolean> = _isReverbEnabled

    private val _isBassBoostEnabled = MutableStateFlow(false)
    val isBassBoostEnabled: StateFlow<Boolean> = _isBassBoostEnabled

    private val _is8DEnabled = MutableStateFlow(false)
    val is8DEnabled: StateFlow<Boolean> = _is8DEnabled

    private val _currentSpeed = MutableStateFlow(1.0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed

    private val _currentPitch = MutableStateFlow(1.0f)
    val currentPitch: StateFlow<Float> = _currentPitch

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentReverbPreset = MutableStateFlow<Short>(PresetReverb.PRESET_LARGEHALL)
    val currentReverbPreset: StateFlow<Short> = _currentReverbPreset

    // --- State های ضبط ---
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingSeconds = MutableStateFlow(0L)
    val recordingSeconds: StateFlow<Long> = _recordingSeconds

    private val _lastSavedFile = MutableStateFlow<File?>(null)
    val lastSavedFile: StateFlow<File?> = _lastSavedFile

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                setupEffects(audioSessionId)
            }
        })

        // کالبک‌های ضبط
        audioExporter.onProgress = { bytes ->
            // تخمین ثانیه از روی بایت
            _recordingSeconds.value = bytes / (44100L * 4L)
        }
        audioExporter.onFinish = { file ->
            _lastSavedFile.value = file
            _isRecording.value = false
        }
        audioExporter.onError = { msg ->
            _recordingError.value = msg
            _isRecording.value = false
        }
    }

    private fun setupEffects(audioSessionId: Int) {
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
    }

    // --- بارگذاری آهنگ ---

    fun loadSong(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    // --- Slowed ---

    fun toggleSlowed() {
        _isSlowed.value = !_isSlowed.value
        if (_isSlowed.value) {
            setSpeedInternal(0.8f)
        } else {
            setSpeedInternal(1.0f)
        }
    }

    // --- سرعت ---

    fun setCustomSpeed(speed: Float) {
        setSpeedInternal(speed)
    }

    private fun setSpeedInternal(speed: Float) {
        _currentSpeed.value = speed
        _isSlowed.value = speed < 1.0f
        applyPlaybackParameters()
    }

    // --- گام (Pitch) ---

    fun setPitch(pitch: Float) {
        _currentPitch.value = pitch
        applyPlaybackParameters()
    }

    private fun applyPlaybackParameters() {
        player.playbackParameters = PlaybackParameters(_currentSpeed.value, _currentPitch.value)
    }

    // --- Reverb ---

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

    // --- Bass Boost ---

    fun toggleBassBoost() {
        _isBassBoostEnabled.value = !_isBassBoostEnabled.value
        effectsManager?.setBassBoostEnabled(_isBassBoostEnabled.value)
    }

    // --- 8D Audio ---

    fun toggle8D() {
        _is8DEnabled.value = !_is8DEnabled.value
        effectsManager?.setVirtualizerEnabled(_is8DEnabled.value)
    }

    // --- اعمال پریست COMA ---

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
    }

    // --- پخش/توقف ---

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    // --- ضبط (Recording) ---

    fun startRecording(mediaProjection: MediaProjection) {
        // پاکسازی خطاهای قبلی
        _recordingError.value = null
        _lastSavedFile.value = null
        _recordingSeconds.value = 0L

        val started = audioExporter.startRecording(mediaProjection)
        if (started) {
            _isRecording.value = true
            // اگه آهنگ پخش نمی‌شه، پخش رو شروع کن
            if (!player.isPlaying) {
                player.play()
            }
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

    // --- آزادسازی ---

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) {
            audioExporter.stopRecording()
        }
        effectsManager?.release()
        player.release()
    }
}
