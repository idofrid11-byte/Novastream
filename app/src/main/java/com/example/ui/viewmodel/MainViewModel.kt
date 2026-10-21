package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.BuiltinAddons
import com.example.data.model.*
import com.example.data.repository.AddonRepository
import com.example.data.repository.LibraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    DISCOVER,
    SEARCH,
    LIBRARY,
    ADDONS
}

sealed class ScreenDestination {
    object Tabs : ScreenDestination()
    data class Detail(val id: String, val type: String) : ScreenDestination()
    data class Player(
        val stream: StreamItem,
        val title: String,
        val subtitle: String?,
        val itemId: String,
        val startPositionMs: Long = 0L,
        val meta: MetaDetail? = null,
        val episode: VideoEpisode? = null
    ) : ScreenDestination()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val addonRepository = AddonRepository(application.applicationContext)
    private val libraryRepository = LibraryRepository(application.applicationContext)

    val installedAddons: StateFlow<List<AddonManifest>> = addonRepository.installedAddons
    val libraryItems: StateFlow<List<LibraryItem>> = libraryRepository.libraryItems

    // Navigation state
    private val _currentTab = MutableStateFlow(AppTab.DISCOVER)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _screenDestination = MutableStateFlow<ScreenDestination>(ScreenDestination.Tabs)
    val screenDestination: StateFlow<ScreenDestination> = _screenDestination.asStateFlow()

    // Home / Discover State
    private val _selectedMediaType = MutableStateFlow("all")
    val selectedMediaType: StateFlow<String> = _selectedMediaType.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _catalogSections = MutableStateFlow<Map<String, List<MetaItem>>>(emptyMap())
    val catalogSections: StateFlow<Map<String, List<MetaItem>>> = _catalogSections.asStateFlow()

    private val _heroItem = MutableStateFlow<MetaItem?>(null)
    val heroItem: StateFlow<MetaItem?> = _heroItem.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(false)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilterType = MutableStateFlow("all")
    val searchFilterType: StateFlow<String> = _searchFilterType.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MetaItem>>(emptyList())
    val searchResults: StateFlow<List<MetaItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Detail State
    private val _currentMeta = MutableStateFlow<MetaDetail?>(null)
    val currentMeta: StateFlow<MetaDetail?> = _currentMeta.asStateFlow()

    private val _currentStreams = MutableStateFlow<List<StreamItem>>(emptyList())
    val currentStreams: StateFlow<List<StreamItem>> = _currentStreams.asStateFlow()

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    private val _isLoadingStreams = MutableStateFlow(false)
    val isLoadingStreams: StateFlow<Boolean> = _isLoadingStreams.asStateFlow()

    // Addons State
    private val _installUrl = MutableStateFlow("")
    val installUrl: StateFlow<String> = _installUrl.asStateFlow()

    private val _isInstallingAddon = MutableStateFlow(false)
    val isInstallingAddon: StateFlow<Boolean> = _isInstallingAddon.asStateFlow()

    private val _addonMessage = MutableStateFlow<String?>(null)
    val addonMessage: StateFlow<String?> = _addonMessage.asStateFlow()

    // Search debounce job
    private var searchJob: Job? = null

    init {
        loadHomeCatalogs()
    }

    fun switchTab(tab: AppTab) {
        _currentTab.value = tab
        if (_screenDestination.value !is ScreenDestination.Tabs) {
            _screenDestination.value = ScreenDestination.Tabs
        }
    }

    fun navigateToDetail(id: String, type: String) {
        _screenDestination.value = ScreenDestination.Detail(id, type)
        loadDetail(id, type)
    }

    fun navigateToPlayer(
        stream: StreamItem,
        title: String,
        subtitle: String? = null,
        itemId: String,
        startPositionMs: Long = 0L,
        meta: MetaDetail? = null,
        episode: VideoEpisode? = null
    ) {
        _screenDestination.value = ScreenDestination.Player(
            stream = stream,
            title = title,
            subtitle = subtitle,
            itemId = itemId,
            startPositionMs = startPositionMs,
            meta = meta,
            episode = episode
        )
    }

    fun navigateBack(): Boolean {
        return when (_screenDestination.value) {
            is ScreenDestination.Player -> {
                _screenDestination.value = if (_currentMeta.value != null) {
                    ScreenDestination.Detail(_currentMeta.value!!.id, _currentMeta.value!!.type)
                } else {
                    ScreenDestination.Tabs
                }
                true
            }
            is ScreenDestination.Detail -> {
                _screenDestination.value = ScreenDestination.Tabs
                true
            }
            is ScreenDestination.Tabs -> {
                if (_currentTab.value != AppTab.DISCOVER) {
                    _currentTab.value = AppTab.DISCOVER
                    true
                } else {
                    false
                }
            }
        }
    }

    fun setMediaTypeFilter(type: String) {
        if (_selectedMediaType.value == type) return
        _selectedMediaType.value = type
        loadHomeCatalogs()
    }

    fun setGenreFilter(genre: String?) {
        _selectedGenre.value = genre
        loadHomeCatalogs()
    }

    fun loadHomeCatalogs() {
        viewModelScope.launch {
            _isLoadingHome.value = true
            try {
                val catalogs = addonRepository.loadCatalogsForSection(
                    mediaType = _selectedMediaType.value,
                    genre = _selectedGenre.value
                )
                _catalogSections.value = catalogs

                // Pick a charismatic hero item
                val firstList = catalogs.values.firstOrNull { it.isNotEmpty() }
                if (firstList != null && _heroItem.value == null) {
                    _heroItem.value = firstList.firstOrNull { it.background != null } ?: firstList.firstOrNull()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading catalogs", e)
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    private fun loadDetail(id: String, type: String) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            _currentMeta.value = null
            _currentStreams.value = emptyList()
            _selectedSeason.value = 1

            try {
                val meta = addonRepository.resolveMeta(id, type)
                _currentMeta.value = meta

                // If series, automatically check if episode saved or first episode
                val streamTargetId = if (meta?.type == "series" && !meta.videos.isNullOrEmpty()) {
                    val firstVid = meta.videos.first()
                    firstVid.id
                } else {
                    id
                }

                loadStreamsForTarget(streamTargetId, type)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading detail", e)
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun selectEpisode(episode: VideoEpisode) {
        val meta = _currentMeta.value ?: return
        loadStreamsForTarget(episode.id, meta.type)
    }

    fun selectSeason(season: Int) {
        _selectedSeason.value = season
    }

    fun loadStreamsForTarget(targetId: String, type: String) {
        viewModelScope.launch {
            _isLoadingStreams.value = true
            try {
                val streams = addonRepository.resolveStreams(targetId, type)
                _currentStreams.value = streams
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error resolving streams", e)
            } finally {
                _isLoadingStreams.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            _isSearching.value = true
            try {
                val results = addonRepository.searchAllAddons(query, _searchFilterType.value)
                _searchResults.value = results
            } catch (e: Exception) {
                Log.e("MainViewModel", "Search error", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun setSearchFilterType(type: String) {
        _searchFilterType.value = type
        if (_searchQuery.value.isNotBlank()) {
            setSearchQuery(_searchQuery.value)
        }
    }

    // Library operations
    fun isSavedInLibrary(id: String): Boolean {
        return libraryRepository.isSaved(id)
    }

    fun toggleLibrary(meta: MetaDetail, status: WatchStatus = WatchStatus.PLAN_TO_WATCH) {
        libraryRepository.toggleLibrary(meta, status)
    }

    fun setLibraryStatus(id: String, status: WatchStatus) {
        libraryRepository.setStatus(id, status)
    }

    fun removeFromLibrary(id: String) {
        libraryRepository.removeFromLibrary(id)
    }

    fun updateProgress(
        id: String,
        positionMs: Long,
        durationMs: Long,
        episodeId: String? = null,
        episodeTitle: String? = null
    ) {
        libraryRepository.updateProgress(
            id = id,
            positionMs = positionMs,
            durationMs = durationMs,
            episodeId = episodeId,
            episodeTitle = episodeTitle,
            metaFallback = _currentMeta.value
        )
    }

    fun getContinueWatchingPosition(id: String): Long {
        return libraryRepository.getItem(id)?.watchPositionMs ?: 0L
    }

    // Addon operations
    fun setInstallUrl(url: String) {
        _installUrl.value = url
    }

    fun installAddon(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _isInstallingAddon.value = true
            _addonMessage.value = null
            try {
                val result = addonRepository.installAddonFromUrl(url)
                if (result.isSuccess) {
                    val manifest = result.getOrThrow()
                    _addonMessage.value = "Successfully installed \"${manifest.name}\" (v${manifest.version})!"
                    _installUrl.value = ""
                    loadHomeCatalogs()
                } else {
                    _addonMessage.value = "Failed: ${result.exceptionOrNull()?.message ?: "Invalid addon URL"}"
                }
            } catch (e: Exception) {
                _addonMessage.value = "Install failed: ${e.message}"
            } finally {
                _isInstallingAddon.value = false
            }
        }
    }

    fun toggleAddon(addonId: String, enabled: Boolean) {
        addonRepository.toggleAddon(addonId, enabled)
        loadHomeCatalogs()
    }

    fun uninstallAddon(addonId: String) {
        addonRepository.uninstallAddon(addonId)
        loadHomeCatalogs()
    }

    fun resetAddonsToDefaults() {
        addonRepository.resetToDefaults()
        _addonMessage.value = "Restored default addons"
        loadHomeCatalogs()
    }

    fun clearAddonMessage() {
        _addonMessage.value = null
    }
}
