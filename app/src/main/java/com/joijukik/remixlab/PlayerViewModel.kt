package com.joijukik.remixlab

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.DefaultAudioSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioProcessor = RemixAudioProcessor()

    private val audioSink: DefaultAudioSink = DefaultAudioSink.Builder(application)
        .setAudioProcessors(arrayOf(audioProcessor))
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(application)
        .setAudioSink(audioSink)
        .build()

    private val _isSlowed = MutableStateFlow(false)
    val isSlowed: StateFlow<Boolean> = _isSlowed

    private val _isReverbEnabled = MutableStateFlow(false)
    val isReverbEnabled: StateFlow<Boolean> = _isReverbEnabled

    private val _isBassBoostEnabled = MutableStateFlow(false)
    val isBassBoostEnabled: StateFlow<Boolean> = _isBassBoostEnabled

    private val _isVirtualizerEnabled = MutableStateFlow(false)
    val isVirtualizerEnabled: StateFlow<Boolean> = _isVirtualizerEnabled

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
                audioProcessor.setAudioSessionId(audioSessionId)
            }
        })
    }

    fun loadSong(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    fun toggleSlowed() {
        _isSlowed.value = !_isSlowed.value
        if (_isSlowed.value) {
            audioProcessor.setSpeed(0.8f)
            _currentSpeed.value = 0.8f
        } else {
            audioProcessor.setSpeed(1.0f)
            _currentSpeed.value = 1.0f
        }
    }

    fun toggleReverb() {
        _isReverbEnabled.value = !_isReverbEnabled.value
        audioProcessor.enableReverb(_isReverbEnabled.value)
    }

    fun toggleBassBoost() {
        _isBassBoostEnabled.value = !_isBassBoostEnabled.value
        audioProcessor.enableBassBoost(_isBassBoostEnabled.value)
    }

    fun toggleVirtualizer() {
        _isVirtualizerEnabled.value = !_isVirtualizerEnabled.value
        audioProcessor.enableVirtualizer(_isVirtualizerEnabled.value)
    }

    fun setCustomSpeed(speed: Float) {
        _currentSpeed.value = speed
        audioProcessor.setSpeed(speed)
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
        player.release()
    }
}
