package com.joijukik.remixlab.data

import androidx.media3.common.Effects
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence

class MixManager {

    data class MixTrack(
        val id: String,
        val mediaItem: MediaItem,
        var volume: Float = 1.0f,
        var enabled: Boolean = true
    )

    private val tracks = mutableListOf<MixTrack>()
    private val trackProcessors = mutableMapOf<String, ChannelMixingAudioProcessor>()

    fun addTrack(mediaItem: MediaItem, trackId: String): MixTrack {
        val track = MixTrack(trackId, mediaItem)
        tracks.add(track)
        trackProcessors[trackId] = createMixProcessor()
        return track
    }

    fun removeTrack(trackId: String) {
        tracks.removeAll { it.id == trackId }
        trackProcessors.remove(trackId)
    }

    fun setTrackVolume(trackId: String, volume: Float) {
        tracks.find { it.id == trackId }?.volume = volume.coerceIn(0f, 2f)
    }

    fun setTrackEnabled(trackId: String, enabled: Boolean) {
        tracks.find { it.id == trackId }?.enabled = enabled
    }

    fun buildComposition(): Composition? {
        val activeTracks = tracks.filter { it.enabled }
        if (activeTracks.isEmpty()) return null

        val sequences = activeTracks.map { track ->
            val processor = trackProcessors[track.id] ?: createMixProcessor()
            val editedItem = EditedMediaItem.Builder(track.mediaItem)
                .setEffects(Effects(listOf(processor), emptyList()))
                .build()
            EditedMediaItemSequence.Builder(editedItem).build()
        }

        return Composition.Builder(sequences).build()
    }

    private fun createMixProcessor(): ChannelMixingAudioProcessor {
        return ChannelMixingAudioProcessor(ChannelMixingMatrix.create(2, 2))
    }

    fun getTracks(): List<MixTrack> = tracks.toList()

    fun clear() {
        tracks.clear()
        trackProcessors.clear()
    }
}
