package com.joijukik.remixlab

object AiTuningAssistant {

    data class TuningResult(
        val detectedKeywords: List<String>,
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f,
        val reverb: Boolean = false,
        val bassBoost: Boolean = false,
        val virtualizer: Boolean = false,
        val dynamics: Boolean = false,
        val explanation: String = ""
    )

    /**
     * تحلیل ساده متن ورودی و برگرداندن تنظیمات پیشنهادی.
     * (بدون نیاز به API خارجی)
     */
    fun analyzeRequest(prompt: String): TuningResult {
        val lower = prompt.lowercase()
        val detected = mutableListOf<String>()

        var speed = 1.0f
        var pitch = 1.0f
        var reverb = false
        var bassBoost = false
        var virtualizer = false
        var dynamics = false
        val explanations = mutableListOf<String>()

        // تشخیص کلیدواژه‌ها
        if (lower.contains("بارون") || lower.contains("rain") || lower.contains("بارانی")) {
            detected += "بارون"
            speed = 0.85f
            pitch = 0.95f
            reverb = true
            explanations += "فضای بارونی با ریورب ملایم"
        }
        if (lower.contains("شب") || lower.contains("night")) {
            detected += "شب"
            speed = 0.8f
            reverb = true
            bassBoost = true
            explanations += "فضای شبانه با بیس گرم"
        }
        if (lower.contains("تند") || lower.contains("nightcore") || lower.contains("سریع")) {
            detected += "تند"
            speed = 1.25f
            pitch = 1.15f
            explanations += "سرعت و گام بالا"
        }
        if (lower.contains("کند") || lower.contains("slowed") || lower.contains("آروم")) {
            detected += "کند"
            speed = 0.8f
            pitch = 0.95f
            explanations += "حالت Slowed"
        }
        if (lower.contains("غرق") || lower.contains("deep") || lower.contains("عمیق")) {
            detected += "عمیق"
            speed = 0.7f
            bassBoost = true
            dynamics = true
            explanations += "بیس عمیق و دینامیک استودیویی"
        }
        if (lower.contains("8d") || lower.contains("چرخش") || lower.contains("دور سر")) {
            detected += "8D"
            virtualizer = true
            explanations += "افکت چرخش ۸ بعدی"
        }
        if (lower.contains("ریورب") || lower.contains("reverb") || lower.contains("پژواک")) {
            detected += "ریورب"
            reverb = true
            explanations += "ریورب فعال"
        }
        if (lower.contains("بیس") || lower.contains("bass")) {
            detected += "بیس"
            bassBoost = true
            explanations += "تقویت بیس"
        }

        val explanation = if (explanations.isEmpty())
            "تنظیمات پیش‌فرض اعمال شد"
        else
            explanations.joinToString(" • ")

        return TuningResult(
            detectedKeywords = detected,
            speed = speed,
            pitch = pitch,
            reverb = reverb,
            bassBoost = bassBoost,
            virtualizer = virtualizer,
            dynamics = dynamics,
            explanation = explanation
        )
    }
}
