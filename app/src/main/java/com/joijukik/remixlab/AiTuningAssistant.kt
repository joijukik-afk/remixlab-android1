package com.joijukik.remixlab

object AiTuningAssistant {

    data class TuningResult(
        val detectedKeywords: List<String>,
        val explanation: String
    )

    fun analyzeRequest(prompt: String): TuningResult {
        val lower = prompt.lowercase()
        val detected = mutableListOf<String>()

        if (lower.contains("بارون") || lower.contains("rain")) detected += "بارون"
        if (lower.contains("شب") || lower.contains("night")) detected += "شب"
        if (lower.contains("تند") || lower.contains("nightcore")) detected += "تند"
        if (lower.contains("کند") || lower.contains("slowed")) detected += "کند"
        if (lower.contains("غرق") || lower.contains("deep")) detected += "عمیق"
        if (lower.contains("8d") || lower.contains("چرخش")) detected += "8D"
        if (lower.contains("ریورب") || lower.contains("reverb")) detected += "ریورب"
        if (lower.contains("بیس") || lower.contains("bass")) detected += "بیس"

        val explanation = if (detected.isEmpty())
            "تنظیمات پیش‌فرض اعمال شد"
        else
            "تشخیص: ${detected.joinToString("، ")}"

        return TuningResult(detected, explanation)
    }
}
