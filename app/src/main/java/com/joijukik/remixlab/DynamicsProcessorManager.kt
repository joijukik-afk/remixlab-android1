package com.joijukik.remixlab

import android.media.audiofx.LoudnessEnhancer

class DynamicsProcessorManager(audioSessionId: Int) {

    private var loudnessEnhancer: LoudnessEnhancer? = null

    init {
        if (audioSessionId != 0 && audioSessionId != -1) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(300)
                    enabled = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loudnessEnhancer = null
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            loudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setGain(gainMillibels: Int) {
        try {
            loudnessEnhancer?.setTargetGain(gainMillibels.coerceIn(0, 2000))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) { }
        loudnessEnhancer = null
    }
}
