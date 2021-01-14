package de.xikolo.controllers.video

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import com.github.rubensousa.previewseekbar.PreviewBar
import com.github.rubensousa.previewseekbar.PreviewSeekBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.xikolo.R
import de.xikolo.controllers.base.BaseFragment
import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.models.VideoStream
import de.xikolo.models.VideoSubtitles
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.extensions.ConnectivityType
import de.xikolo.utils.extensions.connectivityType
import de.xikolo.utils.extensions.isOnline
import de.xikolo.views.CustomFontTextView
import de.xikolo.views.CustomSizeVideoView
import de.xikolo.views.ExoPlayerVideoView
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class VideoStreamPlayerFragment : BaseFragment() {

    companion object {
        val TAG: String = VideoStreamPlayerFragment::class.java.simpleName

        private const val CONTROLS_FADE_DEFAULT_TIMEOUT = 3000
        private const val MESSAGE_CONTROLS_FADE_OUT = 1

        private val SEEKBAR_PREVIEW_HANDLER_TAG = "$TAG/seekBarPreview"
        private const val SEEKBAR_PREVIEW_INTERVAL = 100
        private const val SEEKBAR_PREVIEW_POSITION_DIFFERENCE = 5000

        private const val SEEKBAR_UPDATER_INTERVAL = 100L

        private const val VIDEO_STEPPING_DURATION = 10000

        private const val BUNDLING_KEY_STREAM = "stream"
        private const val BUNDLING_KEY_AUTOPLAY = "autoplay"

        fun bundle(
            instance: VideoStreamPlayerFragment,
            stream: VideoStream,
            autoPlay: Boolean = true
        ) {
            val arguments = instance.arguments ?: Bundle()
            arguments.putAll(
                Bundle().apply {
                    putParcelable(BUNDLING_KEY_STREAM, stream)
                    putBoolean(BUNDLING_KEY_AUTOPLAY, autoPlay)
                })
            instance.arguments = arguments
        }

        fun create(stream: VideoStream, autoPlay: Boolean = true): VideoStreamPlayerFragment {
            return VideoStreamPlayerFragment().apply {
                bundle(this, stream, autoPlay)
            }
        }

        fun unbundle(instance: VideoStreamPlayerFragment, arguments: Bundle?) {
            arguments?.let {
                instance.videoStream = it.getParcelable(BUNDLING_KEY_STREAM)!!
                instance.autoPlay = it.getBoolean(BUNDLING_KEY_AUTOPLAY)
            }
        }
    }

    lateinit var videoStream: VideoStream

    var autoPlay: Boolean = true

    @BindView(R.id.playerView)
    lateinit var playerView: CustomSizeVideoView

    @BindView(R.id.shadowContainer)
    lateinit var shadowContainer: View

    @BindView(R.id.progressBar)
    lateinit var progressBar: View

    @BindView(R.id.interfaceContainer)
    lateinit var interfaceContainer: View

    @BindView(R.id.controlsContainer)
    lateinit var controlsContainer: View

    @BindView(R.id.settingsButton)
    lateinit var settingsButton: TextView

    @BindView(R.id.fullscreenButton)
    lateinit var fullscreenButton: TextView

    @BindView(R.id.seekBar)
    lateinit var seekBar: PreviewSeekBar

    @BindView(R.id.seekBarPreviewLayout)
    lateinit var seekBarPreviewLayout: FrameLayout

    @BindView(R.id.seekBarPreviewImage)
    lateinit var seekBarPreviewImage: ImageView

    @BindView(R.id.playButton)
    lateinit var playButton: CustomFontTextView

    @BindView(R.id.stepForwardButton)
    lateinit var stepForwardButton: CustomFontTextView

    @BindView(R.id.stepBackwardButton)
    lateinit var stepBackwardButton: CustomFontTextView

    @BindView(R.id.retryButton)
    lateinit var retryButton: TextView

    @BindView(R.id.playbackTimeText)
    lateinit var playbackTimeText: TextView

    @BindView(R.id.totalTimeText)
    lateinit var totalTimeText: TextView

    @BindView(R.id.warningContainer)
    lateinit var warningContainer: View

    @BindView(R.id.warningText)
    lateinit var warningText: TextView

    @BindView(R.id.settingsContainer)
    lateinit var settingsContainer: ViewGroup

    private var bottomSheetBehavior: BottomSheetBehavior<ViewGroup>? = null
    private lateinit var videoSettingsHelper: VideoSettingsHelper
    private var settingsOpen = false

    private var userIsSeeking = false
    private var seekBarUpdater: Runnable? = null
    private var seekBarUpdaterIsRunning = false
    private val seekBarPreviewThread: HandlerThread
    private val seekBarPreviewHandler: Handler

    protected var initialVideoPosition: Int = 0
    private var initialPlaybackState: Boolean = autoPlay
    private var isInitialPreparing = true

    private lateinit var controlsVisibilityHandler: ControlsVisibilityHandler

    protected var isOfflineVideo = false

    private val applicationPreferences = ApplicationPreferences()

    val currentPlaybackSpeed: VideoSettingsHelper.PlaybackSpeed
        get() = videoSettingsHelper.currentSpeed

    val currentPosition: Int
        get() = playerView.currentPosition.toInt()

    val duration: Int
        get() = playerView.duration.toInt()

    private val hasEnded: Boolean
        get() = currentPosition >= duration

    val hasAlmostEnded: Boolean
        get() = currentPosition >= duration - 10000

    val isPlaying: Boolean
        get() = playerView.isPlaying()

    val sourceString: String
        get() = if (isOfflineVideo) "offline" else "online"

    val currentQualityString: String
        get() = videoSettingsHelper.currentMode.name.toLowerCase(Locale.ENGLISH)

    var isShowingControls: Boolean = false
        private set

    val controllerInterface: ControllerInterface?
        get() = activity as? ControllerInterface?

    init {
        seekBarPreviewThread = HandlerThread(SEEKBAR_PREVIEW_HANDLER_TAG)
        seekBarPreviewThread.start()
        seekBarPreviewHandler = Handler(seekBarPreviewThread.looper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        unbundle(this, arguments)

        controlsVisibilityHandler = ControlsVisibilityHandler(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleEnd(detector: ScaleGestureDetector?) {
                    detector?.let {
                        val immersive = it.scaleFactor > 1
                        videoSettingsHelper.isImmersiveModeEnabled = immersive
                        changeImmersiveMode(
                            videoSettingsHelper.isImmersiveModeEnabled,
                            immersive,
                            true
                        )
                    }
                }
            }
        )

        this.view?.setOnTouchListener { _, event ->
            if (!settingsOpen && controllerInterface?.isImmersiveModeAvailable() == true) {
                gestureDetector.onTouchEvent(event)
            }
            false
        }

        this.view?.setOnClickListener {
            if (settingsOpen) {
                hideSettings()
            }
            if (playerView.visibility == View.VISIBLE) {
                showControls()
            }
        }

        playerView.onPreparedListener = object : ExoPlayerVideoView.OnPreparedListener {
            override fun onPrepared() {
                hideProgress()

                playerView.visibility = View.VISIBLE
                warningContainer.visibility = View.GONE
                seekBar.max = duration

                seekTo(initialVideoPosition, false)

                totalTimeText.text = getTimeString(duration)
                playbackTimeText.text = getTimeString(currentPosition)

                stepForwardButton.visibility = View.VISIBLE
                stepBackwardButton.visibility = View.VISIBLE

                seekBar.isPreviewEnabled = playerView.previewAvailable

                if (initialPlaybackState) {
                    play(false)
                }
            }
        }

        playerView.onBufferUpdateListener = object : ExoPlayerVideoView.OnBufferUpdateListener {
            override fun onBufferingUpdate(percent: Int) {
                seekBar.secondaryProgress = (seekBar.max * (percent / 100.0)).toInt()
            }

            override fun onBufferingStart() {
                showProgress()
            }

            override fun onBufferingEnd() {
                hideProgress()
            }
        }

        playerView.onCompletionListener = object : ExoPlayerVideoView.OnCompletionListener {
            override fun onCompletion() {
                pause(false)
                playButton.text = getString(R.string.icon_reload)
                stepForwardButton.visibility = View.GONE
                stepBackwardButton.visibility = View.GONE
                showControls()
            }
        }

        playerView.onErrorListener = object : ExoPlayerVideoView.OnErrorListener {
            override fun onError(e: Exception?): Boolean {
                showError()
                return true
            }
        }

        seekBarUpdater = object : Runnable {
            override fun run() {
                activity?.runOnUiThread {
                    seekBarUpdaterIsRunning = true

                    if (!userIsSeeking && isPlaying) {
                        seekBar.progress = currentPosition
                        playbackTimeText.text = getTimeString(currentPosition)
                    }

                    if (!hasEnded && isPlaying) {
                        Handler(Looper.getMainLooper()).postDelayed(
                            Thread(this),
                            SEEKBAR_UPDATER_INTERVAL
                        )
                    } else {
                        seekBarUpdaterIsRunning = false
                    }
                }
            }
        }

        playButton.setOnClickListener {
            showControls()
            if (isPlaying) {
                pause(true)
            } else {
                if (hasEnded) {
                    // 'replay' button was pressed
                    stepForwardButton.visibility = View.VISIBLE
                    stepBackwardButton.visibility = View.VISIBLE
                    seekTo(0, false)
                }

                play(true)
            }
        }

        settingsButton.setOnClickListener {
            showSettings(videoSettingsHelper.buildSettingsView())
        }

        fullscreenButton.setOnClickListener {
            updateFullscreenToggle(
                controllerInterface?.onToggleFullscreen() ?: true
            )
        }

        stepForwardButton.setOnClickListener {
            showControls()
            stepForward()
        }

        stepBackwardButton.setOnClickListener {
            showControls()
            stepBackward()
        }

        seekBar.attachPreviewView(seekBarPreviewLayout)
        seekBar.setPreviewLoader(object : com.github.rubensousa.previewseekbar.PreviewLoader {
            private var lastPreview: Long = 0
            private var lastPosition: Long = -1

            override fun loadPreview(currentPosition: Long, max: Long) {
                if (System.currentTimeMillis() - lastPreview > SEEKBAR_PREVIEW_INTERVAL &&
                    (lastPosition < 0 || abs(
                        currentPosition - lastPosition
                    ) > SEEKBAR_PREVIEW_POSITION_DIFFERENCE)
                ) {
                    seekBarPreviewHandler.removeCallbacksAndMessages(null)
                    seekBarPreviewHandler.postAtFrontOfQueue {
                        val frame = playerView.getFrameAt(currentPosition)
                        activity?.runOnUiThread { seekBarPreviewImage.setImageBitmap(frame) }

                        lastPreview = System.currentTimeMillis()
                        lastPosition = currentPosition
                    }
                }
            }
        })
        seekBar.addOnScrubListener(object : PreviewBar.OnScrubListener {
            override fun onScrubStart(previewBar: PreviewBar?) {
                userIsSeeking = true
            }

            override fun onScrubStop(previewBar: PreviewBar?) {
                seekBarPreviewHandler.removeCallbacksAndMessages(null)

                userIsSeeking = false
                seekTo(previewBar?.progress ?: 0, true)
            }

            override fun onScrubMove(previewBar: PreviewBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    showControls()
                    playbackTimeText.text = getTimeString(progress)
                }
            }
        })

        retryButton.setOnClickListener {
            showProgress()
            if (updateVideo()) {
                seekTo(0, true)
                playerView.start()
                prepare()
            } else {
                showError()
            }
        }

        controllerInterface?.onCreateSettings()?.let {
            settingsContainer = it
        }
        bottomSheetBehavior = BottomSheetBehavior.from(settingsContainer)
        bottomSheetBehavior?.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        settingsOpen = false
                        controllerInterface?.onSettingsClosed()
                        showControls()
                    }
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        settingsOpen = true
                        controllerInterface?.onSettingsOpened()
                        showControls(Integer.MAX_VALUE)
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    controllerInterface?.onSettingsSliding(slideOffset)
                }
            }
        )

        showProgress()
        setupVideo()
        if (updateVideo()) {
            if (autoPlay) {
                playerView.start()
            }
            prepare()
        } else {
            showError()
        }
    }

    private fun setupVideo() {
        this.videoSettingsHelper = VideoSettingsHelper(
            requireActivity(),
            getSubtitleList(),
            object : VideoSettingsHelper.OnSettingsChangeListener {
                override fun onSubtitleChanged(old: VideoSubtitles?, new: VideoSubtitles?) {
                    hideSettings()
                    if (old != new) {
                        changeSubtitles(old, new, true)
                    }
                }

                override fun onPlaybackModeChanged(
                    old: VideoSettingsHelper.PlaybackMode,
                    new: VideoSettingsHelper.PlaybackMode
                ) {
                    hideSettings()
                    if (old != new) {
                        changePlaybackMode(old, new, true)
                    }
                }

                override fun onPlaybackSpeedChanged(
                    old: VideoSettingsHelper.PlaybackSpeed,
                    new: VideoSettingsHelper.PlaybackSpeed
                ) {
                    hideSettings()
                    if (old != new) {
                        changePlaybackSpeed(old, new, true)
                    }
                }

                override fun onImmersiveModeChanged(old: Boolean, new: Boolean) {
                    hideSettings()
                    if (old != new) {
                        changeImmersiveMode(old, new, true)
                    }
                }
            },
            object : VideoSettingsHelper.OnSettingsClickListener {
                override fun onSubtitleClick() {
                    showSettings(videoSettingsHelper.buildSubtitleView())
                }

                override fun onPlaybackSpeedClick() {
                    showSettings(videoSettingsHelper.buildPlaybackSpeedView())
                }

                override fun onQualityClick() {
                    showSettings(videoSettingsHelper.buildQualityView())
                }

                override fun onPipClick() {
                    hideSettings()
                    controllerInterface?.onPipClicked()
                }
            },
            object : VideoSettingsHelper.VideoInfoCallback {
                override fun isAvailable(mode: VideoSettingsHelper.PlaybackMode): Boolean {
                    return getVideoAvailability(mode)
                }

                override fun isOfflineAvailable(mode: VideoSettingsHelper.PlaybackMode): Boolean {
                    return getOfflineAvailability(mode)
                }

                override fun isImmersiveModeAvailable(): Boolean {
                    return controllerInterface?.isImmersiveModeAvailable() ?: false
                }
            }
        )
        videoSettingsHelper.currentMode = getPlaybackMode()

        controllerInterface?.onImmersiveModeChanged(videoSettingsHelper.isImmersiveModeEnabled)
    }

    protected open fun getVideoAvailability(mode: VideoSettingsHelper.PlaybackMode): Boolean {
        return when (mode) {
            VideoSettingsHelper.PlaybackMode.AUTO,
            VideoSettingsHelper.PlaybackMode.LOW,
            VideoSettingsHelper.PlaybackMode.MEDIUM,
            VideoSettingsHelper.PlaybackMode.HIGH,
            VideoSettingsHelper.PlaybackMode.BEST -> videoStream.hlsUrl != null
            VideoSettingsHelper.PlaybackMode.LEGACY_HD ->
                videoStream.hlsUrl == null && videoStream.hdUrl != null
            VideoSettingsHelper.PlaybackMode.LEGACY_SD ->
                videoStream.hlsUrl == null && videoStream.sdUrl != null
        }
    }

    protected open fun getSubtitleList(): List<VideoSubtitles> {
        return emptyList()
    }

    protected open fun getOfflineAvailability(mode: VideoSettingsHelper.PlaybackMode): Boolean {
        return false
    }

    protected open fun getPlaybackMode(): VideoSettingsHelper.PlaybackMode {
        return if (getVideoAvailability(VideoSettingsHelper.PlaybackMode.AUTO)) {
            VideoSettingsHelper.PlaybackMode.AUTO
        } else if (context.connectivityType == ConnectivityType.WIFI ||
            !applicationPreferences.isVideoQualityLimitedOnMobile
        ) {
            when {
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.BEST) ->
                    VideoSettingsHelper.PlaybackMode.BEST
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.HIGH) ->
                    VideoSettingsHelper.PlaybackMode.HIGH
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.MEDIUM) ->
                    VideoSettingsHelper.PlaybackMode.MEDIUM
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.LOW) ->
                    VideoSettingsHelper.PlaybackMode.LOW
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.LEGACY_HD) ->
                    VideoSettingsHelper.PlaybackMode.LEGACY_HD
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.LEGACY_SD) ->
                    VideoSettingsHelper.PlaybackMode.LEGACY_SD
                else -> throw IllegalArgumentException("No supported playback mode")
            }
        } else {
            when {
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.MEDIUM) ->
                    VideoSettingsHelper.PlaybackMode.MEDIUM
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.LOW) ->
                    VideoSettingsHelper.PlaybackMode.LOW
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.HIGH) ->
                    VideoSettingsHelper.PlaybackMode.HIGH
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.BEST) ->
                    VideoSettingsHelper.PlaybackMode.BEST
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.LEGACY_SD) ->
                    VideoSettingsHelper.PlaybackMode.LEGACY_SD
                getVideoAvailability(VideoSettingsHelper.PlaybackMode.LEGACY_HD) ->
                    VideoSettingsHelper.PlaybackMode.LEGACY_HD
                else -> throw IllegalArgumentException("No supported playback mode")
            }
        }
    }

    protected open fun setVideo(mode: VideoSettingsHelper.PlaybackMode): Boolean {
        return when {
            context.isOnline -> { // device has internet connection
                when (mode) {
                    VideoSettingsHelper.PlaybackMode.AUTO -> {
                        playerView.setHLSVideoUri(Uri.parse(videoStream.hlsUrl))
                        playerView.setDesiredBitrate(null)
                    }
                    VideoSettingsHelper.PlaybackMode.LOW -> {
                        playerView.setHLSVideoUri(Uri.parse(videoStream.hlsUrl))
                        playerView.setDesiredBitrate(VideoSettingsHelper.VideoQuality.LOW.bitrate)
                    }
                    VideoSettingsHelper.PlaybackMode.MEDIUM -> {
                        playerView.setHLSVideoUri(Uri.parse(videoStream.hlsUrl))
                        playerView.setDesiredBitrate(
                            VideoSettingsHelper.VideoQuality.MEDIUM.bitrate
                        )
                    }
                    VideoSettingsHelper.PlaybackMode.HIGH -> {
                        playerView.setHLSVideoUri(Uri.parse(videoStream.hlsUrl))
                        playerView.setDesiredBitrate(VideoSettingsHelper.VideoQuality.HIGH.bitrate)
                    }
                    VideoSettingsHelper.PlaybackMode.BEST -> {
                        playerView.setHLSVideoUri(Uri.parse(videoStream.hlsUrl))
                        playerView.setDesiredBitrate(VideoSettingsHelper.VideoQuality.BEST.bitrate)
                    }
                    VideoSettingsHelper.PlaybackMode.LEGACY_HD -> {
                        playerView.setProgressiveVideoUri(Uri.parse(videoStream.hdUrl))
                    }
                    VideoSettingsHelper.PlaybackMode.LEGACY_SD -> {
                        playerView.setProgressiveVideoUri(Uri.parse(videoStream.sdUrl))
                    }
                }
                playerView.setSubtitleUris(
                    getSubtitleList().associate {
                        it.language to Uri.parse(it.vttUrl)
                    }
                )
                isOfflineVideo = false
                true
            }
            else -> {
                warningContainer.visibility = View.VISIBLE
                warningText.text = getString(R.string.video_notification_no_offline_video)
                false
            }
        }
    }

    protected open fun changeSubtitles(
        oldSubtitles: VideoSubtitles?,
        newSubtitles: VideoSubtitles?,
        fromUser: Boolean
    ) {
        showProgress()
        updateSubtitles()
        prepare()
    }

    protected open fun changePlaybackMode(
        oldMode: VideoSettingsHelper.PlaybackMode,
        newMode: VideoSettingsHelper.PlaybackMode,
        fromUser: Boolean
    ) {
        showProgress()
        if (updateVideo()) {
            prepare()
        } else {
            showError()
        }
    }

    protected open fun changePlaybackSpeed(
        oldSpeed: VideoSettingsHelper.PlaybackSpeed, newSpeed: VideoSettingsHelper.PlaybackSpeed,
        fromUser: Boolean
    ) {
        updatePlaybackSpeed()
    }

    protected open fun changeImmersiveMode(oldMode: Boolean, newMode: Boolean, fromUser: Boolean) {
        controllerInterface?.onImmersiveModeChanged(newMode)
    }

    private fun showError() {
        saveCurrentPosition()
        warningContainer.visibility = View.VISIBLE
        warningText.text = getString(R.string.error_plain)
    }

    private fun startSeekBarUpdater() {
        if (!seekBarUpdaterIsRunning) {
            Thread(seekBarUpdater).start()
        }
    }

    open fun play(fromUser: Boolean) {
        playButton.text = getString(R.string.icon_pause)
        startSeekBarUpdater()
        playerView.start()
    }

    open fun pause(fromUser: Boolean) {
        playButton.text = getString(R.string.icon_play)
        saveCurrentPosition()
        playerView.pause()
    }

    protected open fun seekTo(progress: Int, fromUser: Boolean) {
        playerView.seekTo(progress.toLong())
        playbackTimeText.text = getTimeString(progress)
        seekBar.progress = progress
        startSeekBarUpdater()
    }

    private fun stepForward() {
        seekTo(
            min(
                currentPosition + VIDEO_STEPPING_DURATION,
                duration
            ),
            true
        )
    }

    private fun stepBackward() {
        seekTo(
            max(
                currentPosition - VIDEO_STEPPING_DURATION,
                0
            ),
            true
        )
    }

    fun showSettings(view: View) {
        settingsContainer.removeAllViews()
        settingsContainer.addView(view)
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hideSettings() {
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun showProgress() {
        playButton.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE
    }

    fun hideProgress() {
        playButton.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
    }

    fun updateFullscreenToggle(isPortrait: Boolean) {
        if (isPortrait) {
            fullscreenButton.text = getString(R.string.icon_fullscreen)
        } else {
            fullscreenButton.text = getString(R.string.icon_fullscreen_exit)
        }
    }

    fun showControls(timeout: Int = CONTROLS_FADE_DEFAULT_TIMEOUT) {
        isShowingControls = true
        controlsContainer.visibility = View.VISIBLE
        shadowContainer.visibility = View.VISIBLE
        controllerInterface?.onControlsShown()

        if (timeout != 0) {
            controlsVisibilityHandler.removeMessages(MESSAGE_CONTROLS_FADE_OUT)
            controlsVisibilityHandler.sendMessageDelayed(
                controlsVisibilityHandler.obtainMessage(MESSAGE_CONTROLS_FADE_OUT),
                timeout.toLong()
            )
        }
    }

    fun hideControls() {
        isShowingControls = false
        controlsContainer.visibility = View.GONE
        shadowContainer.visibility = View.GONE
        controllerInterface?.onControlsHidden()
    }

    fun setControlsPadding(left: Int?, top: Int?, right: Int?, bottom: Int?) {
        if (view != null) {
            interfaceContainer.setPadding(
                left ?: 0,
                top ?: 0,
                right ?: 0,
                bottom ?: 0
            )
        }
    }

    override fun onPause() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            pause(false)
        }
        super.onPause()
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            pause(false)
        }
        super.onStop()
    }

    override fun onDestroyView() {
        playerView.release()
        super.onDestroyView()
    }

    override fun onDestroy() {
        seekBarPreviewThread.quit()
        super.onDestroy()
    }

    fun handleBackPress(): Boolean {
        if (settingsOpen) {
            hideSettings()
            return false
        }
        return true
    }

    private fun updatePlaybackSpeed() {
        playerView.setPlaybackSpeed(currentPlaybackSpeed.value)
    }

    private fun updateSubtitles() {
        val currentSubtitles = videoSettingsHelper.currentVideoSubtitles
        if (currentSubtitles != null) {
            playerView.showSubtitles(currentSubtitles.language)
        } else {
            playerView.removeSubtitles()
        }
    }

    private fun updateVideo(): Boolean {
        warningContainer.visibility = View.GONE

        if (setVideo(videoSettingsHelper.currentMode)) {
            updateSubtitles()
            updatePlaybackSpeed()
            if (isOfflineVideo) {
                // ToDo
                /*playerView.uri?.let {
                    playerView.setPreviewUri(it)
                }*/
            } else if (context.isOnline) {
                if (videoStream.sdUrl != null) {
                    playerView.setPreviewUri(Uri.parse(videoStream.sdUrl))
                } else if (videoStream.hdUrl != null) {
                    playerView.setPreviewUri(Uri.parse(videoStream.hdUrl))
                }
            }
            return true
        }
        return false
    }

    private fun prepare() {
        initialPlaybackState = isPlaying
        if (!isInitialPreparing) {
            initialVideoPosition = currentPosition
        }
        playerView.pause()
        playerView.prepare()
        isInitialPreparing = false
    }

    protected open fun saveCurrentPosition() {}

    private fun getTimeString(millis: Int): String {
        return String.format(
            Locale.US,
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(
                    millis.toLong()
                )
            )
        )
    }

    interface ControllerInterface {

        fun onControlsShown()

        fun onControlsHidden()

        fun onCreateSettings(): ViewGroup? {
            return null
        }

        fun onSettingsSliding(offset: Float)

        fun onSettingsOpened()

        fun onSettingsClosed()

        fun onPipClicked()

        fun onImmersiveModeChanged(enabled: Boolean)

        fun isImmersiveModeAvailable(): Boolean {
            return false
        }

        fun onToggleFullscreen(): Boolean
    }

    protected class ControlsVisibilityHandler(private val controller: VideoStreamPlayerFragment) :
        Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            when (message.what) {
                MESSAGE_CONTROLS_FADE_OUT ->
                    if (controller.isPlaying) {
                        controller.hideControls()
                    }
            }
        }
    }
}
