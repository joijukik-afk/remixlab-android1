package com.joijukik.remixlab

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class AudioExporter(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var outputFile: File? = null

    var onProgress: ((Long) -> Unit)? = null
    var onFinish: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // نرخ نمونه‌برداری و بیت‌ریت
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channelMask = AudioFormat.CHANNEL_IN_LEFT or AudioFormat.CHANNEL_IN_RIGHT

    fun startRecording(mediaProjection: MediaProjection): Boolean {
        return try {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate, channelConfig, audioFormat
            ).coerceAtLeast(sampleRate * 2)

            audioRecord = AudioRecord.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()

            // ساخت فایل خروجی
            outputFile = createOutputFile()
            audioRecord?.startRecording()
            isRecording = true

            // شروع thread ضبط
            recordingThread = thread {
                writeAudioDataToFile(bufferSize)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke(e.message ?: "خطای ناشناخته در شروع ضبط")
            false
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recordingThread?.join(1000)
        recordingThread = null

        outputFile?.let {
            addWavHeader(it)
            onFinish?.invoke(it)
        }
    }

    private fun writeAudioDataToFile(bufferSize: Int) {
        val data = ByteArray(bufferSize)
        var totalBytes: Long = 0

        FileOutputStream(outputFile).use { outputStream ->
            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: -1
                if (read > 0) {
                    outputStream.write(data, 0, read)
                    totalBytes += read
                    onProgress?.invoke(totalBytes)
                }
            }
        }
    }

    private fun createOutputFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "RemixLab_$timestamp.wav"

        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "RemixLab"
        )
        if (!dir.exists()) dir.mkdirs()

        return File(dir, fileName)
    }

    /**
     * اضافه کردن هدر WAV به فایل PCM خام
     */
    private fun addWavHeader(file: File) {
        try {
            val randomAccessFile = RandomAccessFile(file, "rw")
            val fileSize = randomAccessFile.length()
            val totalAudioLen = fileSize
            val totalDataLen = totalAudioLen + 36
            val channels = 2
            val byteRate = sampleRate * channels * 2

            val header = ByteArray(44)

            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = (channels * 2).toByte()
            header[33] = 0
            header[34] = 16
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

            // نوشتن هدر در ابتدای فایل
            randomAccessFile.seek(0)
            randomAccessFile.write(header, 0, 44)
            randomAccessFile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
