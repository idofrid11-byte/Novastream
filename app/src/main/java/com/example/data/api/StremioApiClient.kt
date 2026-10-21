package com.example.data.api

import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class StremioApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val TAG = "StremioApiClient"

    /**
     * Normalizes an addon URL, handling stremio:// schema
     */
    fun normalizeUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.startsWith("stremio://", ignoreCase = true)) {
            url = "https://" + url.substring("stremio://".length)
        }
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }
        return url
    }

    /**
     * Fetches and parses an Addon Manifest from a URL
     */
    suspend fun fetchManifest(addonUrl: String): Result<AddonManifest> = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeUrl(addonUrl)
            val manifestUrl = if (normalized.endsWith("/manifest.json")) normalized else {
                if (normalized.endsWith("/")) "${normalized}manifest.json" else "$normalized/manifest.json"
            }

            val request = Request.Builder()
                .url(manifestUrl)
                .header("User-Agent", "NovaStream/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty manifest body"))
            val json = JSONObject(body)

            val id = json.optString("id", "custom.addon.${System.currentTimeMillis()}")
            val name = json.optString("name", "Custom Addon")
            val version = json.optString("version", "1.0.0")
            val description = json.optString("description", "")
            val logo = json.optString("logo").takeIf { it.isNotEmpty() }
            val background = json.optString("background").takeIf { it.isNotEmpty() }

            val typesList = mutableListOf<String>()
            val typesJson = json.optJSONArray("types")
            if (typesJson != null) {
                for (i in 0 until typesJson.length()) {
                    typesList.add(typesJson.getString(i))
                }
            } else {
                typesList.addAll(listOf("movie", "series"))
            }

            val resourcesList = mutableListOf<Any>()
            val resourcesJson = json.optJSONArray("resources")
            if (resourcesJson != null) {
                for (i in 0 until resourcesJson.length()) {
                    val item = resourcesJson.get(i)
                    if (item is String) {
                        resourcesList.add(item)
                    } else if (item is JSONObject) {
                        val map = mutableMapOf<String, Any>()
                        item.keys().forEach { k ->
                            map[k] = item.get(k)
                        }
                        resourcesList.add(map)
                    }
                }
            } else {
                resourcesList.addAll(listOf("catalog", "meta", "stream"))
            }

            val catalogsList = mutableListOf<AddonCatalog>()
            val catalogsJson = json.optJSONArray("catalogs")
            if (catalogsJson != null) {
                for (i in 0 until catalogsJson.length()) {
                    val catObj = catalogsJson.getJSONObject(i)
                    catalogsList.add(
                        AddonCatalog(
                            type = catObj.optString("type", "movie"),
                            id = catObj.optString("id", "top"),
                            name = catObj.optString("name", "Catalog")
                        )
                    )
                }
            }

            val prefixesList = mutableListOf<String>()
            val prefixesJson = json.optJSONArray("idPrefixes")
            if (prefixesJson != null) {
                for (i in 0 until prefixesJson.length()) {
                    prefixesList.add(prefixesJson.getString(i))
                }
            }

            val baseUrl = manifestUrl.substringBeforeLast("/") + "/"

            val manifest = AddonManifest(
                id = id,
                name = name,
                version = version,
                description = description,
                logo = logo,
                background = background,
                types = typesList,
                resources = resourcesList,
                catalogs = catalogsList,
                idPrefixes = if (prefixesList.isNotEmpty()) prefixesList else null,
                transportUrl = baseUrl,
                isEnabled = true,
                isBuiltin = false
            )

            Result.success(manifest)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching manifest for $addonUrl", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches catalog items from a remote addon
     */
    suspend fun fetchCatalog(
        manifest: AddonManifest,
        type: String,
        catalogId: String,
        searchQuery: String? = null,
        genre: String? = null
    ): List<MetaItem> = withContext(Dispatchers.IO) {
        if (manifest.transportUrl.startsWith("internal://")) {
            return@withContext emptyList()
        }

        try {
            var endpoint = "${manifest.transportUrl.removeSuffix("/")}/catalog/$type/$catalogId"
            if (!searchQuery.isNullOrBlank()) {
                val encoded = java.net.URLEncoder.encode(searchQuery.trim(), "UTF-8")
                endpoint += "/search=$encoded"
            } else if (!genre.isNullOrBlank()) {
                val encoded = java.net.URLEncoder.encode(genre.trim(), "UTF-8")
                endpoint += "/genre=$encoded"
            }
            endpoint += ".json"

            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", "NovaStream/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val metasArray = json.optJSONArray("metas") ?: return@withContext emptyList()

            val results = mutableListOf<MetaItem>()
            for (i in 0 until metasArray.length()) {
                val obj = metasArray.getJSONObject(i)
                val genres = mutableListOf<String>()
                val genresJson = obj.optJSONArray("genres")
                if (genresJson != null) {
                    for (g in 0 until genresJson.length()) {
                        genres.add(genresJson.getString(g))
                    }
                }

                results.add(
                    MetaItem(
                        id = obj.optString("id"),
                        type = obj.optString("type", type),
                        name = obj.optString("name"),
                        poster = obj.optString("poster").takeIf { it.isNotEmpty() },
                        background = obj.optString("background").takeIf { it.isNotEmpty() },
                        logo = obj.optString("logo").takeIf { it.isNotEmpty() },
                        description = obj.optString("description").takeIf { it.isNotEmpty() },
                        releaseInfo = obj.optString("releaseInfo", obj.optString("year")).takeIf { it.isNotEmpty() },
                        imdbRating = obj.optString("imdbRating").takeIf { it.isNotEmpty() },
                        genres = if (genres.isNotEmpty()) genres else null,
                        addonName = manifest.name,
                        addonId = manifest.id
                    )
                )
            }
            results
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch catalog from ${manifest.name}: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches detailed metadata from an addon
     */
    suspend fun fetchMeta(manifest: AddonManifest, type: String, id: String): MetaDetail? = withContext(Dispatchers.IO) {
        if (manifest.transportUrl.startsWith("internal://")) {
            return@withContext BuiltinAddons.getInternalMeta(id)
        }

        try {
            val endpoint = "${manifest.transportUrl.removeSuffix("/")}/meta/$type/$id.json"
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", "NovaStream/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val metaObj = json.optJSONObject("meta") ?: return@withContext null

            val genres = mutableListOf<String>()
            val genresJson = metaObj.optJSONArray("genres")
            if (genresJson != null) {
                for (g in 0 until genresJson.length()) {
                    genres.add(genresJson.getString(g))
                }
            }

            val directors = mutableListOf<String>()
            val dirJson = metaObj.optJSONArray("director")
            if (dirJson != null) {
                for (d in 0 until dirJson.length()) {
                    directors.add(dirJson.getString(d))
                }
            }

            val cast = mutableListOf<String>()
            val castJson = metaObj.optJSONArray("cast")
            if (castJson != null) {
                for (c in 0 until castJson.length()) {
                    cast.add(castJson.getString(c))
                }
            }

            val videos = mutableListOf<VideoEpisode>()
            val videosJson = metaObj.optJSONArray("videos")
            if (videosJson != null) {
                for (v in 0 until videosJson.length()) {
                    val vidObj = videosJson.getJSONObject(v)
                    videos.add(
                        VideoEpisode(
                            id = vidObj.optString("id", "${id}:${vidObj.optInt("season", 1)}:${vidObj.optInt("episode", 1)}"),
                            title = vidObj.optString("title", "Episode ${vidObj.optInt("episode", 1)}"),
                            released = vidObj.optString("released").takeIf { it.isNotEmpty() },
                            season = vidObj.optInt("season", 1),
                            episode = vidObj.optInt("episode", 1),
                            thumbnail = vidObj.optString("thumbnail").takeIf { it.isNotEmpty() },
                            overview = vidObj.optString("overview").takeIf { it.isNotEmpty() },
                            seasonTitle = vidObj.optString("seasonTitle", vidObj.optString("season_title")).takeIf { it.isNotEmpty() }
                        )
                    )
                }
            }

            MetaDetail(
                id = metaObj.optString("id", id),
                type = metaObj.optString("type", type),
                name = metaObj.optString("name", ""),
                poster = metaObj.optString("poster").takeIf { it.isNotEmpty() },
                background = metaObj.optString("background").takeIf { it.isNotEmpty() },
                logo = metaObj.optString("logo").takeIf { it.isNotEmpty() },
                description = metaObj.optString("description").takeIf { it.isNotEmpty() },
                releaseInfo = metaObj.optString("releaseInfo", metaObj.optString("year")).takeIf { it.isNotEmpty() },
                imdbRating = metaObj.optString("imdbRating").takeIf { it.isNotEmpty() },
                genres = if (genres.isNotEmpty()) genres else null,
                runtime = metaObj.optString("runtime").takeIf { it.isNotEmpty() },
                director = if (directors.isNotEmpty()) directors else null,
                cast = if (cast.isNotEmpty()) cast else null,
                videos = if (videos.isNotEmpty()) videos else null,
                trailer = metaObj.optString("trailer").takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch meta from ${manifest.name}: ${e.message}")
            null
        }
    }

    /**
     * Resolves streams from an addon
     */
    suspend fun fetchStreams(manifest: AddonManifest, type: String, id: String): List<StreamItem> = withContext(Dispatchers.IO) {
        if (manifest.transportUrl.startsWith("internal://")) {
            return@withContext BuiltinAddons.getStreams(id)
        }

        try {
            val endpoint = "${manifest.transportUrl.removeSuffix("/")}/stream/$type/$id.json"
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", "NovaStream/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val streamsArray = json.optJSONArray("streams") ?: return@withContext emptyList()

            val streams = mutableListOf<StreamItem>()
            for (i in 0 until streamsArray.length()) {
                val sObj = streamsArray.getJSONObject(i)
                streams.add(
                    StreamItem(
                        title = sObj.optString("title", sObj.optString("name", "Stream Source")),
                        name = sObj.optString("name", manifest.name),
                        url = sObj.optString("url").takeIf { it.isNotEmpty() },
                        externalUrl = sObj.optString("externalUrl").takeIf { it.isNotEmpty() },
                        ytId = sObj.optString("ytId").takeIf { it.isNotEmpty() },
                        infoHash = sObj.optString("infoHash").takeIf { it.isNotEmpty() },
                        fileIdx = if (sObj.has("fileIdx")) sObj.getInt("fileIdx") else null,
                        quality = when {
                            sObj.optString("title").contains("4K", ignoreCase = true) || sObj.optString("name").contains("4K", ignoreCase = true) -> "4K UHD"
                            sObj.optString("title").contains("1080p", ignoreCase = true) || sObj.optString("name").contains("1080p", ignoreCase = true) -> "1080p"
                            sObj.optString("title").contains("720p", ignoreCase = true) || sObj.optString("name").contains("720p", ignoreCase = true) -> "720p"
                            else -> "HD"
                        },
                        addonName = manifest.name
                    )
                )
            }
            streams
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch streams from ${manifest.name}: ${e.message}")
            emptyList()
        }
    }
}
