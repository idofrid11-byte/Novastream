package com.example

import com.example.data.api.BuiltinAddons
import com.example.data.api.StremioApiClient
import com.example.data.model.AddonCatalog
import com.example.data.model.AddonManifest
import com.example.data.model.LibraryItem
import com.example.data.model.WatchStatus
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testStremioUrlNormalization() {
        val client = StremioApiClient()
        val normalized1 = client.normalizeUrl("stremio://v3-cinemeta.strem.io/manifest.json")
        assertEquals("https://v3-cinemeta.strem.io/manifest.json", normalized1)

        val normalized2 = client.normalizeUrl("v3-cinemeta.strem.io")
        assertEquals("https://v3-cinemeta.strem.io", normalized2)
    }

    @Test
    fun testAddonManifestResourceSupport() {
        val manifest = AddonManifest(
            id = "test.addon",
            name = "Test Addon",
            version = "1.0.0",
            types = listOf("movie", "series"),
            resources = listOf("catalog", "meta", "stream"),
            catalogs = listOf(AddonCatalog(type = "movie", id = "top", name = "Top Movies")),
            idPrefixes = listOf("tt", "blender_")
        )

        assertTrue(manifest.supportsResource("catalog"))
        assertTrue(manifest.supportsResource("meta"))
        assertTrue(manifest.supportsResource("stream"))
        assertFalse(manifest.supportsResource("subtitles"))

        assertTrue(manifest.matchesIdPrefix("tt1234567"))
        assertTrue(manifest.matchesIdPrefix("blender_sintel"))
        assertFalse(manifest.matchesIdPrefix("other_item"))
    }

    @Test
    fun testBuiltinAddonsAndStreams() {
        val streams = BuiltinAddons.getStreams("blender_big_buck_bunny")
        assertTrue(streams.isNotEmpty())
        assertNotNull(streams.first().url)

        val meta = BuiltinAddons.getInternalMeta("blender_big_buck_bunny")
        assertNotNull(meta)
        assertEquals("Big Buck Bunny", meta!!.name)
    }

    @Test
    fun testLibraryItemProgress() {
        val item = LibraryItem(
            id = "test_1",
            type = "movie",
            name = "Test Movie",
            poster = null,
            background = null,
            releaseInfo = "2024",
            imdbRating = "8.0",
            genres = listOf("Sci-Fi"),
            status = WatchStatus.WATCHING,
            watchPositionMs = 5000L,
            durationMs = 10000L
        )

        assertEquals(0.5f, item.progressFraction, 0.01f)
    }

    @Test
    fun testSubtitleProvider() {
        val availableLangs = com.example.data.subtitles.SubtitleProvider.AVAILABLE_LANGUAGES
        assertTrue(availableLangs.any { it.first == "eng" })
        assertTrue(availableLangs.any { it.first == "spa" })

        val cues = com.example.data.subtitles.SubtitleProvider.getCues("blender_big_buck_bunny", "eng")
        assertTrue(cues.isNotEmpty())
        assertTrue(cues.first().text.isNotBlank())
        assertTrue(cues.first().startMs < cues.first().endMs)
    }
}

