package com.joijukik.remixlab

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.naman14.androidlame.AndroidLame
import com.naman14.androidlame.LameBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class AudioExporter(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    private var outputFile: File? = null
    private var androidLame: AndroidLame? = null

    var onProgress: ((Long) -> Unit)? = null
    var onFinish: ((Uri) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val sampleRate = 48000
    private val channelCount = 2
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startRecording(
        mediaProjection: MediaProjection,
        bitRate: Int = 320 * 1024
    ): Boolean {
        return try {
            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val minBufferSizeShorts = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat
            ) / 2
            val bufferSizeShorts = (minBufferSizeShorts * 4).coerceAtLeast(sampleRate)

            val record = AudioRecord.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeShorts * 2)
                .setAudioPlaybackCaptureConfig(captureConfig)
                .build()

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                onError?.invoke("AudioRecord راه‌اندازی نشد")
                record.release()
                return false
            }

            audioRecord = record
            outputFile = createTempFile()
            record.startRecording()
            isRecording = true

            recordingThread = thread {
                encodeAndWriteAudio(bufferSizeShorts, bitRate)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke("شروع ضبط: ${e.javaClass.simpleName} - ${e.message}")
            false
        }
    }

    fun stopRecording() {
        isRecording = false
    }

    private fun encodeAndWriteAudio(bufferSizeShorts: Int, bitRate: Int) {
        val file = outputFile ?: return

        val builder = LameBuilder()
            .setInSampleRate(sampleRate)
            .setOutChannels(channelCount)
            .setOutBitrate(bitRate / 1024)  // kbps
            .setMode(LameBuilder.Mode.JSTEREO)
            .setQuality(2)
            .setVbrMode(LameBuilder.VbrMode.VBR_OFF)

        androidLame = AndroidLame(builder)

        val pcmBuffer = ShortArray(bufferSizeShorts)
        val leftChannel = ShortArray(bufferSizeShorts / 2)
        val rightChannel = ShortArray(bufferSizeShorts / 2)
        val mp3Buffer = ByteArray((7200 + (1.25 * bufferSizeShorts * 2)).toInt())

        try {
            FileOutputStream(file).use { outputStream ->
                val record = audioRecord ?: return
                while (isRecording) {
                    val shortsRead = record.read(pcmBuffer, 0, pcmBuffer.size)
                    if (shortsRead <= 0) continue

                    val samplesPerChannel = shortsRead / 2

                    for (i in 0 until samplesPerChannel) {
                        leftChannel[i] = pcmBuffer[i * 2]
                        rightChannel[i] = pcmBuffer[i * 2 + 1]
                    }

                    val bytesEncoded = androidLame?.encode(
                        leftChannel,
                        rightChannel,
                        samplesPerChannel,
                        mp3Buffer
                    ) ?: 0

                    if (bytesEncoded > 0) {
                        outputStream.write(mp3Buffer, 0, bytesEncoded)
                        onProgress?.invoke(file.length())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke("خطا در انکود: ${e.message}")
        } finally {
            androidLame?.close()
            androidLame = null
            try { audioRecord?.stop() } catch (_: Exception) {}
            try { audioRecord?.release() } catch (_: Exception) {}
            audioRecord = null

            saveMp3ToMediaStore(file)
        }
    }

    private fun saveMp3ToMediaStore(mp3File: File) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "RemixLab_$timestamp.mp3"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/RemixLab")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val uri = context.contentResolver.insert(collection, values)
        if (uri == null) {
            onError?.invoke("خطا در ثبت MP3 در MediaStore")
            return
        }

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(mp3File).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }

            mp3File.delete()
            onFinish?.invoke(uri)

        } catch (e: Exception) {
            e.printStackTrace()
            context.contentResolver.delete(uri, null, null)
            onError?.invoke("خطا در ذخیره MP3: ${e.message}")
        }
    }

    private fun createTempFile(): File {
        val dir = File(context.cacheDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "temp_${System.currentTimeMillis()}.mp3")
    }
}
