package com.joijukik.remixlab

import android.media.audiofx.PresetReverb

data class ComaPreset(
    val id: String,
    val name: String,
    val description: String,
    val speed: Float,
    val pitch: Float,
    val reverbPreset: Short?,
    val bassBoost: Boolean,
    val virtualizer: Boolean,
    val ambientSound: AmbientSoundManager.SoundType? = null,
    val ambientVolume: Float = 0.35f
)

object ComaPresets {

    val all: List<ComaPreset> = listOf(
        ComaPreset(
            id = "slowed_reverb",
            name = "Slowed + Reverb",
            description = "کند و طنین‌دار",
            speed = 0.82f,
            pitch = 1.0f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = false,
            virtualizer = false
        ),
        ComaPreset(
            id = "deep_coma",
            name = "Deep Coma",
            description = "کمای عمیق و سنگین",
            speed = 0.65f,
            pitch = 0.92f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = true,
            virtualizer = false,
            ambientSound = AmbientSoundManager.SoundType.RAIN,
            ambientVolume = 0.25f
        ),
        ComaPreset(
            id = "half_asleep",
            name = "Half-Asleep Vocal",
            description = "وکال نیمه‌خواب و دور",
            speed = 0.80f,
            pitch = 0.95f,
            reverbPreset = PresetReverb.PRESET_MEDIUMHALL,
            bassBoost = false,
            virtualizer = false
        ),
        ComaPreset(
            id = "dark_dreamcore",
            name = "Dark Dreamcore",
            description = "دریم‌کور تاریک و فضایی",
            speed = 0.72f,
            pitch = 0.98f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = false,
            virtualizer = true,
            ambientSound = AmbientSoundManager.SoundType.WIND,
            ambientVolume = 0.3f
        ),
        ComaPreset(
            id = "broken_tape",
            name = "Broken Tape",
            description = "نوار خراب و آنالوگ",
            speed = 0.88f,
            pitch = 0.96f,
            reverbPreset = PresetReverb.PRESET_MEDIUMROOM,
            bassBoost = false,
            virtualizer = false,
            ambientSound = AmbientSoundManager.SoundType.NOISE,
            ambientVolume = 0.2f
        ),
        ComaPreset(
            id = "underwater_echo",
            name = "Underwater Echo",
            description = "اکوی زیر آب",
            speed = 0.83f,
            pitch = 0.94f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = true,
            virtualizer = false
        ),
        ComaPreset(
            id = "heartbeat_dark",
            name = "Heartbeat in the Dark",
            description = "ضربان در تاریکی",
            speed = 0.78f,
            pitch = 1.0f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = true,
            virtualizer = false,
            ambientSound = AmbientSoundManager.SoundType.THUNDER,
            ambientVolume = 0.15f
        ),
        ComaPreset(
            id = "cinematic_outro",
            name = "Cinematic Outro",
            description = "پایان سینمایی و احساسی",
            speed = 0.90f,
            pitch = 1.0f,
            reverbPreset = PresetReverb.PRESET_MEDIUMHALL,
            bassBoost = false,
            virtualizer = false,
            ambientSound = AmbientSoundManager.SoundType.RAIN,
            ambientVolume = 0.15f
        ),
        ComaPreset(
            id = "midnight_rain",
            name = "Midnight Rain",
            description = "باران نیمه‌شب",
            speed = 0.75f,
            pitch = 0.96f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = true,
            virtualizer = true,
            ambientSound = AmbientSoundManager.SoundType.HEAVY_RAIN,
            ambientVolume = 0.35f
        ),
        ComaPreset(
            id = "faded_memory",
            name = "Faded Memory",
            description = "خاطره محو شده",
            speed = 0.70f,
            pitch = 0.90f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = false,
            virtualizer = true,
            ambientSound = AmbientSoundManager.SoundType.WIND,
            ambientVolume = 0.25f
        )
    )
}
