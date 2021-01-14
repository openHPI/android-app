package de.xikolo.models

import com.squareup.moshi.Json
import de.xikolo.models.base.RealmAdapter
import de.xikolo.models.dao.ItemDao.Unmanaged.Companion.findForContent
import de.xikolo.utils.LanguageUtil
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Resource

open class Video : RealmObject() {

    @PrimaryKey
    var id: String? = null

    var summary: String? = null

    var duration: Int = 0

    var singleStream: VideoStream? = null

    var lecturerStream: VideoStream? = null

    var slidesStream: VideoStream? = null

    var slidesUrl: String? = null

    var slidesSize: Int = 0

    var audioUrl: String? = null

    var audioSize: Int = 0

    var transcriptUrl: String? = null

    var transcriptSize: Int = 0

    var thumbnailUrl: String? = null

    var subtitles = RealmList<VideoSubtitles>()

    // local field
    var progress = 0

    @JsonApi(type = "videos")
    class JsonModel : Resource(), RealmAdapter<Video> {

        var summary: String? = null
        var duration: Int = 0

        @field:Json(name = "single_stream")
        var singleStream: VideoStream? = null

        @field:Json(name = "lecturer_stream")
        var lecturerStream: VideoStream? = null

        @field:Json(name = "slides_stream")
        var slidesStream: VideoStream? = null

        @field:Json(name = "slides_url")
        var slidesUrl: String? = null

        @field:Json(name = "slides_size")
        var slidesSize: Int = 0

        @field:Json(name = "audio_url")
        var audioUrl: String? = null

        @field:Json(name = "audio_size")
        var audioSize: Int = 0

        @field:Json(name = "transcript_url")
        var transcriptUrl: String? = null

        @field:Json(name = "transcript_size")
        var transcriptSize: Int = 0

        @field:Json(name = "thumbnail_url")
        var thumbnailUrl: String? = null

        var subtitles: List<VideoSubtitles>? = null

        override fun convertToRealmObject(): Video {
            val video = Video()
            video.id = id
            video.summary = summary
            video.duration = duration
            video.singleStream = singleStream
            video.lecturerStream = lecturerStream
            video.slidesStream = slidesStream
            video.slidesUrl = slidesUrl
            video.slidesSize = slidesSize
            video.audioUrl = audioUrl
            video.audioSize = audioSize
            video.transcriptUrl = transcriptUrl
            video.transcriptSize = transcriptSize
            video.thumbnailUrl = thumbnailUrl
            video.subtitles.addAll(subtitles!!)
            return video
        }
    }

    val streamToPlay: VideoStream?
        get() {
            return if (singleStream != null &&
                (singleStream?.hlsUrl != null ||
                    singleStream?.hdUrl != null ||
                    singleStream?.sdUrl != null)
            ) {
                singleStream
            } else if (lecturerStream != null &&
                (lecturerStream?.hlsUrl != null ||
                    lecturerStream?.hdUrl != null ||
                    lecturerStream?.sdUrl != null)
            ) {
                lecturerStream
            } else slidesStream
        }

    val item: Item?
        get() {
            return findForContent(id)
        }

    val isSummaryAvailable: Boolean
        get() {
            return summary?.let {
                it.trim { it <= ' ' }
                    .isNotEmpty() && !it.trim { it <= ' ' }.contentEquals("Enter content")
            } ?: false
        }

    fun getSubtitlesAsString(): String {
        return subtitles.map {
            LanguageUtil.toLocaleName(it.language)
        }.joinToString(", ", "")
    }
}
