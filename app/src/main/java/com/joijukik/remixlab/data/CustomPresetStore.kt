package com.joijukik.remixlab.data

import android.content.Context
import org.json.JSONObject

/**
 * ذخیرهسازی پریستهای سفارشی کاربر.
 * (نسخه ساده - فقط برای نمایش)
 */
class CustomPresetStore(context: Context) {

    private val prefs = context.getSharedPreferences("remixlab_presets", Context.MODE_PRIVATE)

    fun savePreset(id: String, name: String, data: String) {
        val json = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("data", data)
        }
        prefs.edit().putString("preset_$id", json.toString()).apply()
    }

    fun getAllNames(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith("preset_") }
            .mapNotNull { key ->
                try {
                    JSONObject(prefs.getString(key, "") ?: "").optString("name")
                } catch (e: Exception) { null }
            }
    }

    fun deletePreset(id: String) {
        prefs.edit().remove("preset_$id").apply()
    }
}
