package com.joijukik.remixlab

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer

class AudioEffectsManager(audioSessionId: Int) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null

    init {
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            e.printStackTrace()
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            e.printStackTrace()
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            e.printStackTrace()
            virtualizer = null
        }

        try {
            reverb = PresetReverb(0, audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            e.printStackTrace()
            reverb = null
        }
    }

    // --- Reverb (طنین) ---

    fun setReverbEnabled(enabled: Boolean, preset: Short = PresetReverb.PRESET_LARGEHALL) {
        val r = reverb ?: return
        r.enabled = enabled
        if (enabled) {
            r.preset = preset
        }
    }

    // --- Bass Boost (تقویت باس) ---

    fun setBassBoostEnabled(enabled: Boolean) {
        val b = bassBoost ?: return
        b.enabled = enabled
        if (enabled) {
            b.setStrength(1000.toShort())
        }
    }

    // --- Virtualizer (8D Audio) ---

    fun setVirtualizerEnabled(enabled: Boolean) {
        val v = virtualizer ?: return
        v.enabled = enabled
        if (enabled) {
            v.setStrength(1000.toShort())
        }
    }

    // --- Equalizer (اکولایزر) ---

    fun setEqualizerEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
    }

    fun setEqualizerBandLevel(bandIndex: Int, level: Short) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), level)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- آزادسازی منابع ---

    fun release() {
        try { equalizer?.release() } catch (e: Exception) { }
        try { bassBoost?.release() } catch (e: Exception) { }
        try { virtualizer?.release() } catch (e: Exception) { }
        try { reverb?.release() } catch (e: Exception) { }

        equalizer = null
        bassBoost = null
        virtualizer = null
        reverb = null
    }
}
