package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Standard Stremio Addon Protocol v3 Data Models
 */

@JsonClass(generateAdapter = true)
data class AddonCatalog(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<AddonCatalogExtra>? = null
)

@JsonClass(generateAdapter = true)
data class AddonCatalogExtra(
    val name: String,
    val isRequired: Boolean? = false,
    val options: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AddonManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val logo: String? = null,
    val background: String? = null,
    val types: List<String> = emptyList(),
    val resources: List<Any>? = null, // Can be list of strings or objects
    val catalogs: List<AddonCatalog> = emptyList(),
    val idPrefixes: List<String>? = null,
    @Transient var transportUrl: String = "",
    @Transient var isEnabled: Boolean = true,
    @Transient var isBuiltin: Boolean = false
) {
    fun supportsResource(resourceName: String, type: String? = null): Boolean {
        if (resources == null) return false
        for (res in resources) {
            when (res) {
                is String -> {
                    if (res.equals(resourceName, ignoreCase = true)) return true
                }
                is Map<*, *> -> {
                    val name = res["name"] as? String
                    if (name.equals(resourceName, ignoreCase = true)) {
                        if (type != null) {
                            val typesList = res["types"] as? List<*>
                            if (typesList != null && !typesList.contains(type)) {
                                return false
                            }
                        }
                        return true
                    }
                }
            }
        }
        return false
    }

    fun supportsType(type: String): Boolean {
        return types.contains(type)
    }

    fun matchesIdPrefix(itemId: String): Boolean {
        val prefixes = idPrefixes ?: return true
        if (prefixes.isEmpty()) return true
        return prefixes.any { itemId.startsWith(it) }
    }
}

@JsonClass(generateAdapter = true)
data class CatalogResponse(
    val metas: List<MetaItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MetaResponse(
    val meta: MetaDetail? = null
)

@JsonClass(generateAdapter = true)
data class StreamResponse(
    val streams: List<StreamItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MetaItem(
    val id: String,
    val type: String, // "movie", "series", "channel", etc.
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String>? = null,
    @Transient var addonName: String? = null,
    @Transient var addonId: String? = null
)

@JsonClass(generateAdapter = true)
data class MetaDetail(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String>? = null,
    val runtime: String? = null,
    val director: List<String>? = null,
    val cast: List<String>? = null,
    val videos: List<VideoEpisode>? = null,
    val trailer: String? = null
)

@JsonClass(generateAdapter = true)
data class VideoEpisode(
    val id: String,
    val title: String,
    val released: String? = null,
    val season: Int = 1,
    val episode: Int = 1,
    val thumbnail: String? = null,
    val overview: String? = null,
    val seasonTitle: String? = null
)

@JsonClass(generateAdapter = true)
data class StreamItem(
    val title: String? = null,
    val name: String? = null,
    val url: String? = null,
    val externalUrl: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val quality: String? = null,
    @Transient var addonName: String = "Community Addon"
)

/**
 * Saved Library item state
 */
data class LibraryItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val releaseInfo: String?,
    val imdbRating: String?,
    val genres: List<String>?,
    val status: WatchStatus = WatchStatus.WATCHING,
    val watchPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastEpisodeId: String? = null,
    val lastEpisodeTitle: String? = null,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (watchPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

enum class WatchStatus(val displayName: String) {
    WATCHING("Watching"),
    PLAN_TO_WATCH("Plan to Watch"),
    COMPLETED("Completed"),
    FAVORITE("Favorites")
}

@JsonClass(generateAdapter = true)
data class SubtitleItem(
    val id: String,
    val url: String? = null,
    val lang: String,
    val label: String? = null,
    @Transient var addonName: String = "OpenSubtitles"
)

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class SubtitleSettings(
    val isEnabled: Boolean = true,
    val selectedLanguage: String = "eng", // "eng", "spa", "fre", "ger", "jpn"
    val fontSizeSp: Int = 18,
    val textColor: String = "white", // "white", "yellow", "cyan", "green"
    val hasBackgroundBox: Boolean = true,
    val delayOffsetMs: Long = 0L
)

data class AudioSettings(
    val selectedTrack: String = "default", // "default", "stereo", "surround_51", "night_mode"
    val volumeBoostPercent: Int = 100, // 100, 125, 150, 200
    val audioDelayMs: Long = 0L
)

