package de.xikolo.models

import de.xikolo.App
import de.xikolo.R
import de.xikolo.download.filedownload.FileDownloadItem
import de.xikolo.utils.extensions.asEscapedFileName
import java.io.File

object DownloadAsset {

    class Document(
        val document: de.xikolo.models.Document,
        documentLocalization: DocumentLocalization
    ) : FileDownloadItem(
        documentLocalization.fileUrl,
        documentLocalization.language + "_" +
            documentLocalization.revision + "_" +
            documentLocalization.id + ".pdf"
    ) {
        override fun getFileFolder(): String {
            return super.getFileFolder() + File.separator +
                "Documents" + File.separator +
                document.title?.asEscapedFileName + "_" +
                document.id
        }

        override val title = "Document (" + documentLocalization.language + "): " +
            document.title
    }

    sealed class Certificate(url: String?, fileName: String, val course: de.xikolo.models.Course) :
        FileDownloadItem(url, fileName) {
        override fun getFileFolder(): String {
            return super.getFileFolder() + File.separator +
                "Certificates" + File.separator +
                course.title.asEscapedFileName + "_" +
                course.id
        }

        class ConfirmationOfParticipation(url: String?, course: de.xikolo.models.Course) :
            Certificate(url, "confirmation_of_participation.pdf", course) {
            override val title =
                App.instance.getString(R.string.course_confirmation_of_participation) + ": " +
                    course.title
        }

        class RecordOfAchievement(url: String?, course: de.xikolo.models.Course) :
            Certificate(url, "record_of_achievement.pdf", course) {
            override val title =
                App.instance.getString(R.string.course_record_of_achievement) + ": " +
                    course.title
        }

        class QualifiedCertificate(url: String?, course: de.xikolo.models.Course) :
            Certificate(url, "qualified_certificate.pdf", course) {
            override val title =
                App.instance.getString(R.string.course_qualified_certificate) + ": " +
                    course.title
        }
    }

    sealed class Course(
        url: String?,
        override val fileName: String,
        val course: de.xikolo.models.Course
    ) : FileDownloadItem(url, fileName) {
        override fun getFileFolder(): String {
            return super.getFileFolder() + File.separator +
                "Courses" + File.separator +
                course.title.asEscapedFileName + "_" +
                course.id
        }

        sealed class Item(url: String?, fileName: String, val item: de.xikolo.models.Item) :
            Course(url, fileName, item.section.course) {
            override fun getFileFolder(): String {
                return super.getFileFolder() + File.separator +
                    item.section.title.asEscapedFileName + "_" +
                    item.section.id
            }

            override val fileName = item.title.asEscapedFileName + "_" + fileName

            class Slides(item: de.xikolo.models.Item, video: Video) :
                Item(video.slidesUrl, "slides_${item.id}.pdf", item) {
                override val title = "Slides: " + item.title
                override val size = video.slidesSize.toLong()
            }

            class Transcript(item: de.xikolo.models.Item, video: Video) :
                Item(video.transcriptUrl, "transcript_${item.id}.pdf", item) {
                override val title = "Transcript: " + item.title
                override val size = video.transcriptSize.toLong()
            }

            class VideoSD(item: de.xikolo.models.Item, val video: Video) :
                Item(video.streamToPlay.sdUrl, "video_sd_${item.id}.mp4", item) {
                override val title = "Video (SD): " + item.title
                override val mimeType = "video/mp4"
                override val size = video.streamToPlay.sdSize.toLong()

                override val secondaryDownloadItems: Set<FileDownloadItem>
                    get() {
                        return video.subtitles.map {
                            Subtitles(it, item)
                        }.toSet()
                    }
                override val deleteSecondaryDownloadItemPredicate: (FileDownloadItem) -> Boolean =
                    { _ ->
                        !VideoSD(item, video).downloadExists && !VideoHD(item, video).downloadExists
                    }
            }

            class VideoHD(item: de.xikolo.models.Item, val video: Video) :
                Item(video.streamToPlay.hdUrl, "video_hd_${item.id}.mp4", item) {
                override val title = "Video (HD): " + item.title
                override val mimeType = "video/mp4"
                override val size = video.streamToPlay.hdSize.toLong()

                override val secondaryDownloadItems: Set<FileDownloadItem>
                    get() {
                        return video.subtitles.map {
                            Subtitles(it, item)
                        }.toSet()
                    }
                override val deleteSecondaryDownloadItemPredicate: (FileDownloadItem) -> Boolean =
                    { _ ->
                        !VideoSD(item, video).downloadExists && !VideoHD(item, video).downloadExists
                    }
            }

            class Audio(item: de.xikolo.models.Item, video: Video) :
                Item(video.audioUrl, "audio_${item.id}.mp3", item) {
                override val title = "Audio: " + item.title
                override val mimeType = "audio/mpeg"
                override val size = video.audioSize.toLong()
            }

            class Subtitles(videoSubtitles: VideoSubtitles, item: de.xikolo.models.Item) : Item(
                videoSubtitles.vttUrl,
                "subtitles_${videoSubtitles.language}_${item.id}.vtt",
                item
            ) {
                override fun getFileFolder(): String {
                    return super.getFileFolder() + File.separator + "Subtitles"
                }

                override val showNotification = false
                override val mimeType = "text/vtt"
            }
        }
    }
}
