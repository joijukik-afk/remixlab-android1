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
        // اگه session ID معتبر نیست، کاری نکن
        if (audioSessionId == 0 || audioSessionId == -1) {
            return
        }

        equalizer = try {
            Equalizer(0, audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        bassBoost = try {
            BassBoost(0, audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        virtualizer = try {
            Virtualizer(0, audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        reverb = try {
            PresetReverb(0, audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Reverb ---

    fun setReverbEnabled(enabled: Boolean, preset: Short = PresetReverb.PRESET_LARGEHALL) {
        val r = reverb ?: return
        try {
            r.enabled = enabled
            if (enabled) {
                r.preset = preset
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Bass Boost ---

    fun setBassBoostEnabled(enabled: Boolean) {
        val b = bassBoost ?: return
        try {
            b.enabled = enabled
            if (enabled) {
                b.setStrength(1000.toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Virtualizer (8D Audio) ---

    fun setVirtualizerEnabled(enabled: Boolean) {
        val v = virtualizer ?: return
        try {
            v.enabled = enabled
            if (enabled) {
                v.setStrength(1000.toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Equalizer ---

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        try { equalizer?.release() } catch (_: Exception) { }
        try { bassBoost?.release() } catch (_: Exception) { }
        try { virtualizer?.release() } catch (_: Exception) { }
        try { reverb?.release() } catch (_: Exception) { }

        equalizer = null
        bassBoost = null
        virtualizer = null
        reverb = null
    }
}
