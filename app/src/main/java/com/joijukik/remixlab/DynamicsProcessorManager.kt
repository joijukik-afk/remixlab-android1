package com.joijukik.remixlab

import android.media.audiofx.LoudnessEnhancer
import android.util.Log

class DynamicsProcessorManager(audioSessionId: Int) {

    private val tag = "Dynamics"

    private val loudness: LoudnessEnhancer? = try {
        LoudnessEnhancer(audioSessionId).apply {
            setTargetGain(0)
            enabled = false
        }
    } catch (e: Exception) {
        Log.e(tag, "LoudnessEnhancer init failed", e); null
    }

    fun setEnabled(enabled: Boolean, targetGainMb: Int = 600) {
        loudness?.let {
            it.setTargetGain(targetGainMb)
            it.enabled = enabled
        }
    }

    fun setGain(gainMb: Int) {
        try { loudness?.setTargetGain(gainMb) } catch (e: Exception) {
            Log.e(tag, "setGain fail", e)
        }
    }

    fun release() {
        try { loudness?.release() } catch (_: Exception) {}
    }
}
