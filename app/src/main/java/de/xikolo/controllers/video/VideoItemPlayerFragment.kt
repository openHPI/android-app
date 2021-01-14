package de.xikolo.controllers.video

import android.content.res.Configuration
import android.os.Bundle
import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.download.DownloadItem
import de.xikolo.download.DownloadStatus
import de.xikolo.models.DownloadAsset
import de.xikolo.models.Item
import de.xikolo.models.Video
import de.xikolo.models.VideoSubtitles
import de.xikolo.models.dao.ItemDao
import de.xikolo.models.dao.VideoDao
import de.xikolo.utils.LanalyticsUtil
import io.realm.Realm
import java.util.Locale
import kotlin.math.max

class VideoItemPlayerFragment : VideoStreamPlayerFragment() {

    companion object {
        val TAG: String = VideoItemPlayerFragment::class.java.simpleName

        private const val VIDEO_POSITION_REWIND_TIME = 10000

        private const val BUNDLING_KEY_COURSE_ID = "course_id"
        private const val BUNDLING_KEY_SECTION_ID = "section_id"
        private const val BUNDLING_KEY_ITEM_ID = "item_id"
        private const val BUNDLING_KEY_VIDEO_ID = "video_id"

        fun bundle(instance: VideoItemPlayerFragment, courseId: String, sectionId: String, itemId: String, videoId: String, autoPlay: Boolean = true) {
            bundle(instance, VideoDao.Unmanaged.find(videoId)!!.streamToPlay!!, autoPlay)

            val arguments = instance.arguments ?: Bundle()
            arguments.putAll(
                Bundle().apply {
                    putString(BUNDLING_KEY_COURSE_ID, courseId)
                    putString(BUNDLING_KEY_SECTION_ID, sectionId)
                    putString(BUNDLING_KEY_ITEM_ID, itemId)
                    putString(BUNDLING_KEY_VIDEO_ID, videoId)
                })

            instance.arguments = arguments
        }

        fun create(courseId: String, sectionId: String, itemId: String, videoId: String, autoPlay: Boolean = true): VideoItemPlayerFragment {
            return VideoItemPlayerFragment().apply {
                bundle(this, courseId, sectionId, itemId, videoId, autoPlay)
            }
        }

        fun unbundle(instance: VideoItemPlayerFragment, arguments: Bundle?) {
            arguments?.let {
                instance.courseId = arguments.getString(BUNDLING_KEY_COURSE_ID)!!
                instance.sectionId = arguments.getString(BUNDLING_KEY_SECTION_ID)!!
                instance.itemId = arguments.getString(BUNDLING_KEY_ITEM_ID)!!
                instance.videoId = arguments.getString(BUNDLING_KEY_VIDEO_ID)!!
            }
        }
    }

    lateinit var courseId: String

    lateinit var sectionId: String

    lateinit var itemId: String

    lateinit var videoId: String

    private val video: Video
        get() = VideoDao.Unmanaged.find(videoId)!!

    private val item: Item
        get() = ItemDao.Unmanaged.find(itemId)!!

    private val videoDownloadBest: DownloadAsset.Course.Item.VideoHLS
        get() = DownloadAsset.Course.Item.VideoHLS(
            item,
            video,
            VideoSettingsHelper.VideoQuality.BEST
        )

    private val videoDownloadHigh: DownloadAsset.Course.Item.VideoHLS
        get() = DownloadAsset.Course.Item.VideoHLS(
            item,
            video,
            VideoSettingsHelper.VideoQuality.HIGH
        )

    private val videoDownloadMedium: DownloadAsset.Course.Item.VideoHLS
        get() = DownloadAsset.Course.Item.VideoHLS(
            item,
            video,
            VideoSettingsHelper.VideoQuality.MEDIUM
        )

    private val videoDownloadLow: DownloadAsset.Course.Item.VideoHLS
        get() = DownloadAsset.Course.Item.VideoHLS(
            item,
            video,
            VideoSettingsHelper.VideoQuality.LOW
        )

    private val videoDao = VideoDao(Realm.getDefaultInstance())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        unbundle(this, arguments)

        initialVideoPosition = max(video.progress - VIDEO_POSITION_REWIND_TIME, 0)
    }

    override fun play(fromUser: Boolean) {
        if (fromUser) {
            LanalyticsUtil.trackVideoPlay(
                itemId,
                courseId, sectionId,
                currentPosition,
                currentPlaybackSpeed.value,
                requireActivity().resources.configuration.orientation,
                currentQualityString,
                sourceString
            )
        }
        super.play(fromUser)
    }

    override fun pause(fromUser: Boolean) {
        if (fromUser) {
            LanalyticsUtil.trackVideoPause(
                itemId,
                courseId, sectionId,
                currentPosition,
                currentPlaybackSpeed.value,
                requireActivity().resources.configuration.orientation,
                currentQualityString,
                sourceString
            )
        }
        super.pause(fromUser)
    }

    override fun seekTo(progress: Int, fromUser: Boolean) {
        if (fromUser) {
            LanalyticsUtil.trackVideoSeek(
                itemId,
                courseId, sectionId,
                currentPosition,
                progress,
                currentPlaybackSpeed.value,
                requireActivity().resources.configuration.orientation,
                currentQualityString,
                sourceString
            )
        }
        super.seekTo(progress, fromUser)
    }

    override fun changePlaybackMode(
        oldMode: VideoSettingsHelper.PlaybackMode,
        newMode: VideoSettingsHelper.PlaybackMode,
        fromUser: Boolean
    ) {
        if (fromUser) {
            val oldSourceString = sourceString
            super.changePlaybackMode(oldMode, newMode, true)
            LanalyticsUtil.trackVideoChangeQuality(
                itemId,
                courseId, sectionId,
                currentPosition,
                currentPlaybackSpeed.value,
                requireActivity().resources.configuration.orientation,
                getQualityString(oldMode),
                getQualityString(newMode),
                oldSourceString,
                sourceString
            )
        } else {
            super.changePlaybackMode(oldMode, newMode, false)
        }
    }

    override fun changePlaybackSpeed(oldSpeed: VideoSettingsHelper.PlaybackSpeed, newSpeed: VideoSettingsHelper.PlaybackSpeed, fromUser: Boolean) {
        super.changePlaybackSpeed(oldSpeed, newSpeed, fromUser)
        if (fromUser) {
            LanalyticsUtil.trackVideoChangeSpeed(
                itemId,
                courseId, sectionId,
                currentPosition,
                oldSpeed.value,
                newSpeed.value,
                requireActivity().resources.configuration.orientation,
                currentQualityString,
                sourceString
            )
        }
    }

    override fun saveCurrentPosition() {
        super.saveCurrentPosition()
        videoDao.updateProgress(video, currentPosition)
    }

    override fun getSubtitleList(): List<VideoSubtitles> {
        return video.subtitles ?: emptyList()
    }

    override fun setVideo(mode: VideoSettingsHelper.PlaybackMode): Boolean {
        val item = when (mode) {
            VideoSettingsHelper.PlaybackMode.BEST -> videoDownloadBest
            VideoSettingsHelper.PlaybackMode.HIGH -> videoDownloadHigh
            VideoSettingsHelper.PlaybackMode.MEDIUM -> videoDownloadMedium
            VideoSettingsHelper.PlaybackMode.LOW -> videoDownloadLow
            else -> null
        }

        if (item != null && videoDownloadPresent(item)) {
            playerView.setVideoSource(item.download!!)
            item.subs?.let {
                playerView.setSubtitleSources(it)
            }
            isOfflineVideo = true
            return true
        }

        return super.setVideo(mode)
    }

    override fun getPlaybackMode(): VideoSettingsHelper.PlaybackMode {
        return when {
            videoDownloadPresent(videoDownloadBest) ->
                VideoSettingsHelper.PlaybackMode.BEST
            videoDownloadPresent(videoDownloadHigh) ->
                VideoSettingsHelper.PlaybackMode.HIGH
            videoDownloadPresent(videoDownloadMedium) ->
                VideoSettingsHelper.PlaybackMode.MEDIUM
            videoDownloadPresent(videoDownloadLow) ->
                VideoSettingsHelper.PlaybackMode.LOW
            else -> super.getPlaybackMode()
        }
    }

    override fun getOfflineAvailability(mode: VideoSettingsHelper.PlaybackMode): Boolean {
        return when (mode) {
            VideoSettingsHelper.PlaybackMode.BEST -> videoDownloadPresent(videoDownloadBest)
            VideoSettingsHelper.PlaybackMode.HIGH -> videoDownloadPresent(videoDownloadHigh)
            VideoSettingsHelper.PlaybackMode.MEDIUM -> videoDownloadPresent(videoDownloadMedium)
            VideoSettingsHelper.PlaybackMode.LOW -> videoDownloadPresent(videoDownloadLow)
            else -> false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        LanalyticsUtil.trackVideoChangeOrientation(
            itemId,
            courseId, sectionId,
            currentPosition,
            currentPlaybackSpeed.value,
            newConfig.orientation,
            currentQualityString,
            sourceString
        )
    }

    private fun videoDownloadPresent(item: DownloadItem<*, *>): Boolean {
        return item.status.state == DownloadStatus.State.DOWNLOADED
    }

    private fun getQualityString(mode: VideoSettingsHelper.PlaybackMode): String {
        return mode.name.toLowerCase(Locale.ENGLISH)
    }
}
