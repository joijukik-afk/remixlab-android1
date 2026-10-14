package com.joijukik.remixlab

import android.media.audiofx.PresetReverb
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import java.nio.ByteBuffer

class RemixAudioProcessor : AudioProcessor {

    private val sonicProcessor = SonicAudioProcessor()
    private var reverb: PresetReverb? = null
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

    fun setAudioSessionId(sessionId: Int) {
        this.audioSessionId = sessionId
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return sonicProcessor.configure(inputAudioFormat)
    }

    override fun isActive(): Boolean = sonicProcessor.isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        sonicProcessor.queueInput(inputBuffer)
    }

    override fun queueEndOfStream() {
        sonicProcessor.queueEndOfStream()
    }

    override fun getOutput(): ByteBuffer = sonicProcessor.output

    override fun isEnded(): Boolean = sonicProcessor.isEnded

    override fun flush() {
        sonicProcessor.flush()
    }

    override fun reset() {
        sonicProcessor.reset()
        reverb?.release()
        reverb = null
    }
}
