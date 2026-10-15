package com.joijukik.remixlab.data

import androidx.media3.common.MediaItem

/**
 * مدیریت لیست ترک‌های میکس (بدون CompositionPlayer).
 * (نسخه ساده — فقط داده‌ها رو نگه می‌داره)
 */
class MixManager {

    data class MixTrack(
        val id: String,
        val mediaItem: MediaItem,
        var volume: Float = 1.0f,
        var enabled: Boolean = true
    )

    private val tracks = mutableListOf<MixTrack>()

    fun addTrack(mediaItem: MediaItem, trackId: String): MixTrack {
        val track = MixTrack(trackId, mediaItem)
        tracks.add(track)
        return track
    }

    fun removeTrack(trackId: String) {
        tracks.removeAll { it.id == trackId }
    }

    fun setTrackVolume(trackId: String, volume: Float) {
        tracks.find { it.id == trackId }?.volume = volume.coerceIn(0f, 2f)
    }

    fun setTrackEnabled(trackId: String, enabled: Boolean) {
        tracks.find { it.id == trackId }?.enabled = enabled
    }

    fun getTracks(): List<MixTrack> = tracks.toList()

    fun clear() {
        tracks.clear()
    }
}
