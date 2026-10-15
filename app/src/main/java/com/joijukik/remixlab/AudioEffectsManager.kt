package com.joijukik.remixlab

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log

class AudioEffectsManager(audioSessionId: Int) {

    private val tag = "AudioEffects"

    private val equalizer: Equalizer? = try {
        Equalizer(0, audioSessionId).apply { enabled = true }
    } catch (e: Exception) { Log.e(tag, "EQ fail", e); null }

    private val bassBoost: BassBoost? = try {
        BassBoost(0, audioSessionId).apply { enabled = false }
    } catch (e: Exception) { Log.e(tag, "Bass fail", e); null }

    private val virtualizer: Virtualizer? = try {
        Virtualizer(0, audioSessionId).apply { enabled = false }
    } catch (e: Exception) { Log.e(tag, "Virt fail", e); null }

    private val reverb: PresetReverb? = try {
        PresetReverb(0, audioSessionId).apply { enabled = false }
    } catch (e: Exception) { Log.e(tag, "Rev fail", e); null }

    fun setBassBoostEnabled(enabled: Boolean, strength: Short = 800) {
        bassBoost?.let { it.setStrength(strength); it.enabled = enabled }
    }

    fun setVirtualizerEnabled(enabled: Boolean, strength: Short = 600) {
        virtualizer?.let { it.setStrength(strength); it.enabled = enabled }
    }

    fun setReverbEnabled(enabled: Boolean, preset: Short = PresetReverb.PRESET_LARGEROOM) {
        reverb?.let { it.preset = preset; it.enabled = enabled }
    }

    /**
     * تنظیم گین باند اکولایزر.
     * مقدار gainDb رو به میلی‌بل تبدیل می‌کنیم (1 dB = 100 mB)
     */
    fun setPreEqBandGain(band: Int, gainDb: Float) {
        try {
            equalizer?.let {
                if (band in 0 until it.numberOfBands) {
                    it.setBandLevel(band.toShort(), (gainDb * 100).toInt().toShort())
                }
            }
        } catch (e: Exception) { Log.e(tag, "EQ band fail", e) }
    }

    // متدهای زیر به سادگی کاری انجام نمی‌دن (فقط برای سازگاری با UI)
    fun setCompressorThreshold(band: Int, thresholdDb: Float) { /* no-op */ }
    fun setCompressorRatio(band: Int, ratio: Float) { /* no-op */ }
    fun setLimiterEnabled(enabled: Boolean) { /* no-op */ }
    fun setLimiterThreshold(thresholdDb: Float) { /* no-op */ }
    fun setInputGain(gainDb: Float) { /* no-op */ }

    fun resetAll() {
        bassBoost?.enabled = false
        virtualizer?.enabled = false
        reverb?.enabled = false
    }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { reverb?.release() } catch (_: Exception) {}
    }
}
