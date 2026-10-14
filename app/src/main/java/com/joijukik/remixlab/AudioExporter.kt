package com.joijukik.remixlab

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class AudioExporter(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    private var outputFile: File? = null
    private var outputUri: Uri? = null

    var onProgress: ((Long) -> Unit)? = null
    var onFinish: ((Uri) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val sampleRate = 48000
    private val channelCount = 2
    private val bitRate = 256 * 1024

    fun startRecording(mediaProjection: MediaProjection): Boolean {
        return try {
            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate * 2)

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
            outputFile = createTempFile()
            record.startRecording()
            isRecording = true

            recordingThread = thread {
                encodeAndWriteAudio(bufferSize)
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

    private fun encodeAndWriteAudio(bufferSize: Int) {
        val file = outputFile ?: run {
            onError?.invoke("فایل خروجی وجود ندارد")
            return
        }

        var muxer: MediaMuxer? = null
        var codec: MediaCodec? = null

        try {
            muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                )
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
            }

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val pcmBuffer = ByteArray(bufferSize)
            var totalBytesRead: Long = 0
            var presentationTimeUs: Long = 0
            val timeoutUs = 10000L
            var trackIndex = -1
            var muxerStarted = false

            val record = audioRecord ?: run {
                onError?.invoke("AudioRecord null شد")
                return
            }

            while (isRecording) {
                val bytesRead = record.read(pcmBuffer, 0, pcmBuffer.size)
                if (bytesRead <= 0) continue

                totalBytesRead += bytesRead

                val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        inputBuffer.put(pcmBuffer, 0, bytesRead)
                        codec.queueInputBuffer(
                            inputBufferIndex, 0, bytesRead, presentationTimeUs, 0
                        )
                        presentationTimeUs += (bytesRead.toLong() * 1_000_000L) /
                                (sampleRate.toLong() * channelCount * 2)
                    }
                }

                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0 && outputBuffer != null) {
                        if (!muxerStarted) {
                            val newFormat = codec.outputFormat
                            trackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }

                onProgress?.invoke(totalBytesRead)
            }

            val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
            if (inputBufferIndex >= 0) {
                codec.queueInputBuffer(
                    inputBufferIndex, 0, 0, presentationTimeUs,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            while (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size > 0 && outputBuffer != null && muxerStarted) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            }

            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer.stop() } catch (_: Exception) {}
            try { muxer.release() } catch (_: Exception) {}

            try { record.stop() } catch (_: Exception) {}
            try { record.release() } catch (_: Exception) {}
            audioRecord = null

            // حالا فایل رو از temp به MediaStore منتقل کن
            saveToMediaStore(file)

        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = "${e.javaClass.simpleName}: ${e.message ?: "خطای ناشناخته"}"
            onError?.invoke(errorMsg)

            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            try { audioRecord?.stop() } catch (_: Exception) {}
            try { audioRecord?.release() } catch (_: Exception) {}
            audioRecord = null
        } finally {
            isRecording = false
            recordingThread = null
        }
    }

    /**
     * ذخیره فایل temp توی MediaStore (پوشه Music/RemixLab)
     */
    private fun saveToMediaStore(tempFile: File) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "RemixLab_$timestamp.m4a"

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Audio.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MUSIC}/RemixLab"
                    )
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(collection, contentValues)
            if (uri == null) {
                onError?.invoke("خطا در ثبت فایل در MediaStore")
                return
            }

            // کپی محتوای فایل temp به MediaStore
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(tempFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // حذف فایل temp
            tempFile.delete()

            outputUri = uri
            onFinish?.invoke(uri)

        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke("خطا در ذخیره: ${e.message}")
        }
    }

    private fun createTempFile(): File {
        val dir = File(context.cacheDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "temp_${System.currentTimeMillis()}.m4a")
    }
}
