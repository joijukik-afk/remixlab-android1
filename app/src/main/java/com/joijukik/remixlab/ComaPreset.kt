package com.joijukik.remixlab

import android.media.audiofx.PresetReverb

data class ComaPreset(
    val id: String,
    val name: String,
    val description: String,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val reverbPreset: Short? = null,
    val reverbStrength: Float = 0.5f,
    val bassBoost: Boolean = false,
    val virtualizer: Boolean = false,
    val dynamics: Boolean = false,
    val ambientSound: AmbientSoundManager.SoundType? = null,
    val ambientVolume: Float = 0.4f
)

object ComaPresets {
    val all = listOf(
        ComaPreset(
            id = "coma_classic",
            name = "COMA Classic",
            description = "تجربه کلاسیک",
            speed = 0.85f,
            pitch = 1.0f,
            reverbPreset = PresetReverb.PRESET_LARGEROOM,
            reverbStrength = 0.6f,
            bassBoost = true
        ),
        ComaPreset(
            id = "coma_dream",
            name = "COMA Dream",
            description = "رویایی و آرام",
            speed = 0.75f,
            pitch = 0.95f,
            reverbPreset = PresetReverb.PRESET_PLATE,
            reverbStrength = 0.8f,
            virtualizer = true,
            ambientSound = AmbientSoundManager.SoundType.RAIN,
            ambientVolume = 0.35f
        ),
        ComaPreset(
            id = "coma_night",
            name = "COMA Night",
            description = "شب‌های آرام",
            speed = 0.8f,
            pitch = 0.9f,
            reverbPreset = PresetReverb.PRESET_LARGEROOM,
            reverbStrength = 0.7f,
            ambientSound = AmbientSoundManager.SoundType.THUNDER,
            ambientVolume = 0.3f
        ),
        ComaPreset(
            id = "coma_deep",
            name = "COMA Deep",
            description = "بیس عمیق",
            speed = 0.7f,
            pitch = 0.85f,
            reverbPreset = PresetReverb.PRESET_LARGEROOM,
            reverbStrength = 0.9f,
            bassBoost = true,
            dynamics = true,
            ambientSound = AmbientSoundManager.SoundType.WIND,
            ambientVolume = 0.25f
        ),
        ComaPreset(
            id = "coma_8d",
            name = "COMA 8D",
            description = "چرخش ۸ بعدی",
            speed = 1.0f,
            pitch = 1.0f,
            virtualizer = true,
            reverbPreset = PresetReverb.PRESET_MEDIUMROOM,
            reverbStrength = 0.5f
        ),
        ComaPreset(
            id = "coma_slowed",
            name = "COMA Slowed",
            description = "کلاسیک Slowed",
            speed = 0.85f,
            pitch = 0.95f,
            reverbPreset = PresetReverb.PRESET_PLATE,
            reverbStrength = 0.75f
        ),
        ComaPreset(
            id = "coma_nightcore",
            name = "COMA Nightcore",
            description = "تند و روشن",
            speed = 1.25f,
            pitch = 1.15f,
            reverbPreset = PresetReverb.PRESET_SMALLROOM,
            reverbStrength = 0.3f
        ),
        ComaPreset(
            id = "coma_cafe",
            name = "COMA Café",
            description = "حس کافه دنج",
            speed = 0.9f,
            pitch = 1.0f,
            reverbPreset = PresetReverb.PRESET_MEDIUMROOM,
            reverbStrength = 0.4f,
            ambientSound = AmbientSoundManager.SoundType.FOREST,
            ambientVolume = 0.3f
        )
    )
}
