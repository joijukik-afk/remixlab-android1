package com.joijukik.remixlab

import android.media.audiofx.LoudnessEnhancer

class DynamicsProcessorManager(audioSessionId: Int) {

    private val loudness: LoudnessEnhancer? = try {
        LoudnessEnhancer(audioSessionId).apply {
            setTargetGain(0)
            enabled = false
        }
    } catch (e: Exception) { null }

    fun setEnabled(enabled: Boolean, targetGainMb: Int = 600) {
        loudness?.let {
            it.setTargetGain(targetGainMb)
            it.enabled = enabled
        }
    }

    fun release() { try { loudness?.release() } catch (_: Exception) {} }
}
