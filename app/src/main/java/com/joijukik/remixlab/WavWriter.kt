package com.joijukik.remixlab

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

/**
 * نوشتن فایل WAV از داده‌های PCM 16-bit Stereo
 */
object WavWriter {

    fun write(
        file: File,
        pcmData: ShortArray,
        sampleRate: Int = 44100,
        channels: Int = 2
    ) {
        val byteRate = sampleRate * channels * 2
        val dataSize = pcmData.size * 2

        FileOutputStream(file).use { fos ->
            // نوشتن هدر WAV
            val header = ByteArray(44)

            // "RIFF"
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()

            // ChunkSize
            val chunkSize = 36 + dataSize
            header[4] = (chunkSize and 0xff).toByte()
            header[5] = ((chunkSize shr 8) and 0xff).toByte()
            header[6] = ((chunkSize shr 16) and 0xff).toByte()
            header[7] = ((chunkSize shr 24) and 0xff).toByte()

            // "WAVE"
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()

            // "fmt "
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()

            // Subchunk1Size = 16
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0

            // AudioFormat = 1 (PCM)
            header[20] = 1
            header[21] = 0

            // NumChannels
            header[22] = channels.toByte()
            header[23] = 0

            // SampleRate
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()

            // ByteRate
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()

            // BlockAlign = channels * 2
            header[32] = (channels * 2).toByte()
            header[33] = 0

            // BitsPerSample = 16
            header[34] = 16
            header[35] = 0

            // "data"
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()

            // DataSize
            header[40] = (dataSize and 0xff).toByte()
            header[41] = ((dataSize shr 8) and 0xff).toByte()
            header[42] = ((dataSize shr 16) and 0xff).toByte()
            header[43] = ((dataSize shr 24) and 0xff).toByte()

            fos.write(header)

            // نوشتن داده‌های PCM به صورت Little-Endian
            val buffer = ByteArray(8192)
            var bufferIndex = 0
            for (sample in pcmData) {
                buffer[bufferIndex++] = (sample.toInt() and 0xff).toByte()
                buffer[bufferIndex++] = ((sample.toInt() shr 8) and 0xff).toByte()
                if (bufferIndex >= buffer.size) {
                    fos.write(buffer, 0, bufferIndex)
                    bufferIndex = 0
                }
            }
            if (bufferIndex > 0) {
                fos.write(buffer, 0, bufferIndex)
            }
        }
    }
}
