package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.LibraryItem
import com.example.data.model.MetaDetail
import com.example.data.model.MetaItem
import com.example.data.model.WatchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class LibraryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("novastream_library_prefs", Context.MODE_PRIVATE)
    private val _libraryItems = MutableStateFlow<List<LibraryItem>>(emptyList())
    val libraryItems: StateFlow<List<LibraryItem>> = _libraryItems.asStateFlow()

    private val TAG = "LibraryRepository"

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        val jsonStr = prefs.getString("library_items_json", "[]") ?: "[]"
        val list = mutableListOf<LibraryItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val genres = mutableListOf<String>()
                val gArr = obj.optJSONArray("genres")
                if (gArr != null) {
                    for (g in 0 until gArr.length()) genres.add(gArr.getString(g))
                }

                val statusStr = obj.optString("status", WatchStatus.WATCHING.name)
                val status = try {
                    WatchStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    WatchStatus.WATCHING
                }

                list.add(
                    LibraryItem(
                        id = obj.getString("id"),
                        type = obj.optString("type", "movie"),
                        name = obj.getString("name"),
                        poster = obj.optString("poster").takeIf { it.isNotEmpty() },
                        background = obj.optString("background").takeIf { it.isNotEmpty() },
                        releaseInfo = obj.optString("releaseInfo").takeIf { it.isNotEmpty() },
                        imdbRating = obj.optString("imdbRating").takeIf { it.isNotEmpty() },
                        genres = if (genres.isNotEmpty()) genres else null,
                        status = status,
                        watchPositionMs = obj.optLong("watchPositionMs", 0L),
                        durationMs = obj.optLong("durationMs", 0L),
                        lastEpisodeId = obj.optString("lastEpisodeId").takeIf { it.isNotEmpty() },
                        lastEpisodeTitle = obj.optString("lastEpisodeTitle").takeIf { it.isNotEmpty() },
                        lastWatchedTimestamp = obj.optLong("lastWatchedTimestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading library", e)
        }

        // If library is empty on first launch, seed with 2 friendly sample items so user immediately sees rich UI
        if (list.isEmpty()) {
            list.add(
                LibraryItem(
                    id = "blender_big_buck_bunny",
                    type = "movie",
                    name = "Big Buck Bunny",
                    poster = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=800",
                    background = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=1600",
                    releaseInfo = "2008",
                    imdbRating = "8.1",
                    genres = listOf("Animation", "Comedy"),
                    status = WatchStatus.WATCHING,
                    watchPositionMs = 210000L,
                    durationMs = 596000L,
                    lastWatchedTimestamp = System.currentTimeMillis()
                )
            )
            list.add(
                LibraryItem(
                    id = "blender_tears_of_steel",
                    type = "movie",
                    name = "Tears of Steel",
                    poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=800",
                    background = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1600",
                    releaseInfo = "2012",
                    imdbRating = "7.8",
                    genres = listOf("Sci-Fi", "Action"),
                    status = WatchStatus.PLAN_TO_WATCH,
                    watchPositionMs = 0L,
                    durationMs = 734000L,
                    lastWatchedTimestamp = System.currentTimeMillis() - 86400000L
                )
            )
        }

        _libraryItems.value = list
        saveLibrary()
    }

    private fun saveLibrary() {
        val arr = JSONArray()
        for (item in _libraryItems.value) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("type", item.type)
            obj.put("name", item.name)
            obj.put("poster", item.poster ?: "")
            obj.put("background", item.background ?: "")
            obj.put("releaseInfo", item.releaseInfo ?: "")
            obj.put("imdbRating", item.imdbRating ?: "")
            obj.put("status", item.status.name)
            obj.put("watchPositionMs", item.watchPositionMs)
            obj.put("durationMs", item.durationMs)
            obj.put("lastEpisodeId", item.lastEpisodeId ?: "")
            obj.put("lastEpisodeTitle", item.lastEpisodeTitle ?: "")
            obj.put("lastWatchedTimestamp", item.lastWatchedTimestamp)

            if (item.genres != null) {
                val gArr = JSONArray()
                item.genres.forEach { gArr.put(it) }
                obj.put("genres", gArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("library_items_json", arr.toString()).apply()
    }

    fun isSaved(id: String): Boolean {
        return _libraryItems.value.any { it.id == id }
    }

    fun getItem(id: String): LibraryItem? {
        return _libraryItems.value.find { it.id == id }
    }

    fun toggleLibrary(meta: MetaDetail, status: WatchStatus = WatchStatus.PLAN_TO_WATCH) {
        val current = _libraryItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == meta.id }
        if (index >= 0) {
            current.removeAt(index)
        } else {
            current.add(
                0,
                LibraryItem(
                    id = meta.id,
                    type = meta.type,
                    name = meta.name,
                    poster = meta.poster,
                    background = meta.background,
                    releaseInfo = meta.releaseInfo,
                    imdbRating = meta.imdbRating,
                    genres = meta.genres,
                    status = status,
                    lastWatchedTimestamp = System.currentTimeMillis()
                )
            )
        }
        _libraryItems.value = current
        saveLibrary()
    }

    fun setStatus(id: String, status: WatchStatus) {
        val current = _libraryItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(status = status)
            _libraryItems.value = current
            saveLibrary()
        }
    }

    fun removeFromLibrary(id: String) {
        val current = _libraryItems.value.toMutableList()
        if (current.removeAll { it.id == id }) {
            _libraryItems.value = current
            saveLibrary()
        }
    }

    fun updateProgress(
        id: String,
        positionMs: Long,
        durationMs: Long,
        episodeId: String? = null,
        episodeTitle: String? = null,
        metaFallback: MetaDetail? = null
    ) {
        val current = _libraryItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }

        val isFinished = durationMs > 0 && (positionMs.toFloat() / durationMs.toFloat()) > 0.95f
        val newStatus = if (isFinished) WatchStatus.COMPLETED else WatchStatus.WATCHING

        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(
                watchPositionMs = positionMs,
                durationMs = durationMs,
                lastEpisodeId = episodeId ?: existing.lastEpisodeId,
                lastEpisodeTitle = episodeTitle ?: existing.lastEpisodeTitle,
                status = if (existing.status == WatchStatus.FAVORITE) WatchStatus.FAVORITE else newStatus,
                lastWatchedTimestamp = System.currentTimeMillis()
            )
        } else if (metaFallback != null) {
            current.add(
                0,
                LibraryItem(
                    id = metaFallback.id,
                    type = metaFallback.type,
                    name = metaFallback.name,
                    poster = metaFallback.poster,
                    background = metaFallback.background,
                    releaseInfo = metaFallback.releaseInfo,
                    imdbRating = metaFallback.imdbRating,
                    genres = metaFallback.genres,
                    status = newStatus,
                    watchPositionMs = positionMs,
                    durationMs = durationMs,
                    lastEpisodeId = episodeId,
                    lastEpisodeTitle = episodeTitle,
                    lastWatchedTimestamp = System.currentTimeMillis()
                )
            )
        }
        _libraryItems.value = current
        saveLibrary()
    }
}
