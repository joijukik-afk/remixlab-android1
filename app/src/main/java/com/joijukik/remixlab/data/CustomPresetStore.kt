package com.joijukik.remixlab.data

import android.content.Context
import org.json.JSONObject

/**
 * ذخیره‌سازی پریست‌های سفارشی کاربر.
 */
class CustomPresetStore(context: Context) {

    private val prefs = context.getSharedPreferences("remixlab_presets", Context.MODE_PRIVATE)

    data class PresetData(
        val id: String,
        val name: String,
        val data: String
    )

    fun savePreset(id: String, name: String, data: String) {
        val json = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("data", data)
        }
        prefs.edit().putString("preset_$id", json.toString()).apply()
    }

    fun getAllPresets(): List<PresetData> {
        return prefs.all.keys
            .filter { it.startsWith("preset_") }
            .mapNotNull { key ->
                try {
                    val json = JSONObject(prefs.getString(key, "") ?: "")
                    PresetData(
                        id = json.optString("id"),
                        name = json.optString("name"),
                        data = json.optString("data")
                    )
                } catch (e: Exception) { null }
            }
    }

    fun getPreset(id: String): PresetData? {
        val str = prefs.getString("preset_$id", null) ?: return null
        return try {
            val json = JSONObject(str)
            PresetData(
                id = json.optString("id"),
                name = json.optString("name"),
                data = json.optString("data")
            )
        } catch (e: Exception) { null }
    }

    fun deletePreset(id: String) {
        prefs.edit().remove("preset_$id").apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
