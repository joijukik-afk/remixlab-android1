package com.joijukik.remixlab

import android.media.audiofx.BassBoost
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

@UnstableApi
class RemixAudioProcessor : BaseAudioProcessor() {

    private val sonicProcessor = SonicAudioProcessor()
    private var reverb: PresetReverb? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var audioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    fun setSpeed(speed: Float) {
        sonicProcessor.setSpeed(speed)
    }

    fun setPitch(pitch: Float) {
        sonicProcessor.setPitch(pitch)
    }

    fun enableReverb(enable: Boolean) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (reverb == null) {
            reverb = PresetReverb(0, audioSessionId)
        }
        reverb?.enabled = enable
        if (enable) {
            reverb?.preset = PresetReverb.PRESET_LARGEHALL
        }
    }

    fun enableBassBoost(enable: Boolean) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (bassBoost == null) {
            bassBoost = BassBoost(0, audioSessionId)
        }
        bassBoost?.enabled = enable
        if (enable) {
            bassBoost?.setStrength(1000.toShort()) // 1000 از 1000
        }
    }

    fun enableVirtualizer(enable: Boolean) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (virtualizer == null) {
            virtualizer = Virtualizer(0, audioSessionId)
        }
        virtualizer?.enabled = enable
        if (enable) {
            virtualizer?.setStrength(1000.toShort()) // 1000 از 1000
        }
    }

    fun setAudioSessionId(sessionId: Int) {
        this.audioSessionId = sessionId
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return sonicProcessor.configure(inputAudioFormat)
    }

    override fun isActive(): Boolean = sonicProcessor.isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        sonicProcessor.queueInput(inputBuffer)
        replaceOutputBuffer(sonicProcessor.output.remaining()).put(sonicProcessor.output).flip()
    }

    override fun onQueueEndOfStream() {
        sonicProcessor.queueEndOfStream()
    }

    override fun onFlush() {
        sonicProcessor.flush()
    }

    override fun onReset() {
        sonicProcessor.reset()
        reverb?.release()
        reverb = null
        bassBoost?.release()
        bassBoost = null
        virtualizer?.release()
        virtualizer = null
    }
}
