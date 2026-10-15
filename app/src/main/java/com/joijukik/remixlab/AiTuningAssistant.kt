package com.joijukik.remixlab

import android.media.audiofx.PresetReverb

object AiTuningAssistant {

    data class TuningResult(
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f,
        val reverb: Boolean = false,
        val reverbPreset: Short? = null,
        val bassBoost: Boolean = false,
        val virtualizer: Boolean = false,
        val dynamics: Boolean = false,
        val ambientSound: AmbientSoundManager.SoundType? = null,
        val ambientVolume: Float = 0.3f,
        val detectedKeywords: List<String> = emptyList(),
        val suggestedPreset: String? = null
    )

    fun analyzeRequest(prompt: String): TuningResult {
        val text = prompt.lowercase().trim()
        val detected = mutableListOf<String>()

        var speed = 1.0f
        var pitch = 1.0f
        var reverb = false
        var reverbPreset: Short? = null
        var bassBoost = false
        var virtualizer = false
        var dynamics = false
        var ambientSound: AmbientSoundManager.SoundType? = null
        var ambientVolume = 0.3f
        var suggestedPreset: String? = null

        if (text.containsAny("کمایی", "کما", "coma")) {
            detected.add("کمایی")
            speed = 0.65f
            pitch = 0.92f
            reverb = true
            reverbPreset = PresetReverb.PRESET_LARGEHALL
            bassBoost = true
            ambientSound = AmbientSoundManager.SoundType.RAIN
            ambientVolume = 0.25f
            suggestedPreset = "Deep Coma"
        }

        if (text.containsAny("دریم", "dreamcore", "خواب", "رویا", "کابوس")) {
            detected.add("دریم‌کور")
            speed = 0.72f
            pitch = 0.98f
            reverb = true
            reverbPreset = PresetReverb.PRESET_LARGEHALL
            virtualizer = true
            ambientSound = AmbientSoundManager.SoundType.WIND
            ambientVolume = 0.3f
            suggestedPreset = "Dark Dreamcore"
        }

        if (text.containsAny("کند", "آهسته", "slowed", "slow", "یواش")) {
            detected.add("کند")
            if (speed == 1.0f) speed = 0.8f
            reverb = true
            if (reverbPreset == null) {
                reverbPreset = PresetReverb.PRESET_LARGEHALL
            }
        }

        if (text.containsAny("تند", "سریع", "nightcore", "fast", "نایتکور")) {
            detected.add("تند")
            speed = 1.25f
            pitch = 1.15f
        }

        if (text.containsAny("بم", "بمتر", "پایین", "low pitch", "deep voice")) {
            detected.add("گام بم")
            if (pitch == 1.0f) pitch = 0.9f
        }

        if (text.containsAny("تیز", "زیر", "بالا", "high pitch")) {
            detected.add("گام تیز")
            if (pitch == 1.0f) pitch = 1.1f
        }

        if (text.containsAny("طنین", "ریورب", "reverb", "اکو", "فضا")) {
            detected.add("طنین")
            reverb = true
            if (reverbPreset == null) {
                reverbPreset = PresetReverb.PRESET_LARGEHALL
            }
        }

        if (text.containsAny("باس", "bass", "کوبنده", "سنگین")) {
            detected.add("تقویت باس")
            bassBoost = true
        }

        if (text.containsAny("8d", "هشت بعدی", "فضایی", "چرخش", "spatial")) {
            detected.add("8D")
            virtualizer = true
        }

        if (text.containsAny("استودیو", "کیفیت", "حرفه", "studio", "تمیز")) {
            detected.add("کیفیت استودیویی")
            dynamics = true
        }

        if (text.containsAny("بارون", "باران", "rain")) {
            detected.add("باران")
            if (text.containsAny("شدید", "heavy", "تند")) {
                ambientSound = AmbientSoundManager.SoundType.HEAVY_RAIN
            } else {
                ambientSound = AmbientSoundManager.SoundType.RAIN
            }
            ambientVolume = 0.3f
        }

        if (text.containsAny("رعد", "thunder", "طوفان", "storm")) {
            detected.add("رعد")
            ambientSound = AmbientSoundManager.SoundType.THUNDER
            ambientVolume = 0.2f
        }

        if (text.containsAny("باد", "wind", "نسیم")) {
            detected.add("باد")
            ambientSound = AmbientSoundManager.SoundType.WIND
            ambientVolume = 0.3f
        }

        if (text.containsAny("نویز", "noise", "نوار", "کاست", "خش")) {
            detected.add("نویز آنالوگ")
            ambientSound = AmbientSoundManager.SoundType.NOISE
            ambientVolume = 0.2f
        }

        if (text.containsAny("تاریک", "dark", "غمگین", "غم", "سرد")) {
            detected.add("تاریک")
            if (speed == 1.0f) speed = 0.75f
            if (pitch == 1.0f) pitch = 0.95f
            reverb = true
            if (reverbPreset == null) {
                reverbPreset = PresetReverb.PRESET_LARGEHALL
            }
            if (ambientSound == null) {
                ambientSound = AmbientSoundManager.SoundType.WIND
            }
        }

        if (text.containsAny("پایان", "سینمایی", "outro", "cinematic")) {
            detected.add("پایان سینمایی")
            speed = 0.9f
            reverb = true
            if (reverbPreset == null) {
                reverbPreset = PresetReverb.PRESET_MEDIUMHALL
            }
            suggestedPreset = "Cinematic Outro"
        }

        return TuningResult(
            speed = speed,
            pitch = pitch,
            reverb = reverb,
            reverbPreset = reverbPreset,
            bassBoost = bassBoost,
            virtualizer = virtualizer,
            dynamics = dynamics,
            ambientSound = ambientSound,
            ambientVolume = ambientVolume,
            detectedKeywords = detected,
            suggestedPreset = suggestedPreset
        )
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
