package de.xikolo.models

import de.xikolo.utils.FileUtil
import java.io.File

// the file path should identify a download uniquely, thus this AssetDownload object can be used as an identifier for downloads
sealed class AssetDownload(val url: String?, open val fileName: String) {

    // must not end with separator
    protected open val fileFolder: String = FileUtil.createPublicAppFolderPath()

    open val title: String
        get() = fileName

    val filePath: String
        get() = fileFolder + File.separator + fileName

    sealed class Course(url: String?, override val fileName: String, val course: de.xikolo.models.Course) : AssetDownload(url, fileName) {

        override val fileFolder = super.fileFolder + File.separator + FileUtil.escapeFilename(course.title) + "_" + course.id

        sealed class Item(url: String?, fileName: String, val item: de.xikolo.models.Item, val video: Video) : Course(url, fileName, item.section.course) {

            override val fileFolder = super.fileFolder + File.separator + FileUtil.escapeFilename(item.section.title) + "_" + item.section.id

            override val fileName = FileUtil.escapeFilename(item.title) + "_" + fileName

            abstract val size: Int

            class Slides(item: de.xikolo.models.Item, video: Video) : Item(video.slidesUrl, "slides_${item.id}.pdf", item, video) {
                override val title = "Slides \"" + item.title + "\""
                override val size = video.slidesSize
            }

            class Transcript(item: de.xikolo.models.Item, video: Video) : Item(video.transcriptUrl, "transcript_${item.id}.pdf", item, video) {
                override val title = "Transcript \"" + item.title + "\""
                override val size = video.transcriptSize
            }

            class VideoSD(item: de.xikolo.models.Item, video: Video) : Item(video.singleStream.sdUrl, "video_sd_${item.id}.mp4", item, video) {
                override val title = "SD Video \"" + item.title + "\""
                override val size = video.singleStream.sdSize
            }

            class VideoHD(item: de.xikolo.models.Item, video: Video) : Item(video.singleStream.hdUrl, "video_hd_${item.id}.mp4", item, video) {
                override val title = "HD Video \"" + item.title + "\""
                override val size = video.singleStream.hdSize
            }

            class Audio(item: de.xikolo.models.Item, video: Video) : Item(video.audioUrl, "audio_${item.id}.mp3", item, video) {
                override val title = "Audio \"" + item.title + "\""
                override val size = video.transcriptSize
            }

        }

    }

}