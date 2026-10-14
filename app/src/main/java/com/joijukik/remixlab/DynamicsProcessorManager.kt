package com.joijukik.remixlab

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.DynamicsProcessing.Config
import android.media.audiofx.DynamicsProcessing.Limiter
import android.media.audiofx.DynamicsProcessing.Mbc
import android.media.audiofx.DynamicsProcessing.Eq

class DynamicsProcessorManager(audioSessionId: Int) {

    private var dynamicsProcessor: DynamicsProcessing? = null

    init {
        if (audioSessionId == 0 || audioSessionId == -1) return

        try {
            // پیکربندی با ۲ باند (استریو)
            val config = Config.Builder(2) // ۲ کانال
                .setInputGain(-2f) // کاهش ورودی برای جلوگیری از کلیپینگ
                .setLimiterByChannelIndex(0, true)
                .setLimiterByChannelIndex(1, true)
                .build()

            dynamicsProcessor = DynamicsProcessing(0, audioSessionId, config)
            dynamicsProcessor?.enabled = false
        } catch (e: Exception) {
            e.printStackTrace()
            dynamicsProcessor = null
        }
    }

    fun setEnabled(enabled: Boolean) {
        dynamicsProcessor?.enabled = enabled
    }

    fun setInputGain(db: Float) {
        try {
            dynamicsProcessor?.setInputGain(db)
        } catch (e: Exception) { }
    }

    fun release() {
        try {
            dynamicsProcessor?.release()
        } catch (_: Exception) { }
        dynamicsProcessor = null
    }
}
