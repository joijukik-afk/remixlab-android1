package com.joijukik.remixlab.data

import android.content.Context

/**
 * نسخه ساده: جداسازی شبیه‌سازی‌شده.
 * (بدون نیاز به مدل ONNX)
 */
class SeparationManager(private val context: Context) {

    enum class Stem { VOCALS, DRUMS, BASS, OTHER }

    fun initialize() { /* بدون نیاز به کار خاصی */ }

    /**
     * فیلتر روی استم‌ها (شبیه‌سازی‌شده).
     * فقط برای UI استفاده می‌شود.
     */
    fun getActiveStems(filter: String): List<Stem> {
        return when (filter) {
            "VOCALS_ONLY" -> listOf(Stem.VOCALS)
            "INSTRUMENTAL" -> listOf(Stem.DRUMS, Stem.BASS, Stem.OTHER)
            "NO_DRUMS" -> listOf(Stem.VOCALS, Stem.BASS, Stem.OTHER)
            "NO_BASS" -> listOf(Stem.VOCALS, Stem.DRUMS, Stem.OTHER)
            else -> listOf(Stem.VOCALS, Stem.DRUMS, Stem.BASS, Stem.OTHER)
        }
    }

    fun release() { /* بدون نیاز */ }
}
