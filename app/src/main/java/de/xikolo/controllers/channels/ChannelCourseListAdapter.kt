package de.xikolo.controllers.channels

import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import de.xikolo.App
import de.xikolo.R
import de.xikolo.config.GlideApp
import de.xikolo.controllers.base.BaseCourseListAdapter
import de.xikolo.controllers.video.VideoStreamPlayerActivityAutoBundle
import de.xikolo.models.Course
import de.xikolo.models.VideoStream
import de.xikolo.utils.extensions.isBetween
import de.xikolo.utils.extensions.setMarkdownText
import de.xikolo.utils.extensions.videoThumbnailSize
import de.xikolo.views.CustomSizeImageView
import java.util.Date

class ChannelCourseListAdapter(fragment: Fragment, onCourseButtonClickListener: OnCourseButtonClickListener) : BaseCourseListAdapter<Triple<String?, VideoStream?, String?>>(fragment, onCourseButtonClickListener) {

    companion object {
        val TAG: String = ChannelCourseListAdapter::class.java.simpleName

        const val CHANNEL_COLOR_UNSET = -1
    }

    private var channelColor = CHANNEL_COLOR_UNSET

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_META   -> DescriptionViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.content_channel_description, parent, false)
            )
            ITEM_VIEW_TYPE_HEADER -> createHeaderViewHolder(parent, viewType)
            else                  -> createCourseViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder      -> bindHeaderViewHolder(holder, position)
            is DescriptionViewHolder -> {
                val meta = super.contentList.get(position) as Triple<*, *, *>?
                val description = meta?.first as String?
                val stageStream = meta?.second as VideoStream?
                val imageUrl = meta?.third as String?

                if (description != null) {
                    holder.text.setMarkdownText(description)
                } else {
                    holder.text.visibility = View.GONE
                }

                if (stageStream?.hlsUrl != null ||
                    stageStream?.hdUrl != null ||
                    stageStream?.sdUrl != null
                ) {
                    holder.videoPreview.visibility = View.VISIBLE

                    if (imageUrl != null) {
                        GlideApp.with(fragment)
                            .load(imageUrl)
                            .override(
                                holder.imageVideoThumbnail.forcedWidth,
                                holder.imageVideoThumbnail.forcedHeight
                            )
                            .into(holder.imageVideoThumbnail)
                    }

                    fragment.activity?.let {
                        val thumbnailSize: Point = it.videoThumbnailSize
                        holder.imageVideoThumbnail.setDimensions(thumbnailSize.x, thumbnailSize.y)
                    }

                    holder.playButton.setOnClickListener {
                        fragment.activity?.let { activity ->
                            activity.startActivity(
                                VideoStreamPlayerActivityAutoBundle.builder(stageStream)
                                    .parentIntent(activity.intent)
                                    .overrideActualParent(true)
                                    .build(activity)
                            )
                        }
                    }
                } else {
                    holder.videoPreview.visibility = View.GONE
                }

                holder.durationText.visibility = View.GONE
            }
            is CourseViewHolder      -> {
                val course = super.contentList.get(position) as Course

                holder.textDescription.text = course.shortAbstract
                holder.textDescription.visibility = View.VISIBLE

                if (Date().isBetween(course.startDate, course.endDate)) {
                    holder.textBanner.visibility = View.VISIBLE
                    holder.textBanner.text = App.instance.getText(R.string.banner_running)
                    holder.textBanner.setBackgroundColor(ContextCompat.getColor(App.instance, R.color.banner_green))
                } else {
                    holder.textBanner.visibility = View.GONE
                }

                if (channelColor != CHANNEL_COLOR_UNSET) {
                    holder.buttonCourseAction.setTextColor(channelColor)
                    holder.buttonCourseDetails.setTextColor(channelColor)
                }

                bindCourseViewHolder(holder, position)
            }
        }
    }

    fun setThemeColor(@ColorInt color: Int) {
        this.channelColor = color
    }

    class DescriptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        @BindView(R.id.text)
        lateinit var text: TextView

        @BindView(R.id.videoPreview)
        lateinit var videoPreview: ViewGroup

        @BindView(R.id.playButton)
        lateinit var playButton: View

        @BindView(R.id.videoThumbnail)
        lateinit var imageVideoThumbnail: CustomSizeImageView

        @BindView(R.id.durationText)
        lateinit var durationText: TextView

        init {
            ButterKnife.bind(this, view)
        }
    }

}
