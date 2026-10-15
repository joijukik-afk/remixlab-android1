package com.remixlab.app.data

import android.content.Context
import com.remixlab.app.model.AmbientType
import com.remixlab.app.model.ComaPreset
import org.json.JSONObject

class CustomPresetStore(context: Context) {

    private val prefs = context.getSharedPreferences("remixlab_presets", Context.MODE_PRIVATE)

    fun savePreset(preset: ComaPreset) {
        val json = JSONObject().apply {
            put("id", preset.id)
            put("name", preset.name)
            put("description", preset.description)
            put("speed", preset.speed.toDouble())
            put("pitch", preset.pitch.toDouble())
            put("reverbPreset", preset.reverbPreset?.toInt() ?: -1)
            put("bassBoost", preset.bassBoost)
            put("virtualizer", preset.virtualizer)
            put("dynamics", preset.dynamics)
            put("ambientSound", preset.ambientSound?.name ?: "")
            put("ambientVolume", preset.ambientVolume.toDouble())
            put("icon", preset.icon)
        }
        prefs.edit().putString("preset_${preset.id}", json.toString()).apply()
    }

    fun loadPreset(id: String): ComaPreset? {
        val str = prefs.getString("preset_$id", null) ?: return null
        return try {
            val j = JSONObject(str)
            val ambStr = j.optString("ambientSound", "")
            ComaPreset(
                id = j.getString("id"),
                name = j.getString("name"),
                description = j.optString("description", ""),
                speed = j.optDouble("speed", 1.0).toFloat(),
                pitch = j.optDouble("pitch", 1.0).toFloat(),
                reverbPreset = j.optInt("reverbPreset", -1).let {
                    if (it == -1) null else it.toShort()
                },
                bassBoost = j.optBoolean("bassBoost", false),
                virtualizer = j.optBoolean("virtualizer", false),
                dynamics = j.optBoolean("dynamics", false),
                ambientSound = if (ambStr.isBlank()) null else
                    runCatching { AmbientType.valueOf(ambStr) }.getOrNull(),
                ambientVolume = j.optDouble("ambientVolume", 0.4).toFloat(),
                icon = j.optString("icon", "⭐")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getAll(): List<ComaPreset> {
        return prefs.all.keys
            .filter { it.startsWith("preset_") }
            .mapNotNull { loadPreset(it.removePrefix("preset_")) }
    }

    fun deletePreset(id: String) {
        prefs.edit().remove("preset_$id").apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
