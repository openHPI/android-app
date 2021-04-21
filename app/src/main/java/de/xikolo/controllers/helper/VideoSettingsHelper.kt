package de.xikolo.controllers.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import de.xikolo.R
import de.xikolo.config.Config
import de.xikolo.config.Feature
import de.xikolo.managers.PermissionManager
import de.xikolo.models.VideoSubtitles
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.LanguageUtil
import de.xikolo.views.CustomFontTextView

class VideoSettingsHelper(private val context: Context, private val subtitles: List<VideoSubtitles>?, private val changeListener: OnSettingsChangeListener, private val clickListener: OnSettingsClickListener, private val videoInfoCallback: VideoInfoCallback) {

    enum class VideoMode(val title: String) {
        SD("SD"), HD("HD"), AUTO("Auto")
    }

    enum class PlaybackSpeed(val value: Float) {
        X07(0.7f), X10(1.0f), X13(1.3f), X15(1.5f), X18(1.8f), X20(2.0f);

        override fun toString(): String {
            return "x$value"
        }

        companion object {
            fun get(str: String?): PlaybackSpeed {
                return when (str) {
                    "x0.7" -> X07
                    "x1.0" -> X10
                    "x1.3" -> X13
                    "x1.5" -> X15
                    "x1.8" -> X18
                    "x2.0" -> X20
                    else   -> X10
                }
            }
        }
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private val applicationPreferences: ApplicationPreferences = ApplicationPreferences()

    var currentQuality: VideoMode = VideoMode.HD
    var currentSpeed: PlaybackSpeed = PlaybackSpeed.X10
    var currentVideoSubtitles: VideoSubtitles? = null
    var isImmersiveModeEnabled: Boolean = false

    init {
        currentSpeed = applicationPreferences.videoPlaybackSpeed

        currentQuality = when {
            videoInfoCallback.isAvailable(VideoMode.HD)   -> VideoMode.HD
            videoInfoCallback.isAvailable(VideoMode.SD)   -> VideoMode.SD
            videoInfoCallback.isAvailable(VideoMode.AUTO) -> VideoMode.AUTO
            else                                          -> throw IllegalArgumentException("No video available")
        }

        subtitles?.let {
            for (videoSubtitles in it) {
                if (videoSubtitles.language == applicationPreferences.videoSubtitlesLanguage) {
                    currentVideoSubtitles = videoSubtitles
                }
            }
        }

        isImmersiveModeEnabled = applicationPreferences.isVideoShownImmersive
    }

    fun buildSettingsView(): ViewGroup {
        val list = buildSettingsPanel(null)

        list.addView(
            buildSettingsItem(
                R.string.icon_quality,
                context.getString(R.string.video_settings_quality) + "  " + context.getString(R.string.video_settings_separator) + "  " + currentQuality.title +
                    if (videoInfoCallback.isOfflineAvailable(currentQuality)) " " + context.getString(R.string.video_settings_quality_offline) else "",
                View.OnClickListener { clickListener.onQualityClick() },
                false,
                Config.FONT_MATERIAL
            )
        )
        list.addView(
            buildSettingsItem(
                R.string.icon_speed,
                context.getString(R.string.video_settings_speed) + "  " + context.getString(R.string.video_settings_separator) + "  " + currentSpeed.toString(),
                View.OnClickListener { clickListener.onPlaybackSpeedClick() },
                false,
                Config.FONT_MATERIAL
            )
        )
        if (subtitles?.isNotEmpty() == true) {
            list.addView(
                buildSettingsItem(
                    R.string.icon_subtitles,
                    context.getString(R.string.video_settings_subtitles) + (currentVideoSubtitles?.let {
                        "  " + context.getString(R.string.video_settings_separator) + "  " + LanguageUtil.toNativeName(
                            it.language
                        )
                    } ?: ""),
                    View.OnClickListener { clickListener.onSubtitleClick() },
                    false,
                    Config.FONT_MATERIAL
                )
            )
        }
        if (videoInfoCallback.isImmersiveModeAvailable()) {
            list.addView(
                buildImmersiveSettingsItem(list)
            )
        }
        if (Feature.PIP && PermissionManager.hasPipPermission(context)) {
            list.addView(
                buildSettingsItem(
                    R.string.icon_pip,
                    context.getString(R.string.video_settings_pip),
                    View.OnClickListener { clickListener.onPipClick() },
                    false,
                    Config.FONT_MATERIAL
                )
            )
        }

        return list.parent as ViewGroup
    }

    fun buildQualityView(): ViewGroup {
        val list = buildSettingsPanel(context.getString(R.string.video_settings_quality))

        if (videoInfoCallback.isAvailable(VideoMode.AUTO)) {
            list.addView(
                buildSettingsItem(
                    null,
                    VideoMode.AUTO.title +
                        if (videoInfoCallback.isOfflineAvailable(VideoMode.AUTO)) " " + context.getString(R.string.video_settings_quality_offline) else "",
                    View.OnClickListener {
                        val oldQuality = currentQuality
                        currentQuality = VideoMode.AUTO
                        changeListener.onQualityChanged(oldQuality, currentQuality)
                    },
                    currentQuality == VideoMode.AUTO
                )
            )
        }
        if (videoInfoCallback.isAvailable(VideoMode.HD)) {
            list.addView(
                buildSettingsItem(
                    null,
                    VideoMode.HD.title +
                        if (videoInfoCallback.isOfflineAvailable(VideoMode.HD)) " " + context.getString(R.string.video_settings_quality_offline) else "",
                    View.OnClickListener {
                        val oldQuality = currentQuality
                        currentQuality = VideoMode.HD
                        changeListener.onQualityChanged(oldQuality, currentQuality)
                    },
                    currentQuality == VideoMode.HD
                )
            )
        }
        if (videoInfoCallback.isAvailable(VideoMode.SD)) {
            list.addView(
                buildSettingsItem(
                    null,
                    VideoMode.SD.title +
                        if (videoInfoCallback.isOfflineAvailable(VideoMode.SD)) " " + context.getString(R.string.video_settings_quality_offline) else "",
                    View.OnClickListener {
                        val oldQuality = currentQuality
                        currentQuality = VideoMode.SD
                        changeListener.onQualityChanged(oldQuality, currentQuality)
                    },
                    currentQuality == VideoMode.SD
                )
            )
        }

        return list.parent as ViewGroup
    }

    fun buildPlaybackSpeedView(): ViewGroup {
        val list = buildSettingsPanel(context.getString(R.string.video_settings_speed))

        for (speed in PlaybackSpeed.values()) {
            list.addView(
                buildSettingsItem(
                    null,
                    speed.toString(),
                    View.OnClickListener {
                        val oldSpeed = currentSpeed
                        currentSpeed = speed
                        changeListener.onPlaybackSpeedChanged(oldSpeed, currentSpeed)
                    },
                    currentSpeed == speed
                )
            )
        }

        return list.parent as ViewGroup
    }

    fun buildSubtitleView(): ViewGroup {
        val list = buildSettingsPanel(
            context.getString(R.string.video_settings_subtitles),
            context.getString(R.string.icon_settings),
            View.OnClickListener {
                ContextCompat.startActivity(context, Intent(Settings.ACTION_CAPTIONING_SETTINGS), null)
            }
        )

        list.addView(
            buildSettingsItem(
                null,
                context.getString(R.string.video_settings_subtitles_none),
                View.OnClickListener {
                    val oldVideoSubtitles = currentVideoSubtitles
                    currentVideoSubtitles = null
                    applicationPreferences.videoSubtitlesLanguage = null
                    changeListener.onSubtitleChanged(oldVideoSubtitles, currentVideoSubtitles)
                },
                currentVideoSubtitles == null
            )
        )
        for (videoSubtitles in subtitles!!) {
            list.addView(
                buildSettingsItem(
                    null,
                    LanguageUtil.toNativeName(videoSubtitles.language) +
                        if (videoSubtitles.createdByMachine) " " + context.getString(R.string.video_settings_subtitles_generated) else "",
                    View.OnClickListener {
                        val oldVideoSubtitles = currentVideoSubtitles
                        currentVideoSubtitles = videoSubtitles
                        applicationPreferences.videoSubtitlesLanguage = videoSubtitles.language
                        changeListener.onSubtitleChanged(oldVideoSubtitles, currentVideoSubtitles)
                    },
                    currentVideoSubtitles == videoSubtitles
                )
            )
        }

        return list.parent as ViewGroup
    }

    @SuppressLint("InflateParams")
    private fun buildSettingsPanel(title: String?): ViewGroup {
        val list = inflater
            .inflate(R.layout.container_video_settings, null)
            .findViewById(R.id.content_settings) as LinearLayout

        val titleView = list.findViewById(R.id.content_settings_title) as TextView
        if (title != null) {
            titleView.text = title
        } else {
            titleView.visibility = View.GONE
        }

        return list
    }

    private fun buildSettingsPanel(title: String?, icon: String, iconClickListener: View.OnClickListener): ViewGroup {
        val panel = buildSettingsPanel(title)
        val iconView = panel.findViewById(R.id.content_settings_icon) as TextView
        iconView.text = icon
        iconView.setOnClickListener(iconClickListener)
        iconView.visibility = View.VISIBLE
        return panel
    }

    @SuppressLint("InflateParams")
    private fun buildSettingsItem(@StringRes icon: Int?, title: String, clickListener: View.OnClickListener, active: Boolean, font: String = Config.FONT_XIKOLO): ViewGroup {
        val item = inflater.inflate(R.layout.item_video_settings, null) as LinearLayout

        val iconView = item.findViewById(R.id.item_settings_icon) as CustomFontTextView
        val titleView = item.findViewById(R.id.item_settings_title) as TextView

        if (icon != null) {
            iconView.setCustomFont(context, font)
            iconView.setText(icon)
        } else {
            iconView.setText(R.string.icon_settings)
            iconView.visibility = View.INVISIBLE
        }
        titleView.text = title

        if (active) {
            val activeColor = ContextCompat.getColor(context, R.color.apptheme_secondary)
            iconView.setTextColor(activeColor)
            titleView.setTextColor(activeColor)
        }

        item.setOnClickListener(clickListener)

        return item
    }

    private fun buildImmersiveSettingsItem(parent: ViewGroup): View {
        return buildSettingsItem(
            if (isImmersiveModeEnabled) R.string.icon_show_fitting else R.string.icon_show_immersive,
            if (isImmersiveModeEnabled) context.getString(R.string.video_settings_show_fitting) else context.getString(R.string.video_settings_show_immersive),
            View.OnClickListener {
                isImmersiveModeEnabled = !isImmersiveModeEnabled

                applicationPreferences.isVideoShownImmersive = isImmersiveModeEnabled
                changeListener.onImmersiveModeChanged(!isImmersiveModeEnabled, isImmersiveModeEnabled)

                val index = parent.indexOfChild(it)
                parent.removeViewAt(index)
                parent.addView(buildImmersiveSettingsItem(parent), index)
            },
            false,
            Config.FONT_MATERIAL
        )
    }


    interface OnSettingsClickListener {

        fun onQualityClick()

        fun onPlaybackSpeedClick()

        fun onSubtitleClick()

        fun onPipClick()
    }

    // also invoked when old value equal to new value
    interface OnSettingsChangeListener {

        fun onQualityChanged(old: VideoMode, new: VideoMode)

        fun onPlaybackSpeedChanged(old: PlaybackSpeed, new: PlaybackSpeed)

        // subtitle is null if 'None' is selected
        fun onSubtitleChanged(old: VideoSubtitles?, new: VideoSubtitles?)

        fun onImmersiveModeChanged(old: Boolean, new: Boolean)
    }

    interface VideoInfoCallback {

        fun isAvailable(videoMode: VideoMode): Boolean

        fun isOfflineAvailable(videoMode: VideoMode): Boolean

        fun isImmersiveModeAvailable(): Boolean
    }
}
