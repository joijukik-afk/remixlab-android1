package com.joijukik.remixlab

import android.app.Application
import android.media.audiofx.PresetReverb
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private var effectsManager: AudioEffectsManager? = null

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

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                setupEffects(audioSessionId)
            }
        })
    }

    private fun setupEffects(audioSessionId: Int) {
        effectsManager?.release()
        effectsManager = try {
            AudioEffectsManager(audioSessionId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        // اعمال مجدد افکت‌های فعال
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
        // سرعت
        _currentSpeed.value = preset.speed
        _isSlowed.value = preset.speed < 1.0f

        // گام
        _currentPitch.value = preset.pitch

        // اعمال هر دو با هم
        applyPlaybackParameters()

        // Reverb
        if (preset.reverbPreset != null) {
            _isReverbEnabled.value = true
            _currentReverbPreset.value = preset.reverbPreset
            effectsManager?.setReverbEnabled(true, preset.reverbPreset)
        } else {
            _isReverbEnabled.value = false
            effectsManager?.setReverbEnabled(false)
        }

        // Bass Boost
        _isBassBoostEnabled.value = preset.bassBoost
        effectsManager?.setBassBoostEnabled(preset.bassBoost)

        // Virtualizer
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

    // --- آزادسازی ---

    override fun onCleared() {
        super.onCleared()
        effectsManager?.release()
        player.release()
    }
}
