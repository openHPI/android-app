package de.xikolo.views

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import androidx.annotation.IntRange
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener

open class ExoPlayerVideoView : PlayerView {

    private lateinit var playerContext: Context
    private lateinit var exoplayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DefaultDataSourceFactory
    private lateinit var bandwidthMeter: DefaultBandwidthMeter
    private lateinit var trackSelector: DefaultTrackSelector

    private var videoMediaSource: MediaSource? = null
    private var subtitleMediaSources: Map<String, MediaSource>? = null
    private var mergedMediaSource: MediaSource? = null
    private var mediaMetadataRetriever: MediaMetadataRetriever? = null

    var onPreparedListener: OnPreparedListener? = null

    var onBufferUpdateListener: OnBufferUpdateListener? = null

    var onCompletionListener: OnCompletionListener? = null

    var onErrorListener: OnErrorListener? = null

    private var isPreparing = false
    private var isBuffering = false

    private var previewPrepareThread = Thread()

    var aspectRatio: Float = 16.0f / 9.0f

    var previewAvailable = false
        private set

    val duration: Long
        get() = exoplayer.duration

    val currentPosition: Long
        get() = exoplayer.currentPosition

    constructor(context: Context) : super(context) {
        setup(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setup(context)
    }

    private fun setup(context: Context) {
        playerContext = context

        bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        dataSourceFactory = DefaultDataSourceFactory(
            playerContext,
            Util.getUserAgent(playerContext, playerContext.packageName),
            bandwidthMeter
        )
        trackSelector = DefaultTrackSelector(
            context,
            AdaptiveTrackSelection.Factory()
        )

        exoplayer = SimpleExoPlayer.Builder(
            context
        )
            .setTrackSelector(trackSelector)
            .build()

        exoplayer.addListener(
            object : Player.EventListener {
                override fun onLoadingChanged(isLoading: Boolean) {
                    onBufferUpdateListener?.onBufferingUpdate(exoplayer.bufferedPercentage)
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    if (playbackState == Player.STATE_READY && isPreparing) {
                        previewPrepareThread.join()

                        onPreparedListener?.onPrepared()
                        isPreparing = false
                    }

                    if (playbackState == Player.STATE_ENDED && currentPosition >= duration) {
                        onCompletionListener?.onCompletion()
                    }

                    if (playbackState == Player.STATE_BUFFERING) {
                        isBuffering = true
                        onBufferUpdateListener?.onBufferingStart()
                    } else if (isBuffering) {
                        isBuffering = false
                        onBufferUpdateListener?.onBufferingEnd()
                    }
                }

                override fun onPlayerError(error: ExoPlaybackException) {
                    onErrorListener?.onError(error)
                }

                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                }

                override fun onSeekProcessed() {
                }

                override fun onTracksChanged(
                    trackGroups: TrackGroupArray,
                    trackSelections: TrackSelectionArray
                ) {
                }

                override fun onPositionDiscontinuity(reason: Int) {
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                }
            }
        )

        exoplayer.addVideoListener(
            object : VideoListener {
                override fun onVideoSizeChanged(
                    width: Int,
                    height: Int,
                    unappliedRotationDegrees: Int,
                    pixelWidthHeightRatio: Float
                ) {
                    aspectRatio = (width * pixelWidthHeightRatio) / height
                }
            }
        )

        player = exoplayer
    }

    fun start() {
        exoplayer.playWhenReady = true
    }

    fun pause() {
        exoplayer.playWhenReady = false
    }

    fun seekTo(position: Long) {
        exoplayer.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoplayer.setPlaybackParameters(
            PlaybackParameters(
                speed,
                exoplayer.playbackParameters.pitch
            )
        )
    }

    fun setProgressiveVideoUri(uri: Uri) {
        setVideoSource(
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(
                    MediaItem.fromUri(uri)
                )
        )
    }

    fun setHLSVideoUri(uri: Uri) {
        setVideoSource(
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(
                    MediaItem.fromUri(uri)
                )
        )
    }

    fun setSubtitleUris(uris: Map<String, Uri>) {
        setSubtitleSources(
            uris.entries.associate {
                it.key to SingleSampleMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(
                        MediaItem.Subtitle(
                            it.value,
                            MimeTypes.TEXT_VTT,
                            it.key,
                            C.SELECTION_FLAG_DEFAULT
                        ),
                        C.TIME_UNSET
                    )
            }
        )
    }

    fun setVideoSource(source: MediaSource) {
        videoMediaSource = source
        mergedMediaSource = videoMediaSource
    }

    fun setSubtitleSources(sources: Map<String, MediaSource>) {
        subtitleMediaSources = sources
    }

    fun setPreviewUri(uri: Uri) {
        previewPrepareThread = Thread {
            mediaMetadataRetriever = MediaMetadataRetriever()
            previewAvailable =
                try {
                    mediaMetadataRetriever?.setDataSource(playerContext, uri)
                    true
                } catch (e1: Exception) {
                    try {
                        mediaMetadataRetriever?.setDataSource(
                            uri.toString(),
                            HashMap<String, String>()
                        )
                        true
                    } catch (e2: Exception) {
                        try {
                            mediaMetadataRetriever?.setDataSource(uri.toString())
                            true
                        } catch (e3: Exception) {
                            false
                        }
                    }
                }
        }
        previewPrepareThread.start()
    }

    fun showSubtitles(language: String) {
        mergedMediaSource = subtitleMediaSources?.get(language)?.let { subtitles ->
            videoMediaSource?.let { video ->
                MergingMediaSource(video, subtitles)
            }
        } ?: videoMediaSource
    }

    fun removeSubtitles() {
        mergedMediaSource = videoMediaSource
    }

    fun getFrameAt(position: Long): Bitmap? {
        if (previewAvailable) {
            return mediaMetadataRetriever?.getFrameAtTime(position * 1000)
        }
        return null
    }

    fun setDesiredBitrate(bitrate: Int?) {
        trackSelector.setParameters(
            DefaultTrackSelector.ParametersBuilder(context)
                .apply {
                    if (bitrate != null) {
                        setMaxVideoBitrate(bitrate)
                        setMinVideoBitrate(bitrate)
                    }
                }
        )
    }

    fun scaleToFill() {
        exoplayer.videoScalingMode = Renderer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
    }

    fun scaleToFit() {
        exoplayer.videoScalingMode = Renderer.VIDEO_SCALING_MODE_DEFAULT
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    fun prepare() {
        isPreparing = true
        mergedMediaSource?.let {
            exoplayer.setMediaSource(it)
            exoplayer.prepare()
        }
    }

    fun release() {
        exoplayer.stop()
        exoplayer.release()
    }

    fun isPlaying(): Boolean {
        return exoplayer.playWhenReady
    }

    interface OnPreparedListener {
        fun onPrepared()
    }

    interface OnBufferUpdateListener {
        fun onBufferingUpdate(@IntRange(from = 0L, to = 100L) percent: Int)

        fun onBufferingStart()

        fun onBufferingEnd()
    }

    interface OnCompletionListener {
        fun onCompletion()
    }

    interface OnErrorListener {
        fun onError(e: Exception?): Boolean
    }
}
