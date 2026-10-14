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
    val virtualizer: Boolean
)

object ComaPresets {

    val all: List<ComaPreset> = listOf(
        ComaPreset(
            id = "slowed_reverb",
            name = "Slowed + Reverb",
            description = "کند و طنین‌دار",
            speed = 0.8f,
            pitch = 1.0f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = false,
            virtualizer = false
        ),
        ComaPreset(
            id = "deep_coma",
            name = "Deep Coma",
            description = "کمای عمیق و سنگین",
            speed = 0.7f,
            pitch = 0.95f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = true,
            virtualizer = false
        ),
        ComaPreset(
            id = "half_asleep",
            name = "Half-Asleep Vocal",
            description = "وکال نیمه‌خواب و دور",
            speed = 0.85f,
            pitch = 0.98f,
            reverbPreset = PresetReverb.PRESET_MEDIUMHALL,
            bassBoost = false,
            virtualizer = false
        ),
        ComaPreset(
            id = "dark_dreamcore",
            name = "Dark Dreamcore",
            description = "دریم‌کور تاریک و فضایی",
            speed = 0.75f,
            pitch = 1.0f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = false,
            virtualizer = true
        ),
        ComaPreset(
            id = "broken_tape",
            name = "Broken Tape",
            description = "نوار خراب و آنالوگ",
            speed = 0.9f,
            pitch = 0.97f,
            reverbPreset = PresetReverb.PRESET_MEDIUMROOM,
            bassBoost = false,
            virtualizer = false
        ),
        ComaPreset(
            id = "underwater_echo",
            name = "Underwater Echo",
            description = "اکوی زیر آب",
            speed = 0.85f,
            pitch = 0.95f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = true,
            virtualizer = false
        ),
        ComaPreset(
            id = "heartbeat_dark",
            name = "Heartbeat in the Dark",
            description = "ضربان در تاریکی",
            speed = 0.8f,
            pitch = 1.0f,
            reverbPreset = PresetReverb.PRESET_LARGEHALL,
            bassBoost = true,
            virtualizer = false
        ),
        ComaPreset(
            id = "cinematic_outro",
            name = "Cinematic Outro",
            description = "پایان سینمایی و احساسی",
            speed = 0.9f,
            pitch = 1.0f,
            reverbPreset = PresetReverb.PRESET_MEDIUMHALL,
            bassBoost = false,
            virtualizer = false
        )
    )
}
