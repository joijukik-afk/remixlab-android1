package com.joijukik.remixlab

import android.media.audiofx.PresetReverb

data class ComaPreset(
    val id: String,
    val name: String,
    val description: String,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val reverbPreset: Short? = null,
    val bassBoost: Boolean = false,
    val virtualizer: Boolean = false,
    val ambientSound: AmbientSoundManager.SoundType? = null,
    val ambientVolume: Float = 0.4f
)

object ComaPresets {
    val all = listOf(
        ComaPreset("coma_classic", "COMA Classic", "تجربه کلاسیک",
            speed = 0.85f, reverbPreset = PresetReverb.PRESET_LARGEROOM, bassBoost = true),
        ComaPreset("coma_dream", "COMA Dream", "رویایی و آرام",
            speed = 0.75f, pitch = 0.95f, reverbPreset = PresetReverb.PRESET_PLATE,
            virtualizer = true, ambientSound = AmbientSoundManager.SoundType.RAIN),
        ComaPreset("coma_night", "COMA Night", "شب‌های آرام",
            speed = 0.8f, pitch = 0.9f, reverbPreset = PresetReverb.PRESET_LARGEROOM,
            ambientSound = AmbientSoundManager.SoundType.THUNDER),
        ComaPreset("coma_deep", "COMA Deep", "بیس عمیق",
            speed = 0.7f, pitch = 0.85f, reverbPreset = PresetReverb.PRESET_LARGEROOM,
            bassBoost = true, ambientSound = AmbientSoundManager.SoundType.WIND),
        ComaPreset("coma_8d", "COMA 8D", "چرخش ۸ بعدی",
            virtualizer = true, reverbPreset = PresetReverb.PRESET_MEDIUMROOM),
        ComaPreset("coma_slowed", "COMA Slowed", "کلاسیک Slowed",
            speed = 0.85f, pitch = 0.95f, reverbPreset = PresetReverb.PRESET_PLATE),
        ComaPreset("coma_nightcore", "COMA Nightcore", "تند و روشن",
            speed = 1.25f, pitch = 1.15f, reverbPreset = PresetReverb.PRESET_SMALLROOM),
        ComaPreset("coma_cafe", "COMA Café", "حس کافه دنج",
            speed = 0.9f, reverbPreset = PresetReverb.PRESET_MEDIUMROOM,
            ambientSound = AmbientSoundManager.SoundType.FOREST)
    )
}
