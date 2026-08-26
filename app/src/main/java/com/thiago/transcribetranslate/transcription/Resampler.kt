package com.thiago.transcribetranslate.transcription

object Resampler {
    fun to16k(input: FloatArray, inputRate: Int): FloatArray {
        if (inputRate == 16_000) return input
        val outputSize = (input.size.toLong() * 16_000L / inputRate).toInt()
        return FloatArray(outputSize) { i ->
            val position = i.toDouble() * inputRate / 16_000.0
            val left = position.toInt().coerceIn(0, input.lastIndex)
            val right = (left + 1).coerceAtMost(input.lastIndex)
            val fraction = (position - left).toFloat()
            input[left] * (1f - fraction) + input[right] * fraction
        }
    }
}
