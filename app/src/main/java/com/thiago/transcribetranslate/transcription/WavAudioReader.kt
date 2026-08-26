package com.thiago.transcribetranslate.transcription

import android.content.ContentResolver
import android.net.Uri
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PcmAudio(val samples: FloatArray, val sampleRate: Int)

object WavAudioReader {
    fun read(contentResolver: ContentResolver, uri: Uri): PcmAudio {
        contentResolver.openInputStream(uri).use { raw ->
            requireNotNull(raw) { "Não foi possível abrir o arquivo" }
            val input = BufferedInputStream(raw)
            val header = ByteArray(12)
            require(input.read(header) == 12) { "Arquivo WAV inválido" }
            require(String(header, 0, 4) == "RIFF" && String(header, 8, 4) == "WAVE") {
                "Por enquanto, a versão integrada aceita WAV PCM."
            }

            var channels = 0
            var sampleRate = 0
            var bits = 0
            var data: ByteArray? = null

            while (true) {
                val chunkHeader = ByteArray(8)
                if (input.read(chunkHeader) != 8) break
                val id = String(chunkHeader, 0, 4)
                val size = ByteBuffer.wrap(chunkHeader, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val chunk = ByteArray(size)
                var offset = 0
                while (offset < size) {
                    val n = input.read(chunk, offset, size - offset)
                    if (n < 0) break
                    offset += n
                }
                if (id == "fmt ") {
                    val b = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
                    val format = b.short.toInt()
                    channels = b.short.toInt()
                    sampleRate = b.int
                    b.int
                    b.short
                    bits = b.short.toInt()
                    require(format == 1 && bits == 16) { "Use WAV PCM 16-bit." }
                } else if (id == "data") {
                    data = chunk
                    break
                }
                if (size % 2 == 1) input.read()
            }

            require(channels > 0 && sampleRate > 0 && data != null) { "WAV incompleto" }
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val frames = data.size / (2 * channels)
            val samples = FloatArray(frames)
            for (i in 0 until frames) {
                var sum = 0f
                repeat(channels) { sum += buffer.short / 32768f }
                samples[i] = sum / channels
            }
            return PcmAudio(samples, sampleRate)
        }
    }
}
