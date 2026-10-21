package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.LibraryItem
import com.example.data.model.MetaDetail
import com.example.data.model.WatchStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class LibraryRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("novastream_library_prefs", Context.MODE_PRIVATE)
    private val _libraryItems = MutableStateFlow<List<LibraryItem>>(emptyList())
    val libraryItems: StateFlow<List<LibraryItem>> = _libraryItems.asStateFlow()

    private val _watchedEpisodes = MutableStateFlow<Set<String>>(emptySet())
    val watchedEpisodes: StateFlow<Set<String>> = _watchedEpisodes.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "LibraryRepository"

    init {
        loadLibrary()
        loadWatchedEpisodes()
        // Listen to auth state to sync with Firestore
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                syncFromCloud(user.uid)
            }
        }
    }

    private fun getFirestoreDb(): FirebaseFirestore {
        val dbId = context.getString(R.string.firestore_database_id)
        return FirebaseFirestore.getInstance(dbId)
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

        // Default friendly initial items on very first install
        if (list.isEmpty() && !prefs.contains("library_initialized")) {
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
            prefs.edit().putBoolean("library_initialized", true).apply()
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

    private fun syncItemToCloud(item: LibraryItem) {
        val uid = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestoreDb()
                val payload = mutableMapOf<String, Any>(
                    "id" to item.id,
                    "userId" to uid,
                    "type" to item.type,
                    "name" to item.name,
                    "status" to item.status.name,
                    "watchPositionMs" to item.watchPositionMs,
                    "durationMs" to item.durationMs,
                    "lastWatchedTimestamp" to item.lastWatchedTimestamp,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                item.poster?.let { payload["poster"] = it }
                item.background?.let { payload["background"] = it }
                item.releaseInfo?.let { payload["releaseInfo"] = it }
                item.imdbRating?.let { payload["imdbRating"] = it }
                item.lastEpisodeId?.let { payload["lastEpisodeId"] = it }
                item.lastEpisodeTitle?.let { payload["lastEpisodeTitle"] = it }

                val docRef = db.collection("users").document(uid).collection("library").document(item.id)
                val snapshot = docRef.get().await()
                if (!snapshot.exists()) {
                    payload["createdAt"] = FieldValue.serverTimestamp()
                }

                docRef.set(payload, SetOptions.merge()).await()
                Log.d(TAG, "Item ${item.id} synchronized to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync item ${item.id} to Firestore: ${e.message}")
            }
        }
    }

    private fun removeItemFromCloud(itemId: String) {
        val uid = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestoreDb()
                db.collection("users").document(uid).collection("library").document(itemId).delete().await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete item $itemId from Firestore: ${e.message}")
            }
        }
    }

    fun syncFromCloud(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = getFirestoreDb()
                val snapshot = db.collection("users").document(uid).collection("library").get().await()
                if (!snapshot.isEmpty) {
                    val cloudItems = mutableListOf<LibraryItem>()
                    for (doc in snapshot.documents) {
                        val statusStr = doc.getString("status") ?: WatchStatus.WATCHING.name
                        val status = try {
                            WatchStatus.valueOf(statusStr)
                        } catch (e: Exception) {
                            WatchStatus.WATCHING
                        }

                        cloudItems.add(
                            LibraryItem(
                                id = doc.getString("id") ?: doc.id,
                                type = doc.getString("type") ?: "movie",
                                name = doc.getString("name") ?: "Title",
                                poster = doc.getString("poster"),
                                background = doc.getString("background"),
                                releaseInfo = doc.getString("releaseInfo"),
                                imdbRating = doc.getString("imdbRating"),
                                genres = null,
                                status = status,
                                watchPositionMs = doc.getLong("watchPositionMs") ?: 0L,
                                durationMs = doc.getLong("durationMs") ?: 0L,
                                lastEpisodeId = doc.getString("lastEpisodeId"),
                                lastEpisodeTitle = doc.getString("lastEpisodeTitle"),
                                lastWatchedTimestamp = doc.getLong("lastWatchedTimestamp") ?: System.currentTimeMillis()
                            )
                        )
                    }

                    // Merge cloud items with local items
                    val currentMap = _libraryItems.value.associateBy { it.id }.toMutableMap()
                    for (ci in cloudItems) {
                        val existing = currentMap[ci.id]
                        if (existing == null || ci.lastWatchedTimestamp > existing.lastWatchedTimestamp) {
                            currentMap[ci.id] = ci
                        }
                    }
                    _libraryItems.value = currentMap.values.toList()
                    saveLibrary()
                } else {
                    // Upload existing local items to cloud
                    for (localItem in _libraryItems.value) {
                        syncItemToCloud(localItem)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing library from cloud: ${e.message}")
            }
        }
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
            val removed = current.removeAt(index)
            removeItemFromCloud(removed.id)
        } else {
            val newItem = LibraryItem(
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
            current.add(0, newItem)
            syncItemToCloud(newItem)
        }
        _libraryItems.value = current
        saveLibrary()
    }

    fun setStatus(id: String, status: WatchStatus) {
        val current = _libraryItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updated = current[index].copy(status = status)
            current[index] = updated
            _libraryItems.value = current
            saveLibrary()
            syncItemToCloud(updated)
        }
    }

    fun removeFromLibrary(id: String) {
        val current = _libraryItems.value.toMutableList()
        if (current.removeAll { it.id == id }) {
            _libraryItems.value = current
            saveLibrary()
            removeItemFromCloud(id)
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

        var itemToSync: LibraryItem? = null

        if (index >= 0) {
            val existing = current[index]
            val updated = existing.copy(
                watchPositionMs = positionMs,
                durationMs = durationMs,
                lastEpisodeId = episodeId ?: existing.lastEpisodeId,
                lastEpisodeTitle = episodeTitle ?: existing.lastEpisodeTitle,
                status = if (existing.status == WatchStatus.FAVORITE) WatchStatus.FAVORITE else newStatus,
                lastWatchedTimestamp = System.currentTimeMillis()
            )
            current[index] = updated
            itemToSync = updated
        } else if (metaFallback != null) {
            val newItem = LibraryItem(
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
            current.add(0, newItem)
            itemToSync = newItem
        }
        _libraryItems.value = current
        saveLibrary()

        itemToSync?.let { syncItemToCloud(it) }
    }

    private fun loadWatchedEpisodes() {
        val set = prefs.getStringSet("watched_episodes_ids", emptySet()) ?: emptySet()
        _watchedEpisodes.value = set.toSet()
    }

    private fun saveWatchedEpisodes() {
        prefs.edit().putStringSet("watched_episodes_ids", _watchedEpisodes.value).apply()
    }

    fun isEpisodeWatched(episodeId: String): Boolean {
        return _watchedEpisodes.value.contains(episodeId)
    }

    fun toggleEpisodeWatched(episodeId: String, seriesMeta: MetaDetail? = null, episodeTitle: String? = null) {
        val current = _watchedEpisodes.value.toMutableSet()
        val willBeWatched = !current.contains(episodeId)
        if (willBeWatched) {
            current.add(episodeId)
        } else {
            current.remove(episodeId)
        }
        _watchedEpisodes.value = current
        saveWatchedEpisodes()

        if (seriesMeta != null && willBeWatched) {
            updateProgress(
                id = seriesMeta.id,
                positionMs = 1000L,
                durationMs = 1000L,
                episodeId = episodeId,
                episodeTitle = episodeTitle,
                metaFallback = seriesMeta
            )
        }
    }

    fun markSeasonWatched(episodeIds: List<String>, isWatched: Boolean, seriesMeta: MetaDetail? = null) {
        val current = _watchedEpisodes.value.toMutableSet()
        if (isWatched) {
            current.addAll(episodeIds)
        } else {
            current.removeAll(episodeIds.toSet())
        }
        _watchedEpisodes.value = current
        saveWatchedEpisodes()

        if (isWatched && seriesMeta != null && episodeIds.isNotEmpty()) {
            val lastId = episodeIds.last()
            updateProgress(
                id = seriesMeta.id,
                positionMs = 1000L,
                durationMs = 1000L,
                episodeId = lastId,
                episodeTitle = "Season Complete",
                metaFallback = seriesMeta
            )
        }
    }

    fun isMovieWatched(id: String): Boolean {
        val item = getItem(id)
        return item != null && (item.status == WatchStatus.COMPLETED || item.progressFraction >= 0.95f)
    }

    fun toggleMovieWatched(meta: MetaDetail) {
        val item = getItem(meta.id)
        val currentlyWatched = item != null && (item.status == WatchStatus.COMPLETED || item.progressFraction >= 0.95f)
        val current = _libraryItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == meta.id }

        if (currentlyWatched) {
            if (index >= 0) {
                val updated = current[index].copy(
                    status = WatchStatus.PLAN_TO_WATCH,
                    watchPositionMs = 0L
                )
                current[index] = updated
                _libraryItems.value = current
                saveLibrary()
                syncItemToCloud(updated)
            }
        } else {
            val dur = if (index >= 0 && current[index].durationMs > 0) current[index].durationMs else 3600000L
            val updated = if (index >= 0) {
                current[index].copy(
                    status = WatchStatus.COMPLETED,
                    watchPositionMs = dur,
                    durationMs = dur,
                    lastWatchedTimestamp = System.currentTimeMillis()
                )
            } else {
                LibraryItem(
                    id = meta.id,
                    type = meta.type,
                    name = meta.name,
                    poster = meta.poster,
                    background = meta.background,
                    releaseInfo = meta.releaseInfo,
                    imdbRating = meta.imdbRating,
                    genres = meta.genres,
                    status = WatchStatus.COMPLETED,
                    watchPositionMs = dur,
                    durationMs = dur,
                    lastWatchedTimestamp = System.currentTimeMillis()
                )
            }
            if (index >= 0) {
                current[index] = updated
            } else {
                current.add(0, updated)
            }
            _libraryItems.value = current
            saveLibrary()
            syncItemToCloud(updated)
        }
    }
}
