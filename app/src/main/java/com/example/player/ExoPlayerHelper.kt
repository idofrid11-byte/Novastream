package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.data.settings.PlaybackSettings

/**
 * Helper to build and configure ExoPlayer instances with optimal streaming
 * load control, HLS/MP4 source resolution, and user hardware acceleration settings.
 */
@OptIn(UnstableApi::class)
object ExoPlayerHelper {

    private const val USER_AGENT = "Stremio/5.0.0-beta (Linux; Android; Mobile)"

    fun createExoPlayer(
        context: Context,
        settings: PlaybackSettings? = null
    ): ExoPlayer {
        val bufferSecs = settings?.bufferCacheSeconds ?: 30
        val minBufferMs = (bufferSecs * 1000).coerceAtLeast(15000)
        val maxBufferMs = (minBufferMs * 2).coerceAtLeast(30000)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context.applicationContext).apply {
            val extensionMode = if (settings?.hardwareAcceleration != false) {
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            } else {
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            }
            setExtensionRendererMode(extensionMode)
            setEnableDecoderFallback(true)
        }

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context.applicationContext)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .setSeekForwardIncrementMs(10000L)
            .setSeekBackIncrementMs(10000L)
            .build().apply {
                if (settings != null) {
                    playbackParameters = PlaybackParameters(settings.playbackSpeed)
                }
            }
    }

    fun buildMediaSource(context: Context, url: String): MediaSource {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        val dataSourceFactory = DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory)
        val uri = Uri.parse(url)

        val isHls = url.contains(".m3u8", ignoreCase = true) ||
                url.contains("/hls", ignoreCase = true)

        return if (isHls) {
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            val mediaItem = MediaItem.fromUri(uri)
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }
    }
}
