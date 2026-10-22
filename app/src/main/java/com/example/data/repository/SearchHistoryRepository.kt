package com.example.data.repository

import com.example.data.db.RecentSearch
import com.example.data.db.RecentSearchDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting access to local search history data.
 */
class SearchHistoryRepository(private val recentSearchDao: RecentSearchDao) {

    val recentSearches: Flow<List<RecentSearch>> = recentSearchDao.getRecentSearches()

    suspend fun saveSearchQuery(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isNotBlank() && query.length >= 2) {
            recentSearchDao.insertSearch(
                RecentSearch(
                    query = query,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeSearchQuery(query: String) {
        recentSearchDao.deleteSearch(query.trim())
    }

    suspend fun clearSearchHistory() {
        recentSearchDao.clearAll()
    }
}
