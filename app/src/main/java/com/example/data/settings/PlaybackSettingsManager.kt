package com.example.data.settings

import android.content.Context
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackSettings(
    val playbackSpeed: Float = 1.0f,
    val autoPlayNextEpisode: Boolean = true,
    val autoSkipIntro: Boolean = true,
    val hardwareAcceleration: Boolean = true,
    val resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val defaultSubtitleLang: String = "eng",
    val subtitleFontSize: Int = 18,
    val subtitleStyle: String = "box",
    val subtitleTextColor: String = "white",
    val subtitleVerticalPosition: String = "bottom",
    val audioPassthrough: Boolean = false,
    val audioVolumeBoost: Int = 100,
    val audioDelayMs: Long = 0L,
    val audioNightMode: Boolean = false,
    val audioPreferredLanguage: String = "default",
    val audioStereoMode: String = "stereo",
    val bufferCacheSeconds: Int = 60,
    val debridProvider: String = "none",
    val debridApiKey: String = ""
)

class PlaybackSettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("stremio_playback_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PlaybackSettings> = _settings.asStateFlow()

    private fun loadSettings(): PlaybackSettings {
        return PlaybackSettings(
            playbackSpeed = prefs.getFloat("playback_speed", 1.0f),
            autoPlayNextEpisode = prefs.getBoolean("auto_play_next", true),
            autoSkipIntro = prefs.getBoolean("auto_skip_intro", true),
            hardwareAcceleration = prefs.getBoolean("hardware_acceleration", true),
            resizeMode = prefs.getInt("resize_mode", AspectRatioFrameLayout.RESIZE_MODE_FIT),
            defaultSubtitleLang = prefs.getString("subtitle_lang", "eng") ?: "eng",
            subtitleFontSize = prefs.getInt("subtitle_font_size", 18),
            subtitleStyle = prefs.getString("subtitle_style", "box") ?: "box",
            subtitleTextColor = prefs.getString("subtitle_text_color", "white") ?: "white",
            subtitleVerticalPosition = prefs.getString("subtitle_vertical_pos", "bottom") ?: "bottom",
            audioPassthrough = prefs.getBoolean("audio_passthrough", false),
            audioVolumeBoost = prefs.getInt("audio_volume_boost", 100),
            audioDelayMs = prefs.getLong("audio_delay_ms", 0L),
            audioNightMode = prefs.getBoolean("audio_night_mode", false),
            audioPreferredLanguage = prefs.getString("audio_pref_lang", "default") ?: "default",
            audioStereoMode = prefs.getString("audio_stereo_mode", "stereo") ?: "stereo",
            bufferCacheSeconds = prefs.getInt("buffer_cache_sec", 60),
            debridProvider = prefs.getString("debrid_provider", "none") ?: "none",
            debridApiKey = prefs.getString("debrid_api_key", "") ?: ""
        )
    }

    fun updatePlaybackSpeed(speed: Float) {
        prefs.edit().putFloat("playback_speed", speed).apply()
        _settings.value = _settings.value.copy(playbackSpeed = speed)
    }

    fun setAutoPlayNextEpisode(enabled: Boolean) {
        prefs.edit().putBoolean("auto_play_next", enabled).apply()
        _settings.value = _settings.value.copy(autoPlayNextEpisode = enabled)
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        prefs.edit().putBoolean("auto_skip_intro", enabled).apply()
        _settings.value = _settings.value.copy(autoSkipIntro = enabled)
    }

    fun setHardwareAcceleration(enabled: Boolean) {
        prefs.edit().putBoolean("hardware_acceleration", enabled).apply()
        _settings.value = _settings.value.copy(hardwareAcceleration = enabled)
    }

    fun setResizeMode(mode: Int) {
        prefs.edit().putInt("resize_mode", mode).apply()
        _settings.value = _settings.value.copy(resizeMode = mode)
    }

    fun setDefaultSubtitleLang(lang: String) {
        prefs.edit().putString("subtitle_lang", lang).apply()
        _settings.value = _settings.value.copy(defaultSubtitleLang = lang)
    }

    fun setSubtitleFontSize(sizeSp: Int) {
        prefs.edit().putInt("subtitle_font_size", sizeSp).apply()
        _settings.value = _settings.value.copy(subtitleFontSize = sizeSp)
    }

    fun setSubtitleStyle(style: String) {
        prefs.edit().putString("subtitle_style", style).apply()
        _settings.value = _settings.value.copy(subtitleStyle = style)
    }

    fun setSubtitleTextColor(color: String) {
        prefs.edit().putString("subtitle_text_color", color).apply()
        _settings.value = _settings.value.copy(subtitleTextColor = color)
    }

    fun setSubtitleVerticalPosition(pos: String) {
        prefs.edit().putString("subtitle_vertical_pos", pos).apply()
        _settings.value = _settings.value.copy(subtitleVerticalPosition = pos)
    }

    fun setAudioPassthrough(enabled: Boolean) {
        prefs.edit().putBoolean("audio_passthrough", enabled).apply()
        _settings.value = _settings.value.copy(audioPassthrough = enabled)
    }

    fun setAudioVolumeBoost(boost: Int) {
        prefs.edit().putInt("audio_volume_boost", boost).apply()
        _settings.value = _settings.value.copy(audioVolumeBoost = boost)
    }

    fun setAudioDelayMs(delayMs: Long) {
        prefs.edit().putLong("audio_delay_ms", delayMs).apply()
        _settings.value = _settings.value.copy(audioDelayMs = delayMs)
    }

    fun setAudioNightMode(enabled: Boolean) {
        prefs.edit().putBoolean("audio_night_mode", enabled).apply()
        _settings.value = _settings.value.copy(audioNightMode = enabled)
    }

    fun setAudioPreferredLanguage(lang: String) {
        prefs.edit().putString("audio_pref_lang", lang).apply()
        _settings.value = _settings.value.copy(audioPreferredLanguage = lang)
    }

    fun setAudioStereoMode(mode: String) {
        prefs.edit().putString("audio_stereo_mode", mode).apply()
        _settings.value = _settings.value.copy(audioStereoMode = mode)
    }

    fun setBufferCacheSeconds(seconds: Int) {
        prefs.edit().putInt("buffer_cache_sec", seconds).apply()
        _settings.value = _settings.value.copy(bufferCacheSeconds = seconds)
    }

    fun setDebridProvider(provider: String) {
        prefs.edit().putString("debrid_provider", provider).apply()
        _settings.value = _settings.value.copy(debridProvider = provider)
    }

    fun setDebridApiKey(key: String) {
        prefs.edit().putString("debrid_api_key", key).apply()
        _settings.value = _settings.value.copy(debridApiKey = key)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _settings.value = PlaybackSettings()
    }
}
