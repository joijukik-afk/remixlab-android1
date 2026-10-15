package com.joijukik.remixlab

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ضبط صدا با MediaProjection و ذخیره فایل.
 * (نسخه ساده - ذخیره به صورت PCM خام)
 */
class AudioExporter(private val context: Context) {

    companion object {
        private const val TAG = "AudioExporter"
        private const val SAMPLE_RATE = 44100
        private const val CHANNELS = 2
    }

    var onProgress: ((Long) -> Unit)? = null
    var onFinish: ((Uri) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var outputFile: File? = null

    @Volatile private var isRecording = false

    fun startRecording(projection: MediaProjection): Boolean {
        if (isRecording) return false

        return try {
            mediaProjection = projection

            val audioPlaybackConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()

            audioRecord = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(
                    AudioRecord.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT
                    ) * 2
                )
                .setAudioPlaybackCaptureConfig(audioPlaybackConfig)
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError?.invoke("AudioRecord مقدار اولیه را نگرفت")
                return false
            }

            outputFile = createOutputFile()
            isRecording = true
            audioRecord?.startRecording()

            recordJob = scope.launch { captureLoop() }
            true
        } catch (e: Exception) {
            Log.e(TAG, "startRecording fail", e)
            onError?.invoke(e.message ?: "خطای ناشناخته")
            false
        }
    }

    private fun createOutputFile(): File {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "RemixLab"
        ).apply { if (!exists()) mkdirs() }

        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "remix_$ts.pcm")
    }

    private suspend fun captureLoop() {
        val buffer = ShortArray(4096)
        val outputStream = FileOutputStream(outputFile!!)
        val startTime = System.currentTimeMillis()

        try {
            while (isRecording && currentCoroutineContext().isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val bytes = ByteArray(read * 2)
                    var j = 0
                    for (i in 0 until read) {
                        bytes[j++] = (buffer[i].toInt() and 0xFF).toByte()
                        bytes[j++] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
                    }
                    outputStream.write(bytes)
                }
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                onProgress?.invoke(elapsed)
            }
            outputStream.flush()
            outputStream.close()

            withContext(Dispatchers.Main) {
                onFinish?.invoke(Uri.fromFile(outputFile))
            }
        } catch (e: Exception) {
            Log.e(TAG, "captureLoop fail", e)
            try { outputStream.close() } catch (_: Exception) {}
            withContext(Dispatchers.Main) { onError?.invoke(e.message ?: "خطای ضبط") }
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    fun release() {
        stopRecording()
        scope.cancel()
    }
}
