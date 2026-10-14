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
    private var isRecording = false
    private var outputFile: File? = null

    var onProgress: ((Long) -> Unit)? = null
    var onFinish: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // تنظیمات ثابت صوتی
    private val sampleRate = 44100
    private val channelCount = 2
    private val bitRate = 192 * 1024 // 192 kbps

    fun startRecording(mediaProjection: MediaProjection): Boolean {
        return try {
            // ۱. آماده‌سازی AudioRecord برای ضبط صدای اپ
            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate * 2)

            audioRecord = AudioRecord.Builder()
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

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError?.invoke("AudioRecord راه‌اندازی نشد")
                return false
            }

            outputFile = createOutputFile()
            audioRecord?.startRecording()
            isRecording = true

            recordingThread = thread {
                // ۲. ضبط و انکود در یک Thread جداگانه
                encodeAndWriteAudio(bufferSize)
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
        recordingThread?.join(2000)
        recordingThread = null
    }

    private fun encodeAndWriteAudio(bufferSize: Int) {
        val muxer = MediaMuxer(outputFile!!.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        try {
            // ۳. پیکربندی انکودر AAC
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                )
            }

            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val pcmBuffer = ByteArray(bufferSize)
            var totalBytesRead: Long = 0
            var presentationTimeUs: Long = 0
            val timeoutUs = 10000L

            // ۴. حلقه اصلی: خواندن PCM و فرستادن به انکودر
            while (isRecording) {
                val bytesRead = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: -1
                if (bytesRead <= 0) continue

                totalBytesRead += bytesRead

                // ورودی به انکودر
                val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                    inputBuffer.clear()
                    inputBuffer.put(pcmBuffer, 0, bytesRead)
                    codec.queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs, 0)
                    presentationTimeUs += (bytesRead.toLong() * 1_000_000L) /
                            (sampleRate.toLong() * channelCount * 2)
                }

                // خروجی از انکودر
                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0) {
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

            // ۵. پایان کار و آزادسازی منابع
            if (muxerStarted) {
                val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inputBufferIndex >= 0) {
                    codec.queueInputBuffer(
                        inputBufferIndex, 0, 0, presentationTimeUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }

                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            codec.stop()
            codec.release()
            muxer.stop()
            muxer.release()

            outputFile?.let { onFinish?.invoke(it) }

        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke(e.message ?: "خطا در انکود و ذخیره فایل")
            try { muxer.release() } catch (_: Exception) {}
        }
    }

    private fun createOutputFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "RemixLab_$timestamp.m4a"
        val dir = File(context.getExternalFilesDir(null), "Recordings")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName)
    }
}
