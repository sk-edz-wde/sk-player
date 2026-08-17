package com.example.audio

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent preferences for audio DSP effects:
 * - Equalizer (Master Switch & Bands)
 * - Golden Bass Booster (100% One-Touch ON/OFF)
 * - 8D Surround Audio (Master Switch)
 * - "Save Settings" checkbox persistence
 */
class AudioSettingsPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sk_audio_engine_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SAVE_SETTINGS_CHECKED = "key_save_settings_checked"
        private const val KEY_EQ_ENABLED = "key_eq_enabled"
        private const val KEY_BASS_ENABLED = "key_bass_enabled"
        private const val KEY_8D_ENABLED = "key_8d_enabled"
        private const val KEY_SELECTED_PRESET = "key_selected_preset"
        private const val KEY_BAND_0 = "key_band_0"
        private const val KEY_BAND_1 = "key_band_1"
        private const val KEY_BAND_2 = "key_band_2"
        private const val KEY_BAND_3 = "key_band_3"
        private const val KEY_BAND_4 = "key_band_4"
        private const val KEY_PLAYBACK_SPEED = "key_playback_speed"
        private const val KEY_BASS_LEVEL = "key_bass_level"
    }

    var isSaveSettingsChecked: Boolean
        get() = prefs.getBoolean(KEY_SAVE_SETTINGS_CHECKED, false)
        set(value) = prefs.edit().putBoolean(KEY_SAVE_SETTINGS_CHECKED, value).apply()

    var bassLevel: Float
        get() {
            if (!isSaveSettingsChecked) return 80f // Default 80%
            return prefs.getFloat(KEY_BASS_LEVEL, 80f)
        }
        set(value) {
            if (isSaveSettingsChecked) {
                prefs.edit().putFloat(KEY_BASS_LEVEL, value).apply()
            }
        }

    var isEqualizerEnabled: Boolean
        get() {
            if (!isSaveSettingsChecked) return false // Default OFF
            return prefs.getBoolean(KEY_EQ_ENABLED, false)
        }
        set(value) {
            if (isSaveSettingsChecked) {
                prefs.edit().putBoolean(KEY_EQ_ENABLED, value).apply()
            }
        }

    var isGoldenBassEnabled: Boolean
        get() {
            if (!isSaveSettingsChecked) return false // Default OFF
            return prefs.getBoolean(KEY_BASS_ENABLED, false)
        }
        set(value) {
            if (isSaveSettingsChecked) {
                prefs.edit().putBoolean(KEY_BASS_ENABLED, value).apply()
            }
        }

    var is8dAudioEnabled: Boolean
        get() {
            if (!isSaveSettingsChecked) return false // Default OFF
            return prefs.getBoolean(KEY_8D_ENABLED, false)
        }
        set(value) {
            if (isSaveSettingsChecked) {
                prefs.edit().putBoolean(KEY_8D_ENABLED, value).apply()
            }
        }

    var selectedPreset: String
        get() {
            if (!isSaveSettingsChecked) return EqualizerPreset.Flat.name
            return prefs.getString(KEY_SELECTED_PRESET, EqualizerPreset.Flat.name) ?: EqualizerPreset.Flat.name
        }
        set(value) {
            if (isSaveSettingsChecked) {
                prefs.edit().putString(KEY_SELECTED_PRESET, value).apply()
            }
        }

    var playbackSpeed: Float
        get() {
            if (!isSaveSettingsChecked) return 1.0f
            return prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
        }
        set(value) {
            if (isSaveSettingsChecked) {
                prefs.edit().putFloat(KEY_PLAYBACK_SPEED, value).apply()
            }
        }

    fun getSavedBandGains(): List<Float> {
        if (!isSaveSettingsChecked) return listOf(0f, 0f, 0f, 0f, 0f)
        return listOf(
            prefs.getFloat(KEY_BAND_0, 0f),
            prefs.getFloat(KEY_BAND_1, 0f),
            prefs.getFloat(KEY_BAND_2, 0f),
            prefs.getFloat(KEY_BAND_3, 0f),
            prefs.getFloat(KEY_BAND_4, 0f)
        )
    }

    fun saveBandGains(gains: List<Float>) {
        if (!isSaveSettingsChecked) return
        val editor = prefs.edit()
        gains.forEachIndexed { index, gain ->
            when (index) {
                0 -> editor.putFloat(KEY_BAND_0, gain)
                1 -> editor.putFloat(KEY_BAND_1, gain)
                2 -> editor.putFloat(KEY_BAND_2, gain)
                3 -> editor.putFloat(KEY_BAND_3, gain)
                4 -> editor.putFloat(KEY_BAND_4, gain)
            }
        }
        editor.apply()
    }

    /**
     * Force save current in-memory settings when the user checks the Save Settings checkbox.
     */
    fun saveAll(
        eqEnabled: Boolean,
        bassEnabled: Boolean,
        eightDEnabled: Boolean,
        preset: String,
        bandGains: List<Float>,
        speed: Float,
        bassLvl: Float = 80f
    ) {
        prefs.edit()
            .putBoolean(KEY_SAVE_SETTINGS_CHECKED, true)
            .putBoolean(KEY_EQ_ENABLED, eqEnabled)
            .putBoolean(KEY_BASS_ENABLED, bassEnabled)
            .putBoolean(KEY_8D_ENABLED, eightDEnabled)
            .putString(KEY_SELECTED_PRESET, preset)
            .putFloat(KEY_BAND_0, bandGains.getOrElse(0) { 0f })
            .putFloat(KEY_BAND_1, bandGains.getOrElse(1) { 0f })
            .putFloat(KEY_BAND_2, bandGains.getOrElse(2) { 0f })
            .putFloat(KEY_BAND_3, bandGains.getOrElse(3) { 0f })
            .putFloat(KEY_BAND_4, bandGains.getOrElse(4) { 0f })
            .putFloat(KEY_PLAYBACK_SPEED, speed)
            .putFloat(KEY_BASS_LEVEL, bassLvl)
            .apply()
    }
}
