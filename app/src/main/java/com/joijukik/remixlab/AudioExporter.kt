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
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class AudioExporter(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    private var pcmFile: File? = null

    var onProgress: ((Long) -> Unit)? = null
    var onFinish: ((Uri) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // کیفیت استودیویی
    private val sampleRate = 48000
    private val channelCount = 2
    private val bitRate = 320 * 1024 // 320 kbps for MP3

    fun startRecording(mediaProjection: MediaProjection): Boolean {
        return try {
            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            // بافر بزرگ برای جلوگیری از underrun (خش صدا)
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 4).coerceAtLeast(sampleRate * 2)

            val record = AudioRecord.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(captureConfig)
                .build()

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                onError?.invoke("AudioRecord راه‌اندازی نشد")
                record.release()
                return false
            }

            audioRecord = record
            pcmFile = createTempPcmFile()
            record.startRecording()
            isRecording = true

            recordingThread = thread {
                writePcmData(bufferSize)
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

    private fun writePcmData(bufferSize: Int) {
        val file = pcmFile ?: return
        val data = ByteArray(bufferSize)
        var totalBytes: Long = 0

        file.outputStream().use { outputStream ->
            val record = audioRecord ?: return
            while (isRecording) {
                val read = record.read(data, 0, bufferSize)
                if (read > 0) {
                    outputStream.write(data, 0, read)
                    totalBytes += read
                    onProgress?.invoke(totalBytes)
                }
            }
        }

        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null

        // 🔑 تبدیل PCM به MP3 با FFmpeg
        convertPcmToMp3(file)
    }

    /**
     * تبدیل فایل خام PCM به MP3 با استفاده از کتابخانه LAME
     */
    private fun convertPcmToMp3(pcmFile: File) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputDir = File(context.cacheDir, "recordings")
        if (!outputDir.exists()) outputDir.mkdirs()
        val mp3File = File(outputDir, "RemixLab_$timestamp.mp3")

        // 🎯 دستور FFmpeg برای تبدیل PCM به MP3 با کیفیت 320kbps
        val command = "-y -f s16le -ar $sampleRate -ac $channelCount -i \"${pcmFile.absolutePath}\" -codec:a libmp3lame -b:a 320k \"${mp3File.absolutePath}\""

        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                // ✅ تبدیل موفق - فایل PCM رو پاک کن و MP3 رو ذخیره کن
                pcmFile.delete()
                saveMp3ToMediaStore(mp3File)
            } else {
                onError?.invoke("خطا در تبدیل به MP3: ${session.failStackTrace}")
                // اگه تبدیل نشد، فایل PCM رو نگه دار (برای دیباگ)
            }
        }
    }

    /**
     * ذخیره فایل MP3 در پوشه Music/RemixLab
     */
    private fun saveMp3ToMediaStore(mp3File: File) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "RemixLab_$timestamp.mp3"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg") // ← MP3 MIME type
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

    private fun createTempPcmFile(): File {
        val dir = File(context.cacheDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "temp_${System.currentTimeMillis()}.pcm")
    }
}
