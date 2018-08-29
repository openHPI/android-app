package de.xikolo.views

import android.content.Context
import android.net.Uri
import android.support.annotation.IntRange
import android.util.AttributeSet
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util


open class ExoPlayerVideoView : PlayerView {

    private lateinit var playerContext: Context
    private lateinit var exoplayer: SimpleExoPlayer
    private lateinit var playerListener: Player.EventListener
    private lateinit var dataSourceFactory: DefaultDataSourceFactory

    private var videoMediaSource: MediaSource? = null
    private var mergedMediaSource: MediaSource? = null

    var onPreparedListener: OnPreparedListener? = null

    var onBufferUpdateListener: OnBufferUpdateListener? = null

    var onCompletionListener: OnCompletionListener? = null

    var onErrorListener: OnErrorListener? = null

    var isPreparing = false

    val duration: Long
        get() = player.duration

    val currentPosition: Long
        get() = player.currentPosition

    constructor(context: Context) : super(context) {
        setup(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup(context, attrs)
    }

    private fun setup(context: Context, attrs: AttributeSet?) {
        playerContext = context

        dataSourceFactory = DefaultDataSourceFactory(playerContext, Util.getUserAgent(playerContext, playerContext.packageName), DefaultBandwidthMeter())

        exoplayer = ExoPlayerFactory.newSimpleInstance(
            context,
            DefaultTrackSelector(
                AdaptiveTrackSelection.Factory(DefaultBandwidthMeter())
            )
        )

        playerListener = object : Player.EventListener {
            override fun onLoadingChanged(isLoading: Boolean) {
                onBufferUpdateListener?.onBufferingUpdate(player.bufferedPercentage)
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_READY && isPreparing) {
                    onPreparedListener?.onPrepared()
                    isPreparing = false
                }

                if (playbackState == Player.STATE_ENDED && currentPosition >= duration) {
                    onCompletionListener?.onCompletion()
                }
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                onErrorListener?.onError(error)
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            }

            override fun onSeekProcessed() {
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            }

            override fun onPositionDiscontinuity(reason: Int) {
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            }

            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            }
        }
        exoplayer.addListener(playerListener)

        player = exoplayer

        this.useController = false
    }

    fun start() {
        player.playWhenReady = true
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(
            speed,
            player.playbackParameters.pitch,
            player.playbackParameters.skipSilence
        )
    }

    fun setVideoURI(uri: Uri) {
        videoMediaSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
        mergedMediaSource = videoMediaSource
        prepare()
    }

    fun showSubtitles(uri: String, language: String) {
        val subtitleMediaSource = SingleSampleMediaSource.Factory(dataSourceFactory)
            .createMediaSource(
                Uri.parse(uri),
                Format.createTextSampleFormat(null, MimeTypes.TEXT_VTT, C.SELECTION_FLAG_FORCED, language),
                C.TIME_UNSET
            )

        mergedMediaSource = MergingMediaSource(videoMediaSource, subtitleMediaSource)
        prepare()
    }

    fun removeSubtitles() {
        mergedMediaSource = videoMediaSource
        prepare()
    }

    private fun prepare() {
        isPreparing = true
        exoplayer.prepare(mergedMediaSource)
    }

    fun release() {
        player.release()
    }

    fun isPlaying(): Boolean {
        return player.playWhenReady
    }

    interface OnPreparedListener {
        fun onPrepared()
    }

    interface OnBufferUpdateListener {
        fun onBufferingUpdate(@IntRange(from = 0L, to = 100L) percent: Int)
    }

    interface OnCompletionListener {
        fun onCompletion()
    }

    interface OnErrorListener {
        fun onError(e: Exception?): Boolean
    }
}
