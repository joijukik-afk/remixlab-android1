package com.joijukik.remixlab

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.random
import kotlin.math.sin

/**
 * تولید و پخش صداهای محیطی (باران، باد، رعد و...)
 * بدون نیاز به فایل صوتی - کاملاً در کد ساخته می‌شه
 */
class AmbientSoundManager {

    private var audioTrack: AudioTrack? = null
    private var generatorThread: Thread? = null

    @Volatile private var isPlaying = false
    @Volatile private var soundType: SoundType = SoundType.RAIN
    @Volatile private var volume: Float = 0.5f

    // فیلتر low-pass ساده برای نرم کردن نویز
    private var lowPassBuffer = 0.0f
    private val lowPassAlpha = 0.05f

    // بافر برای قطره‌های باران (drop hits)
    private var dropBuffer = FloatArray(64)
    private var dropIndex = 0

    enum class SoundType {
        RAIN,          // باران
        HEAVY_RAIN,    // باران شدید
        WIND,          // باد
        THUNDER,       // رعد (بصری)
        NOISE          // نویز آنالوگ
    }

    // --- API عمومی ---

    fun play(type: SoundType, volume: Float = 0.5f) {
        stop()
        this.soundType = type
        this.volume = volume.coerceIn(0f, 1f)
        startAudioTrack()
    }

    fun stop() {
        isPlaying = false
        generatorThread?.join(500)
        generatorThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) { }
        audioTrack = null
    }

    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(this.volume)
        } catch (_: Exception) { }
    }

    fun isRunning(): Boolean = isPlaying

    // --- پیاده‌سازی داخلی ---

    private fun startAudioTrack() {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 4) // float = 4 bytes
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(volume)
            audioTrack?.play()
            isPlaying = true

            generatorThread = thread {
                generateAndWriteAudio(sampleRate, bufferSize)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    private fun generateAndWriteAudio(sampleRate: Int, bufferSize: Int) {
        val floatBuffer = FloatArray(bufferSize * 2) // stereo

        while (isPlaying) {
            for (i in 0 until bufferSize) {
                val (left, right) = generateSample(sampleRate, i)
                floatBuffer[i * 2] = left
                floatBuffer[i * 2 + 1] = right
            }

            try {
                audioTrack?.write(floatBuffer, 0, floatBuffer.size, AudioTrack.WRITE_BLOCKING)
            } catch (_: Exception) {
                break
            }
        }
    }

    private fun generateSample(sampleRate: Int, index: Int): Pair<Float, Float> {
        return when (soundType) {
            SoundType.RAIN -> generateRain(sampleRate, index, intensity = 0.4f)
            SoundType.HEAVY_RAIN -> generateRain(sampleRate, index, intensity = 0.9f)
            SoundType.WIND -> generateWind(sampleRate, index)
            SoundType.THUNDER -> generateThunder(sampleRate, index)
            SoundType.NOISE -> generateAnalogNoise(sampleRate, index)
        }
    }

    /**
     * باران: نویز سفید فیلترشده + قطره‌های تصادفی
     */
    private fun generateRain(sampleRate: Int, index: Int, intensity: Float): Pair<Float, Float> {
        // نویز سفید
        val white = (Math.random() * 2 - 1).toFloat()

        // فیلتر low-pass برای تبدیل به صدای باران
        lowPassBuffer = lowPassBuffer * (1 - lowPassAlpha) + white * lowPassAlpha
        val rainBase = lowPassBuffer * intensity * 2f

        // قطره‌های تصادفی (drop hits) - صدای چک‌چک باران روی سطوح
        val drop = generateDrops(sampleRate)

        // استریو با کمی تفاوت در دو کانال (حس فضایی)
        val left = (rainBase + drop * 0.7f) * volume
        val right = (rainBase * 0.95f + drop * 0.6f) * volume

        return Pair(left.coerceIn(-1f, 1f), right.coerceIn(-1f, 1f))
    }

    /**
     * قطره‌های باران: پالس‌های کوتاه تصادفی
     */
    private fun generateDrops(sampleRate: Int): Float {
        // هر 2000 نمونه، شاید یه قطره
        if (index % 2000 == 0 && Math.random() < 0.4) {
            // شروع یه قطره جدید
            val amplitude = (Math.random() * 0.5 + 0.3).toFloat()
            dropBuffer = FloatArray(64) { i ->
                amplitude * exp(-i * 0.15f).toFloat() *
                        (Math.random() * 2 - 1).toFloat()
            }
            dropIndex = 0
        }

        if (dropIndex < dropBuffer.size) {
            return dropBuffer[dropIndex++]
        }
        return 0f
    }

    /**
     * باد: نویز با فرکانس پایین
     */
    private fun generateWind(sampleRate: Int, index: Int): Pair<Float, Float> {
        val white = (Math.random() * 2 - 1).toFloat()
        // فیلتر قوی‌تر برای صدای بم
        lowPassBuffer = lowPassBuffer * 0.98f + white * 0.02f

        // مدولاسیون آهسته برای حس وزش باد
        val lfo = sin(index * 2.0 * PI * 0.3 / sampleRate).toFloat()

        val wind = lowPassBuffer * 3f * (0.5f + lfo * 0.5f) * volume
        val left = wind * 1.1f
        val right = wind * 0.9f

        return Pair(left.coerceIn(-1f, 1f), right.coerceIn(-1f, 1f))
    }

    /**
     * رعد: پالس‌های بلند و بم
     */
    private fun generateThunder(sampleRate: Int, index: Int): Pair<Float, Float> {
        val white = (Math.random() * 2 - 1).toFloat()
        lowPassBuffer = lowPassBuffer * 0.995f + white * 0.005f

        // هر ۵ ثانیه یه رعد
        val cyclePos = index % (sampleRate * 5)
        val cycleNorm = cyclePos.toFloat() / (sampleRate * 5)

        val thunderEnvelope = if (cycleNorm < 0.15f) {
            exp(-cycleNorm * 15f).toFloat()
        } else 0f

        val thunder = lowPassBuffer * 8f * thunderEnvelope * volume

        return Pair(thunder, thunder)
    }

    /**
     * نویز آنالوگ (مثل نوار کاست قدیمی)
     */
    private fun generateAnalogNoise(sampleRate: Int, index: Int): Pair<Float, Float> {
        val white = (Math.random() * 2 - 1).toFloat()
        // صدای خش‌خش نوار
        val hiss = white * 0.15f

        // صدای وزوز
        val hum = sin(index * 2.0 * PI * 60.0 / sampleRate).toFloat() * 0.05f

        val noise = (hiss + hum) * volume
        return Pair(noise, noise)
    }
}
