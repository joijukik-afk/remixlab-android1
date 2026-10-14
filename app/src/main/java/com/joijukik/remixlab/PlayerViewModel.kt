package com.joijukik.remixlab

import android.app.Application
import android.media.audiofx.PresetReverb
import android.media.projection.MediaProjection
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    private val audioExporter = AudioExporter(application)

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

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingSeconds = MutableStateFlow(0L)
    val recordingSeconds: StateFlow<Long> = _recordingSeconds

    private val _lastSavedUri = MutableStateFlow<Uri?>(null)
    val lastSavedUri: StateFlow<Uri?> = _lastSavedUri

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

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    setupEffects(player.audioSessionId)
                }
            }
        })

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
    }

    fun loadSong(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

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

    fun toggleBassBoost() {
        _isBassBoostEnabled.value = !_isBassBoostEnabled.value
        effectsManager?.setBassBoostEnabled(_isBassBoostEnabled.value)
    }

    fun toggle8D() {
        _is8DEnabled.value = !_is8DEnabled.value
        effectsManager?.setVirtualizerEnabled(_is8DEnabled.value)
    }

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

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun startRecording(mediaProjection: MediaProjection) {
        _recordingError.value = null
        _lastSavedUri.value = null
        _recordingSeconds.value = 0L

        val started = audioExporter.startRecording(mediaProjection)
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
        effectsManager?.release()
        player.release()
    }
}
