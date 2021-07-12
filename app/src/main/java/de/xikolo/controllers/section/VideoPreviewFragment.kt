package de.xikolo.controllers.section

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import butterknife.BindView
import com.yatatsu.autobundle.AutoBundleField
import de.xikolo.R
import de.xikolo.config.GlideApp
import de.xikolo.controllers.base.ViewModelFragment
import de.xikolo.controllers.dialogs.VideoDownloadQualityHintDialog
import de.xikolo.controllers.helper.DownloadViewHelper
import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.controllers.settings.SettingsActivity
import de.xikolo.controllers.video.VideoItemPlayerActivityAutoBundle
import de.xikolo.download.DownloadStatus
import de.xikolo.extensions.observe
import de.xikolo.models.DownloadAsset
import de.xikolo.models.Item
import de.xikolo.models.Video
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.LanalyticsUtil
import de.xikolo.utils.LanguageUtil
import de.xikolo.utils.extensions.cast
import de.xikolo.utils.extensions.isCastConnected
import de.xikolo.utils.extensions.setMarkdownText
import de.xikolo.utils.extensions.videoThumbnailSize
import de.xikolo.viewmodels.section.VideoPreviewViewModel
import de.xikolo.views.CustomSizeImageView
import java.util.concurrent.TimeUnit

const val MAX_DESCRIPTION_LINES = 4

class VideoPreviewFragment : ViewModelFragment<VideoPreviewViewModel>() {

    companion object {
        val TAG: String = VideoPreviewFragment::class.java.simpleName
    }

    @AutoBundleField
    lateinit var courseId: String

    @AutoBundleField
    lateinit var sectionId: String

    @AutoBundleField
    lateinit var itemId: String

    @BindView(R.id.textTitle)
    lateinit var textTitle: TextView

    @BindView(R.id.textSubtitles)
    lateinit var textSubtitles: TextView

    @BindView(R.id.textPreviewDescription)
    lateinit var textDescription: TextView

    @BindView(R.id.showDescriptionButton)
    lateinit var showDescriptionButton: Button

    @BindView(R.id.videoDescriptionContainer)
    lateinit var videoDescriptionContainer: FrameLayout

    @BindView(R.id.durationText)
    lateinit var textDuration: TextView

    @BindView(R.id.videoThumbnail)
    lateinit var imageVideoThumbnail: CustomSizeImageView

    @BindView(R.id.containerDownloads)
    lateinit var linearLayoutDownloads: LinearLayout

    @BindView(R.id.refresh_layout)
    lateinit var viewContainer: View

    @BindView(R.id.playButton)
    lateinit var viewPlay: View

    @BindView(R.id.videoMetadata)
    lateinit var videoMetadata: ViewGroup

    private var video: Video? = null

    override val layoutResource = R.layout.fragment_video_preview

    override fun createViewModel(): VideoPreviewViewModel {
        return VideoPreviewViewModel(itemId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.viewContainer.visibility = View.GONE

        activity?.let {
            val thumbnailSize: Point = it.videoThumbnailSize
            imageVideoThumbnail.setDimensions(thumbnailSize.x, thumbnailSize.y)

            if (it.resources?.configuration?.orientation != Configuration.ORIENTATION_PORTRAIT) {
                val paramsMeta = videoMetadata.layoutParams
                paramsMeta.width = thumbnailSize.x
                videoMetadata.layoutParams = paramsMeta
            }
        }

        viewModel.item
            .observe(viewLifecycleOwner) { item ->
                this.video = viewModel.video
                video?.let { video ->
                    updateView(item, video)
                    showContent()
                }
            }
    }

    private fun updateView(item: Item, video: Video) {
        viewContainer.visibility = View.VISIBLE

        GlideApp.with(this)
            .load(video.thumbnailUrl)
            .override(imageVideoThumbnail.forcedWidth, imageVideoThumbnail.forcedHeight)
            .into(imageVideoThumbnail)

        textTitle.text = item.title

        if (video.isSummaryAvailable) {
            textDescription.setMarkdownText(video.summary?.trim())
            displayCollapsedDescription()

            showDescriptionButton.setOnClickListener {
                if (textDescription.maxLines == MAX_DESCRIPTION_LINES) {
                    displayFullDescription()
                } else {
                    displayCollapsedDescription()
                }
            }
        } else {
            hideDescription()
        }

        displayAvailableSubtitles()

        val minutes = TimeUnit.SECONDS.toMinutes(video.duration.toLong())
        val seconds = video.duration -
            TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(video.duration.toLong()))
        textDuration.text = getString(R.string.duration, minutes, seconds)

        viewPlay.setOnClickListener { play() }

        activity?.let {
            updateDownloadViewHelpers(it, item, video)
        }
    }

    private fun updateDownloadViewHelpers(activity: FragmentActivity, item: Item, video: Video) {
        linearLayoutDownloads.removeAllViews()

        if (video.streamToPlay?.hlsUrl != null) {
            val videoLow = DownloadAsset.Course.Item.VideoHLS(
                item,
                video,
                VideoSettingsHelper.VideoQuality.LOW
            ) to activity.getString(R.string.settings_video_download_quality_low)
            val videoMedium = DownloadAsset.Course.Item.VideoHLS(
                item,
                video,
                VideoSettingsHelper.VideoQuality.MEDIUM
            ) to activity.getString(R.string.settings_video_download_quality_medium)
            val videoHigh = DownloadAsset.Course.Item.VideoHLS(
                item,
                video,
                VideoSettingsHelper.VideoQuality.HIGH
            ) to activity.getString(R.string.settings_video_download_quality_high)
            val videoBest = DownloadAsset.Course.Item.VideoHLS(
                item,
                video,
                VideoSettingsHelper.VideoQuality.BEST
            ) to activity.getString(R.string.settings_video_download_quality_best)

            val videoDefault = when (ApplicationPreferences().videoDownloadQuality) {
                VideoSettingsHelper.VideoQuality.LOW -> videoLow
                VideoSettingsHelper.VideoQuality.MEDIUM -> videoMedium
                VideoSettingsHelper.VideoQuality.HIGH -> videoHigh
                VideoSettingsHelper.VideoQuality.BEST -> videoBest
            }

            fun showDownloadQualityHint() {
                val prefs = ApplicationPreferences()
                if (!prefs.videoDownloadQualityHintShown) {
                    val dialog = VideoDownloadQualityHintDialog()
                    dialog.listener = object : VideoDownloadQualityHintDialog.Listener {
                        override fun onOpenSettingsClicked() {
                            startActivity(
                                Intent(context, SettingsActivity::class.java)
                            )
                            prefs.videoDownloadQualityHintShown = true
                        }

                        override fun onDismissed() {
                            prefs.videoDownloadQualityHintShown = true
                        }
                    }
                    dialog.show(childFragmentManager, VideoDownloadQualityHintDialog.TAG)
                }
            }

            linearLayoutDownloads.addView(
                DownloadViewHelper(
                    activity,
                    videoDefault.first,
                    getString(R.string.video_with_quality).format(videoDefault.second),
                    openText = getString(R.string.play),
                    openClick = { play() },
                    downloadClick = { showDownloadQualityHint() }
                ).view
            )

            (setOf(videoLow, videoMedium, videoHigh, videoBest) - videoDefault).forEach {
                if (it.first.status.state != DownloadStatus.State.DELETED) {
                    linearLayoutDownloads.addView(
                        DownloadViewHelper(
                            activity,
                            it.first,
                            getString(R.string.video_with_quality).format(it.second),
                            openText = getString(R.string.play),
                            openClick = { play() },
                            downloadClick = { showDownloadQualityHint() },
                            onDeleted = { updateDownloadViewHelpers(activity, item, video) }
                        ).view
                    )
                }
            }
        }

        if (video.slidesUrl != null) {
            val dvh = DownloadViewHelper(
                activity,
                DownloadAsset.Course.Item.Slides(item, video),
                activity.getText(R.string.slides_as_pdf)
            )
            linearLayoutDownloads.addView(dvh.view)
        }

        if (video.transcriptUrl != null) {
            val dvh = DownloadViewHelper(
                activity,
                DownloadAsset.Course.Item.Transcript(item, video),
                activity.getText(R.string.transcript_as_pdf)
            )
            linearLayoutDownloads.addView(dvh.view)
        }
    }

    private fun play() {
        video?.let { video ->
            if (activity?.isCastConnected == true) {
                LanalyticsUtil.trackVideoPlay(
                    itemId,
                    courseId,
                    sectionId,
                    video.progress,
                    1.0f,
                    Configuration.ORIENTATION_LANDSCAPE,
                    "hd",
                    "cast"
                )

                activity?.let {
                    video.cast(it, true)
                }
            } else {
                activity?.let {
                    val intent = video.id?.let { videoId ->
                        VideoItemPlayerActivityAutoBundle
                            .builder(courseId, sectionId, itemId, videoId)
                            .parentIntent(it.intent)
                            .build(it)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun displayAvailableSubtitles() {
        video?.subtitles?.let { subtitles ->
            if (subtitles.isNotEmpty()) {
                textSubtitles.text =
                    subtitles.map {
                        LanguageUtil.toLocaleName(it.language)
                    }.joinToString(", ", getString(R.string.video_settings_subtitles) + ": ")
                textSubtitles.visibility = View.VISIBLE
            } else {
                textSubtitles.visibility = View.GONE
            }
        }
    }

    private fun displayCollapsedDescription() {
        videoDescriptionContainer.visibility = View.VISIBLE
        videoDescriptionContainer.foreground = null
        showDescriptionButton.visibility = View.GONE
        textDescription.ellipsize = TextUtils.TruncateAt.END
        textDescription.maxLines = MAX_DESCRIPTION_LINES
        showDescriptionButton.text = getString(R.string.show_more)

        textDescription.post {
            try {
                if (textDescription.lineCount > MAX_DESCRIPTION_LINES) {
                    showDescriptionButton.visibility = View.VISIBLE
                    videoDescriptionContainer.foreground = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.gradient_light_to_transparent_from_bottom
                    )
                }
            } catch (e: IllegalStateException) {
                // Fragment not attached to context anymore
            }
        }
    }

    private fun displayFullDescription() {
        textDescription.ellipsize = null
        textDescription.maxLines = Integer.MAX_VALUE
        videoDescriptionContainer.foreground = null
        showDescriptionButton.text = getString(R.string.show_less)
    }

    private fun hideDescription() {
        videoDescriptionContainer.visibility = View.GONE
        showDescriptionButton.visibility = View.GONE
    }
}
