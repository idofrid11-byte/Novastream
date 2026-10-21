package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.BuiltinAddons
import com.example.data.api.StremioApiClient
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AddonRepository(
    private val context: Context,
    private val apiClient: StremioApiClient = StremioApiClient()
) {
    private val prefs = context.getSharedPreferences("novastream_addons_prefs", Context.MODE_PRIVATE)
    private val _installedAddons = MutableStateFlow<List<AddonManifest>>(emptyList())
    val installedAddons: StateFlow<List<AddonManifest>> = _installedAddons.asStateFlow()

    private val TAG = "AddonRepository"

    init {
        loadInstalledAddons()
    }

    private fun loadInstalledAddons() {
        val savedCustomJson = prefs.getString("custom_addons_json", "[]") ?: "[]"
        val customAddons = mutableListOf<AddonManifest>()
        try {
            val jsonArray = JSONArray(savedCustomJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val types = mutableListOf<String>()
                val typesArr = obj.optJSONArray("types")
                if (typesArr != null) {
                    for (t in 0 until typesArr.length()) types.add(typesArr.getString(t))
                }
                val catalogs = mutableListOf<AddonCatalog>()
                val catArr = obj.optJSONArray("catalogs")
                if (catArr != null) {
                    for (c in 0 until catArr.length()) {
                        val cObj = catArr.getJSONObject(c)
                        catalogs.add(AddonCatalog(type = cObj.getString("type"), id = cObj.getString("id"), name = cObj.getString("name")))
                    }
                }
                val prefixes = mutableListOf<String>()
                val prefArr = obj.optJSONArray("idPrefixes")
                if (prefArr != null) {
                    for (p in 0 until prefArr.length()) prefixes.add(prefArr.getString(p))
                }

                customAddons.add(
                    AddonManifest(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        version = obj.optString("version", "1.0.0"),
                        description = obj.optString("description"),
                        logo = obj.optString("logo").takeIf { it.isNotEmpty() },
                        types = types,
                        resources = listOf("catalog", "meta", "stream"),
                        catalogs = catalogs,
                        idPrefixes = if (prefixes.isNotEmpty()) prefixes else null,
                        transportUrl = obj.getString("transportUrl"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        isBuiltin = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved addons", e)
        }

        // Check disabled builtin addons
        val disabledBuiltinIds = prefs.getStringSet("disabled_builtin_ids", emptySet()) ?: emptySet()
        val builtin = BuiltinAddons.DEFAULT_ADDONS.map {
            it.copy(isEnabled = !disabledBuiltinIds.contains(it.id))
        }

        _installedAddons.value = builtin + customAddons
    }

    private fun saveCustomAddons() {
        val customAddons = _installedAddons.value.filter { !it.isBuiltin }
        val array = JSONArray()
        for (addon in customAddons) {
            val obj = JSONObject()
            obj.put("id", addon.id)
            obj.put("name", addon.name)
            obj.put("version", addon.version)
            obj.put("description", addon.description ?: "")
            obj.put("logo", addon.logo ?: "")
            obj.put("transportUrl", addon.transportUrl)
            obj.put("isEnabled", addon.isEnabled)

            val typesArr = JSONArray()
            addon.types.forEach { typesArr.put(it) }
            obj.put("types", typesArr)

            val catArr = JSONArray()
            addon.catalogs.forEach { c ->
                val cObj = JSONObject()
                cObj.put("type", c.type)
                cObj.put("id", c.id)
                cObj.put("name", c.name)
                catArr.put(cObj)
            }
            obj.put("catalogs", catArr)

            if (addon.idPrefixes != null) {
                val pArr = JSONArray()
                addon.idPrefixes.forEach { pArr.put(it) }
                obj.put("idPrefixes", pArr)
            }
            array.put(obj)
        }
        prefs.edit().putString("custom_addons_json", array.toString()).apply()

        val disabledBuiltin = _installedAddons.value.filter { it.isBuiltin && !it.isEnabled }.map { it.id }.toSet()
        prefs.edit().putStringSet("disabled_builtin_ids", disabledBuiltin).apply()
    }

    suspend fun installAddonFromUrl(url: String): Result<AddonManifest> {
        val result = apiClient.fetchManifest(url)
        if (result.isSuccess) {
            val manifest = result.getOrThrow()
            // Check if already installed
            val current = _installedAddons.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.id == manifest.id }
            if (existingIndex >= 0) {
                current[existingIndex] = manifest.copy(isEnabled = true, isBuiltin = false)
            } else {
                current.add(manifest.copy(isEnabled = true, isBuiltin = false))
            }
            _installedAddons.value = current
            saveCustomAddons()
            return Result.success(manifest)
        }
        return result
    }

    fun toggleAddon(addonId: String, enabled: Boolean) {
        val current = _installedAddons.value.toMutableList()
        val index = current.indexOfFirst { it.id == addonId }
        if (index >= 0) {
            current[index] = current[index].copy(isEnabled = enabled)
            _installedAddons.value = current
            saveCustomAddons()
        }
    }

    fun uninstallAddon(addonId: String) {
        val current = _installedAddons.value.toMutableList()
        val toRemove = current.find { it.id == addonId }
        if (toRemove != null && !toRemove.isBuiltin) {
            current.remove(toRemove)
            _installedAddons.value = current
            saveCustomAddons()
        }
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _installedAddons.value = BuiltinAddons.DEFAULT_ADDONS
    }

    /**
     * Aggregates catalog rows from all active addons
     */
    suspend fun loadCatalogsForSection(mediaType: String, genre: String? = null): Map<String, List<MetaItem>> {
        val activeAddons = _installedAddons.value.filter { it.isEnabled }
        val results = mutableMapOf<String, List<MetaItem>>()

        // First add curated internal addons
        if (activeAddons.any { it.id == BuiltinAddons.BLENDER_OPEN_CINEMA.id }) {
            val items = BuiltinAddons.BLENDER_MOVIES.filter {
                (mediaType == "all" || it.type == mediaType) &&
                        (genre == null || it.genres?.contains(genre) == true)
            }
            if (items.isNotEmpty()) {
                results["Blender Open 4K Showcase"] = items
            }
        }

        if (activeAddons.any { it.id == BuiltinAddons.PUBLIC_DOMAIN_CLASSICS.id } && (mediaType == "all" || mediaType == "movie")) {
            val items = BuiltinAddons.PUBLIC_DOMAIN_MOVIES.filter {
                genre == null || it.genres?.contains(genre) == true
            }
            if (items.isNotEmpty()) {
                results["Public Domain Masterpieces"] = items
            }
        }

        if (activeAddons.any { it.id == BuiltinAddons.NASA_SPACE_STREAMS.id } && (mediaType == "all" || mediaType == "channel" || mediaType == "movie")) {
            val items = BuiltinAddons.NASA_ITEMS.filter {
                mediaType == "all" || it.type == mediaType
            }
            if (items.isNotEmpty()) {
                results["NASA Space & Live Cameras"] = items
            }
        }

        // Now query external addons (like Cinemeta)
        for (addon in activeAddons) {
            if (addon.transportUrl.startsWith("internal://")) continue
            if (!addon.supportsResource("catalog")) continue

            for (catalog in addon.catalogs) {
                if (mediaType != "all" && catalog.type != mediaType) continue
                try {
                    val items = apiClient.fetchCatalog(addon, catalog.type, catalog.id, genre = genre)
                    if (items.isNotEmpty()) {
                        results["${addon.name} - ${catalog.name}"] = items
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching catalog ${catalog.name} from ${addon.name}: ${e.message}")
                }
            }
        }

        return results
    }

    /**
     * Searches across all installed and active addons
     */
    suspend fun searchAllAddons(query: String, type: String = "all"): List<MetaItem> {
        if (query.isBlank()) return emptyList()

        val activeAddons = _installedAddons.value.filter { it.isEnabled }
        val aggregated = mutableListOf<MetaItem>()
        val seenIds = mutableSetOf<String>()

        // 1. Search internal collections
        val internalPool = BuiltinAddons.BLENDER_MOVIES + BuiltinAddons.PUBLIC_DOMAIN_MOVIES + BuiltinAddons.NASA_ITEMS
        val matchedInternal = internalPool.filter { item ->
            (type == "all" || item.type == type) &&
                    (item.name.contains(query, ignoreCase = true) ||
                            item.description?.contains(query, ignoreCase = true) == true ||
                            item.genres?.any { it.contains(query, ignoreCase = true) } == true)
        }
        for (item in matchedInternal) {
            if (seenIds.add(item.id)) aggregated.add(item)
        }

        // 2. Search external addons concurrently
        val externalAddons = activeAddons.filter { !it.transportUrl.startsWith("internal://") && it.supportsResource("catalog") }

        val searchTypes = if (type == "all") listOf("movie", "series") else listOf(type)

        for (addon in externalAddons) {
            for (st in searchTypes) {
                val cat = addon.catalogs.firstOrNull { it.type == st } ?: AddonCatalog(st, "top", "Search")
                val results = apiClient.fetchCatalog(addon, st, cat.id, searchQuery = query)
                for (res in results) {
                    if (seenIds.add(res.id)) {
                        aggregated.add(res)
                    }
                }
            }
        }

        return aggregated
    }

    /**
     * Resolves metadata for an item across all addons
     */
    suspend fun resolveMeta(id: String, type: String): MetaDetail? {
        // Check internal addons first
        val internalMeta = BuiltinAddons.getInternalMeta(id)
        if (internalMeta != null) return internalMeta

        val activeAddons = _installedAddons.value.filter { it.isEnabled && it.supportsResource("meta") }
        for (addon in activeAddons) {
            if (addon.matchesIdPrefix(id)) {
                val meta = apiClient.fetchMeta(addon, type, id)
                if (meta != null) return meta
            }
        }
        return null
    }

    /**
     * Resolves all available streams for an item across all installed addons
     */
    suspend fun resolveStreams(id: String, type: String): List<StreamItem> {
        val activeAddons = _installedAddons.value.filter { it.isEnabled && it.supportsResource("stream") }
        val streamList = mutableListOf<StreamItem>()

        // 1. Check internal streams
        val internalStreams = BuiltinAddons.getStreams(id)
        streamList.addAll(internalStreams)

        // 2. Query external addons that support stream
        for (addon in activeAddons) {
            if (addon.transportUrl.startsWith("internal://")) continue
            if (addon.matchesIdPrefix(id)) {
                val streams = apiClient.fetchStreams(addon, type, id)
                streamList.addAll(streams)
            }
        }

        // If no streams found, fallback to sample multi-CDN stream so user always has playback
        if (streamList.isEmpty()) {
            streamList.addAll(BuiltinAddons.getStreams(id))
        }

        return streamList
    }
}
