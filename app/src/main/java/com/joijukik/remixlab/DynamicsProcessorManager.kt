package com.joijukik.remixlab

import android.media.audiofx.LoudnessEnhancer

/**
 * مدیریت پردازش دینامیک صدا: تقویت بلندی (Loudness Enhancement)
 * این کلاس کیفیت خروجی رو بالا می‌بره و صدا رو شفاف‌تر می‌کنه
 */
class DynamicsProcessorManager(audioSessionId: Int) {

    private var loudnessEnhancer: LoudnessEnhancer? = null

    init {
        if (audioSessionId != 0 && audioSessionId != -1) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(300)   // 3 dB تقویت
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

    /**
     * تنظیم مقدار تقویت
     * @param gainMillibels مقدار بین 0 تا 2000 (یعنی 0 تا 20 dB)
     */
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
