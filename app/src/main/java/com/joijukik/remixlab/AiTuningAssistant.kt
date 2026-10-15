package com.joijukik.remixlab

import org.json.JSONObject

/**
 * دستیار AI برای تبدیل توضیحات متنی به پارامترهای صوتی
 * از یک LLM (مثلاً DeepSeek API) برای تحلیل درخواست استفاده می‌کند
 */
class AiTuningAssistant(private val apiKey: String) {

    data class TuningResult(
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f,
        val reverb: Boolean = false,
        val bassBoost: Boolean = false,
        val virtualizer: Boolean = false,
        val dynamicsEnabled: Boolean = false,
        val ambientSound: AmbientSoundManager.SoundType? = null,
        val ambientVolume: Float = 0.3f
    )

    /**
     * درخواست کاربر را تحلیل و به پارامترها تبدیل می‌کند
     * مثال: "این آهنگ رو کمایی و کند کن، صدای بارون هم پخش کن"
     */
    fun analyzeRequest(userPrompt: String): TuningResult {
        // در پیاده‌سازی واقعی، اینجا درخواست به LLM ارسال می‌شود
        // و پاسخ JSON解析 می‌شود
        return parseResponse("{}")
    }

    private fun parseResponse(json: String): TuningResult {
        return try {
            val obj = JSONObject(json)
            TuningResult(
                speed = obj.optDouble("speed", 1.0).toFloat(),
                pitch = obj.optDouble("pitch", 1.0).toFloat(),
                reverb = obj.optBoolean("reverb", false),
                bassBoost = obj.optBoolean("bassBoost", false),
                virtualizer = obj.optBoolean("virtualizer", false),
                dynamicsEnabled = obj.optBoolean("dynamics", false),
                ambientSound = null, //解析 از رشته
                ambientVolume = obj.optDouble("ambientVolume", 0.3).toFloat()
            )
        } catch (e: Exception) {
            TuningResult()
        }
    }

    /**
     * پرامپت پیشنهادی برای ارسال به LLM
     */
    fun buildSystemPrompt(): String {
        return """
            تو یک مهندس صدا هستی. کاربر می‌خواهد ریمیکس بسازد.
            درخواست او را به پارامترهای زیر تبدیل کن و به صورت JSON برگردان:
            {
              "speed": عدد بین 0.5 تا 2.0,
              "pitch": عدد بین 0.5 تا 2.0,
              "reverb": true/false,
              "bassBoost": true/false,
              "virtualizer": true/false,
              "dynamics": true/false,
              "ambientSound": "RAIN" | "HEAVY_RAIN" | "WIND" | "THUNDER" | "NOISE" | null,
              "ambientVolume": عدد بین 0 تا 1
            }
            
            مثال:
            - "کمایی و کند کن" -> {"speed": 0.65, "pitch": 0.92, "reverb": true, "bassBoost": true}
            - "بارون پخش کن" -> {"ambientSound": "RAIN", "ambientVolume": 0.3}
            - "صدای 8D" -> {"virtualizer": true}
        """.trimIndent()
    }
}
