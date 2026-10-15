package com.joijukik.remixlab

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
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

    private val dynamics: DynamicsProcessing? = try {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            2, true, 10, true, 4, true, 10, true
        ).build()
        DynamicsProcessing(0, audioSessionId, config).apply { enabled = true }
    } catch (e: Exception) { Log.e(tag, "Dyn fail", e); null }

    fun setBassBoostEnabled(enabled: Boolean, strength: Short = 800) {
        bassBoost?.let { it.setStrength(strength); it.enabled = enabled }
    }

    fun setVirtualizerEnabled(enabled: Boolean, strength: Short = 600) {
        virtualizer?.let { it.setStrength(strength); it.enabled = enabled }
    }

    fun setReverbEnabled(enabled: Boolean, preset: Short = PresetReverb.PRESET_LARGEROOM) {
        reverb?.let { it.preset = preset; it.enabled = enabled }
    }

    fun setPreEqBandGain(band: Int, gainDb: Float) {
        dynamics?.let {
            val eq = it.getPreEqByChannelIndex(0)
            if (band < eq.bandCount) eq.setBandLevel(band, gainDb)
        }
    }

    fun setCompressorThreshold(band: Int, thresholdDb: Float) {
        dynamics?.let {
            val mbc = it.getMbcByChannelIndex(0)
            if (band < mbc.bandCount) mbc.setThreshold(band, thresholdDb)
        }
    }

    fun setCompressorRatio(band: Int, ratio: Float) {
        dynamics?.let {
            val mbc = it.getMbcByChannelIndex(0)
            if (band < mbc.bandCount) mbc.setRatio(band, ratio)
        }
    }

    fun setLimiterEnabled(enabled: Boolean) {
        dynamics?.limiterEnabled = enabled
    }

    fun setLimiterThreshold(thresholdDb: Float) {
        dynamics?.let { it.limiterThreshold = thresholdDb }
    }

    fun setInputGain(gainDb: Float) {
        dynamics?.inputGain = gainDb
    }

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
        try { dynamics?.release() } catch (_: Exception) {}
    }
}
