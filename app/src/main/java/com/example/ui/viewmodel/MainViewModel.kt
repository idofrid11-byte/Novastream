package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.BuiltinAddons
import com.example.data.api.StreamingServicesData
import com.example.data.auth.AuthManager
import com.example.data.model.*
import com.example.data.repository.AddonRepository
import com.example.data.repository.LibraryRepository
import com.example.data.settings.PlaybackSettings
import com.example.data.settings.PlaybackSettingsManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab {
    DISCOVER,
    STREAMING,
    SEARCH,
    WATCHLIST,
    SETTINGS,
    LIBRARY, // Backward compatibility alias
    ADDONS,  // Backward compatibility alias
    ACCOUNT  // Backward compatibility alias
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

    private val authManager = AuthManager(application.applicationContext)
    private val addonRepository = AddonRepository(application.applicationContext)
    private val libraryRepository = LibraryRepository(application.applicationContext)
    private val playbackSettingsManager = PlaybackSettingsManager(application.applicationContext)

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val authError: StateFlow<String?> = authManager.authError
    val isAuthLoading: StateFlow<Boolean> = authManager.isLoading

    val installedAddons: StateFlow<List<AddonManifest>> = addonRepository.installedAddons
    val libraryItems: StateFlow<List<LibraryItem>> = libraryRepository.libraryItems
    val watchedEpisodes: StateFlow<Set<String>> = libraryRepository.watchedEpisodes

    // Playback Settings
    val playbackSettings: StateFlow<PlaybackSettings> = playbackSettingsManager.settings

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

    // Streaming Services Tab State
    private val _selectedStreamingProvider = MutableStateFlow("netflix")
    val selectedStreamingProvider: StateFlow<String> = _selectedStreamingProvider.asStateFlow()

    private val _streamingCatalogItems = MutableStateFlow<List<MetaItem>>(StreamingServicesData.getItemsForProvider("netflix"))
    val streamingCatalogItems: StateFlow<List<MetaItem>> = _streamingCatalogItems.asStateFlow()

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

    private val _isDirectPlaying = MutableStateFlow(false)
    val isDirectPlaying: StateFlow<Boolean> = _isDirectPlaying.asStateFlow()

    // Addons State
    private val _installUrl = MutableStateFlow("")
    val installUrl: StateFlow<String> = _installUrl.asStateFlow()

    private val _isInstallingAddon = MutableStateFlow(false)
    val isInstallingAddon: StateFlow<Boolean> = _isInstallingAddon.asStateFlow()

    private val _addonMessage = MutableStateFlow<String?>(null)
    val addonMessage: StateFlow<String?> = _addonMessage.asStateFlow()

    // Sync State
    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    // Search debounce job
    private var searchJob: Job? = null

    init {
        loadHomeCatalogs()
        authManager.attemptAutoSignIn(viewModelScope)
    }

    fun switchTab(tab: AppTab) {
        // Map backward compatibility aliases
        val targetTab = when (tab) {
            AppTab.LIBRARY -> AppTab.WATCHLIST
            AppTab.ACCOUNT, AppTab.ADDONS -> AppTab.SETTINGS
            else -> tab
        }
        _currentTab.value = targetTab
        if (_screenDestination.value !is ScreenDestination.Tabs) {
            _screenDestination.value = ScreenDestination.Tabs
        }
    }

    fun selectStreamingProvider(providerId: String) {
        _selectedStreamingProvider.value = providerId
        _streamingCatalogItems.value = addonRepository.loadStreamingCatalog(providerId)
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

    /**
     * WuPlay Direct Play: Resolves the best stream automatically in the background
     * and jumps immediately to PlayerScreen without prompting for stream selection.
     */
    fun playDirect(meta: MetaDetail, episode: VideoEpisode? = null) {
        viewModelScope.launch {
            _isDirectPlaying.value = true
            try {
                val targetId = episode?.id ?: meta.id
                val streams = addonRepository.resolveStreams(targetId, meta.type)
                val bestStream = streams.firstOrNull { it.quality?.contains("1080", ignoreCase = true) == true }
                    ?: streams.firstOrNull { it.quality?.contains("4K", ignoreCase = true) == true }
                    ?: streams.firstOrNull()
                    ?: StreamItem(
                        title = "WuPlay Direct Fast Stream",
                        name = "WuPlay Fast Stream Engine",
                        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        quality = "1080p",
                        addonName = "WuPlay Core"
                    )

                val resumePos = getContinueWatchingPosition(meta.id)
                val subtitle = episode?.let { "S${it.season}:E${it.episode} • ${it.title}" } ?: meta.releaseInfo

                navigateToPlayer(
                    stream = bestStream,
                    title = meta.name,
                    subtitle = subtitle,
                    itemId = meta.id,
                    startPositionMs = resumePos,
                    meta = meta,
                    episode = episode
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error initiating direct play", e)
            } finally {
                _isDirectPlaying.value = false
            }
        }
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

                val streamTargetId = if (meta?.type == "series" && !meta.videos.isNullOrEmpty()) {
                    val firstVid = meta.videos.first()
                    firstVid.id
                } else {
                    id
                }

                // Pre-resolve streams in background so play is instant
                val streams = addonRepository.resolveStreams(streamTargetId, type)
                _currentStreams.value = streams
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading detail", e)
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    fun selectSeason(season: Int) {
        _selectedSeason.value = season
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

    // Auth operations
    fun signInWithGoogle(activity: Activity) {
        authManager.signInWithGoogle(activity, viewModelScope)
    }

    fun signOut() {
        authManager.signOut(viewModelScope)
    }

    fun clearAuthError() {
        authManager.clearError()
    }

    fun syncLibraryWithCloud() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isSyncingCloud.value = true
            try {
                libraryRepository.syncFromCloud(user.uid)
            } finally {
                _isSyncingCloud.value = false
            }
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

    // Watched status operations
    fun isMovieWatched(id: String): Boolean {
        return libraryRepository.isMovieWatched(id)
    }

    fun toggleMovieWatched(meta: MetaDetail) {
        libraryRepository.toggleMovieWatched(meta)
    }

    fun isEpisodeWatched(episodeId: String): Boolean {
        return libraryRepository.isEpisodeWatched(episodeId)
    }

    fun toggleEpisodeWatched(episode: VideoEpisode, meta: MetaDetail) {
        libraryRepository.toggleEpisodeWatched(episode.id, meta, episode.title)
    }

    fun markSeasonWatched(episodes: List<VideoEpisode>, isWatched: Boolean, meta: MetaDetail) {
        libraryRepository.markSeasonWatched(episodes.map { it.id }, isWatched, meta)
    }

    // Playback Settings Operations
    fun updatePlaybackSpeed(speed: Float) {
        playbackSettingsManager.updatePlaybackSpeed(speed)
    }

    fun setAutoPlayNextEpisode(enabled: Boolean) {
        playbackSettingsManager.setAutoPlayNextEpisode(enabled)
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        playbackSettingsManager.setAutoSkipIntro(enabled)
    }

    fun setHardwareAcceleration(enabled: Boolean) {
        playbackSettingsManager.setHardwareAcceleration(enabled)
    }

    fun setResizeMode(mode: Int) {
        playbackSettingsManager.setResizeMode(mode)
    }

    fun setDefaultSubtitleLang(lang: String) {
        playbackSettingsManager.setDefaultSubtitleLang(lang)
    }

    fun setSubtitleFontSize(sizeSp: Int) {
        playbackSettingsManager.setSubtitleFontSize(sizeSp)
    }

    fun setSubtitleStyle(style: String) {
        playbackSettingsManager.setSubtitleStyle(style)
    }

    fun setAudioPassthrough(enabled: Boolean) {
        playbackSettingsManager.setAudioPassthrough(enabled)
    }

    fun setBufferCacheSeconds(seconds: Int) {
        playbackSettingsManager.setBufferCacheSeconds(seconds)
    }

    fun resetPlaybackSettings() {
        playbackSettingsManager.resetToDefaults()
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
