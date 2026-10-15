package com.joijukik.remixlab

/**
 * دستیار هوشمند تنظیم ریمیکس
 * با استفاده از تطبیق کلمات کلیدی فارسی و انگلیسی
 * بدون نیاز به API خارجی، کامل آفلاین
 */
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

    /**
     * تحلیل درخواست کاربر و تبدیل به پارامترهای صوتی
     */
    fun analyzeRequest(prompt: String): TuningResult {
        val text = prompt.lowercase().trim()
        val detected = mutableListOf<String>()

        // مقادیر پیش‌فرض
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

        // ═══════════════════════════════════════════════════
        // ۱. تشخیص حالت کمایی (Deep Coma)
        // ═══════════════════════════════════════════════════
        if (text.containsAny("کمایی", "کما", "coma", "کمای عمیق")) {
            detected.add("کمایی")
            speed = 0.65f
            pitch = 0.92f
            reverb = true
            reverbPreset = android.media.audiofx.PresetReverb.PRESET_LARGEHALL
            bassBoost = true
            ambientSound = AmbientSoundManager.SoundType.RAIN
            ambientVolume = 0.25f
            suggestedPreset = "Deep Coma"
        }

        // ═══════════════════════════════════════════════════
        // ۲. تشخیص حالت دریم‌کور
        // ═══════════════════════════════════════════════════
        if (text.containsAny("دریم", "dreamcore", "خواب", "رویا", "کابوس")) {
            detected.add("دریم‌کور")
            speed = 0.72f
            pitch = 0.98f
            reverb = true
            reverbPreset = android.media.audiofx.PresetReverb.PRESET_LARGEHALL
            virtualizer = true
            ambientSound = AmbientSoundManager.SoundType.WIND
            ambientVolume = 0.3f
            suggestedPreset = "Dark Dreamcore"
        }

        // ═══════════════════════════════════════════════════
        // ۳. تشخیص کند کردن (Slowed)
        // ═══════════════════════════════════════════════════
        if (text.containsAny("کند", "آهسته", "slowed", "slow", "یواش")) {
            detected.add("کند")
            if (speed == 1.0f) speed = 0.8f
            reverb = true
            if (reverbPreset == null) {
                reverbPreset = android.media.audiofx.PresetReverb.PRESET_LARGEHALL
            }
        }

        // ═══════════════════════════════════════════════════
        // ۴. تشخیص تند کردن (Nightcore)
        // ═══════════════════════════════════════════════════
        if (text.containsAny("تند", "سریع", "nightcore", "fast", "نایتکور")) {
            detected.add("تند")
            speed = 1.25f
            pitch = 1.15f
        }

        // ═══════════════════════════════════════════════════
        // ۵. تشخیص گام (Pitch)
        // ═══════════════════════════════════════════════════
        if (text.containsAny("بم", "بمتر", "پایین", "low pitch", "deep voice")) {
            detected.add("گام بم")
            if (pitch == 1.0f) pitch = 0.9f
        }
        if (text.containsAny("تیز", "زیر", "بالا", "high pitch")) {
            detected.add("گام تیز")
            if (pitch == 1.0f) pitch = 1.1f
        }

        // ═══════════════════════════════════════════════════
        // ۶. تشخیص Reverb / طنین
        // ═══════════════════════════════════════════════════
        if (text.containsAny("طنین", "ریورب", "reverb", "echo", "اکو", "فضا")) {
            detected.add("طنین")
            reverb = true
            if (reverbPreset == null) {
                reverbPreset = android.media.audiofx.PresetReverb.PRESET_LARGEHALL
            }
        }

        // ═══════════════════════════════════════════════════
        // ۷. تشخیص Bass Boost
        // ═══════════════════════════════════════════════════
        if (text.containsAny("باس", "bass", "کوبنده", "سنگین")) {
            detected.add("تقویت باس")
            bassBoost = true
        }

        // ═══════════════════════════════════════════════════
        // ۸. تشخیص 8D Audio
        // ═══════════════════════════════════════════════════
        if (text.containsAny("8d", "هشت بعدی", "فضایی", "چرخش", "spatial")) {
            detected.add("8D")
            virtualizer = true
        }

        // ═══════════════════════════════════════════════════
        // ۹. تشخیص کیفیت استودیویی
        // ═══════════════════════════════════════════════════
        if (text.containsAny("استودیو", "کیفیت", "حرفه", "professional", "studio", "تمیز")) {
            detected.add("کیفیت استودیویی")
            dynamics = true
        }

        // ═══════════════════════════════════════════════════
        // ۱۰. تشخیص صداهای محیطی
        // ═══════════════════════════════════════════════════
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

        // ═══════════════════════════════════════════════════
        // ۱۱. تشخیص حالت تاریک
        // ═══════════════════════════════════════════════════
        if (text.containsAny("تاریک", "dark", "غمگین", "غم", "سرد")) {
            detected.add("تاریک")
            if (speed == 1.0f) speed = 0.75f
            if (pitch == 1.0f) pitch = 0.95f
            reverb = true
            if (reverbPreset == null) {
                reverbPreset = android.media.audiofx.PresetReverb.PRESET_LARGEHALL
            }
            if (ambientSound == null) {
                ambientSound = AmbientSoundManager.SoundType.WIND
            }
        }

        // ═══════════════════════════════════════════════════
        // ۱۲. تشخیص پایان سینمایی
        // ═══════════════════════════════════════════════════
        if (text.containsAny("پایان", "سینمایی", "outro", "cinematic", "احساسی")) {
            detected.add("پایان سینمایی")
            speed = 0.9f
            reverb = true
            if (reverbPreset == null) {
                reverbPreset = android.media.audiofx.PresetReverb.PRESET_MEDIUMHALL
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

    /**
     * بررسی می‌کنه آیا متن شامل هر یک از کلمات کلیدی هست
     */
    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
