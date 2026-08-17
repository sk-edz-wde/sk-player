package com.example.audio

enum class RepeatMode {
    OFF, ALL, ONE
}

data class EqualizerBand(
    val frequencyLabel: String,
    val centerFreqHz: Int,
    val gainDb: Float // -12f to +12f
)

data class EqualizerPreset(
    val name: String,
    val bandGains: List<Float> // 5 values for 60Hz, 230Hz, 910Hz, 4kHz, 14kHz
) {
    companion object {
        val Flat = EqualizerPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f))
        val BassBoost = EqualizerPreset("Bass Boost", listOf(11.5f, 9.0f, 2.5f, 4.0f, 4.8f))
        val TrebleEnhancer = EqualizerPreset("Treble Enhancer", listOf(-1.0f, 0f, 2.0f, 5.0f, 6.5f))
        val VocalBoost = EqualizerPreset("Vocal Boost", listOf(-1.5f, 1.0f, 5.5f, 4.0f, 2.0f))
        val Rock = EqualizerPreset("Rock", listOf(7.5f, 5.0f, 0.5f, 4.0f, 5.5f))
        val EdmParty = EqualizerPreset("EDM Party", listOf(11.0f, 8.5f, 2.0f, 5.0f, 6.5f))

        val AllPresets = listOf(Flat, BassBoost, TrebleEnhancer, VocalBoost, Rock, EdmParty)
    }
}
