package com.joijukik.remixlab

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AmbientSoundManager {

    enum class SoundType { RAIN, WIND, THUNDER, FOREST, CAFE }

    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile private var isRunning = false
    @Volatile private var currentType: SoundType? = null
    @Volatile private var volume: Float = 0.4f

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    fun play(type: SoundType, volume: Float = 0.4f) {
        stop()
        currentType = type
        this.volume = volume.coerceIn(0f, 1f)
        isRunning = true

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        job = scope.launch { generateLoop() }
    }

    private suspend fun generateLoop() {
        val buffer = ShortArray(bufferSize / 2)
        var pink = 0.0

        while (isRunning && currentCoroutineContext().isActive) {
            val type = currentType ?: break
            for (i in buffer.indices) {
                val white = Random.nextDouble(-1.0, 1.0)
                val sample: Double = when (type) {
                    SoundType.RAIN -> {
                        pink = pink * 0.95 + white * 0.05
                        pink * 0.35
                    }
                    SoundType.WIND -> {
                        pink = pink * 0.98 + white * 0.02
                        val mod = sin(System.currentTimeMillis() / 3000.0 * 2 * PI) * 0.3 + 0.7
                        pink * 0.4 * mod
                    }
                    SoundType.THUNDER -> {
                        pink = pink * 0.95 + white * 0.05
                        val rumble = if (Random.nextInt(5000) == 0) sin(i.toDouble()) * 0.5 else 0.0
                        pink * 0.25 + rumble * 0.4
                    }
                    SoundType.FOREST -> {
                        pink = pink * 0.97 + white * 0.03
                        val chirp = if (Random.nextInt(5000) == 0)
                            sin(System.currentTimeMillis() / 20.0 * 2 * PI) * 0.3 else 0.0
                        pink * 0.3 + chirp
                    }
                    SoundType.CAFE -> {
                        pink = pink * 0.9 + white * 0.1
                        val murmur = sin(System.currentTimeMillis() / 500.0 * 2 * PI) * 0.05
                        pink * 0.35 + murmur
                    }
                }
                buffer[i] = (sample * Short.MAX_VALUE * volume).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            audioTrack?.write(buffer, 0, buffer.size)
            delay(5)
        }
    }

    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }

    fun stop() {
        isRunning = false
        job?.cancel()
        job = null
        try { audioTrack?.stop(); audioTrack?.release() } catch (_: Exception) {}
        audioTrack = null
        currentType = null
    }

    fun release() { stop(); scope.cancel() }
}
