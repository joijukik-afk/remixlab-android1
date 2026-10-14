package com.joijukik.remixlab

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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class AudioExporter(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    private var outputFile: File? = null

    var onProgress: ((Long) -> Unit)? = null
    var onFinish: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val sampleRate = 44100
    private val channelCount = 2
    private val bitRate = 192 * 1024

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
            outputFile = createOutputFile()
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
        // فقط flag رو false کن - thread خودش می‌فهمه و تمیز می‌بنده
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

                // --- ورودی به انکودر ---
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

                // --- خروجی از انکودر ---
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

            // --- Flush نهایی ---
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

            // --- بستن تمیز ---
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer.stop() } catch (_: Exception) {}
            try { muxer.release() } catch (_: Exception) {}

            // --- آزادسازی AudioRecord ---
            try { record.stop() } catch (_: Exception) {}
            try { record.release() } catch (_: Exception) {}
            audioRecord = null

            onFinish?.invoke(file)

        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = "${e.javaClass.simpleName}: ${e.message ?: "خطای ناشناخته"}"
            onError?.invoke(errorMsg)

            // آزادسازی منابع در صورت خطا
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

    private fun createOutputFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "RemixLab_$timestamp.m4a"

        // پوشه اپ، اگه نبود پوشه cache
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, "Recordings")
        if (!dir.exists()) dir.mkdirs()

        return File(dir, fileName)
    }
}
