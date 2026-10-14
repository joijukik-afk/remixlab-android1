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

    private var reverb: PresetReverb? = null

    private val _isSlowed = MutableStateFlow(false)
    val isSlowed: StateFlow<Boolean> = _isSlowed

    private val _isReverbEnabled = MutableStateFlow(false)
    val isReverbEnabled: StateFlow<Boolean> = _isReverbEnabled

    private val _currentSpeed = MutableStateFlow(1.0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    fun loadSong(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        setupReverbIfNeeded()
        player.play()
    }

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

    fun toggleReverb() {
        _isReverbEnabled.value = !_isReverbEnabled.value
        setupReverbIfNeeded()
        reverb?.enabled = _isReverbEnabled.value
    }

    private fun setupReverbIfNeeded() {
        if (reverb == null) {
            val sessionId = player.audioSessionId
            if (sessionId != 0) {
                try {
                    reverb = PresetReverb(0, sessionId).apply {
                        preset = PresetReverb.PRESET_LARGEHALL
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setCustomSpeed(speed: Float) {
        _currentSpeed.value = speed
        player.playbackParameters = PlaybackParameters(speed)
        _isSlowed.value = speed < 1.0f
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        reverb?.release()
        player.release()
    }
}
