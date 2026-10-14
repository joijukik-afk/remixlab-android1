package com.joijukik.remixlab

import android.app.Application
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

    // --- State ها برای UI ---

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

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

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
        // اگه افکت‌ها قبلاً روشن بودن، دوباره اعمال کن
        effectsManager?.setReverbEnabled(_isReverbEnabled.value)
        effectsManager?.setBassBoostEnabled(_isBassBoostEnabled.value)
        effectsManager?.setVirtualizerEnabled(_is8DEnabled.value)
    }

    // --- بارگذاری و پخش آهنگ ---

    fun loadSong(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    // --- Slowed ---

    fun toggleSlowed() {
        _isSlowed.value = !_isSlowed.value
        if (_isSlowed.value) {
            player.playbackParameters = PlaybackParameters(0.8f)
            _currentSpeed.value = 0.8f
        } else {
            player.playbackParameters = PlaybackParameters(1.0f)
            _currentSpeed.value = 1.0f
        }
    }

    // --- سرعت دلخواه (اسلایدر) ---

    fun setCustomSpeed(speed: Float) {
        _currentSpeed.value = speed
        player.playbackParameters = PlaybackParameters(speed)
        _isSlowed.value = speed < 1.0f
    }

    // --- Reverb ---

    fun toggleReverb() {
        _isReverbEnabled.value = !_isReverbEnabled.value
        effectsManager?.setReverbEnabled(_isReverbEnabled.value)
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

    // --- پخش/توقف ---

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    // --- آزادسازی منابع ---

    override fun onCleared() {
        super.onCleared()
        effectsManager?.release()
        player.release()
    }
}
