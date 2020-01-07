package de.xikolo.models

import de.xikolo.App
import de.xikolo.R
import de.xikolo.managers.DownloadManager
import de.xikolo.utils.extensions.asEscapedFileName
import de.xikolo.utils.extensions.preferredStorage
import java.io.File

open class DownloadAsset(val url: String?, open val fileName: String, var storage: Storage = App.instance.preferredStorage) {

    // must not end with separator and always have a getter function, otherwise dynamic storage changes will not work
    protected open val fileFolder: String
        get() = storage.file.absolutePath

    open val title: String
        get() = fileName

    open val size: Long = 0L

    val sizeWithSecondaryAssets: Long
        get() {
            var secondarySize = 0L
            secondaryAssets.forEach {
                secondarySize += it.sizeWithSecondaryAssets
            }
            return size + secondarySize
        }

    val filePath: String
        get() = fileFolder + File.separator + fileName

    open val mimeType = "application/pdf"

    open val showNotification = true

    open val secondaryAssets = mutableSetOf<DownloadAsset>()

    open val deleteSecondaryAssets: (DownloadAsset, DownloadManager) -> Boolean = { _, _ -> true }

    class Document(
        val document: de.xikolo.models.Document,
        documentLocalization: DocumentLocalization
    ) : DownloadAsset(
        documentLocalization.fileUrl,
        documentLocalization.language + "_" + documentLocalization.revision + "_" + documentLocalization.id + ".pdf"
    ) {
        override val fileFolder
            get() = super.fileFolder + File.separator + "Documents" + File.separator + document.title?.asEscapedFileName + "_" + document.id

        override val title = "Document (" + documentLocalization.language + "): " + document.title
    }

    sealed class Certificate(url: String?, fileName: String, val course: de.xikolo.models.Course) : DownloadAsset(url, fileName) {
        override val fileFolder
            get() = super.fileFolder + File.separator + "Certificates" + File.separator + course.title.asEscapedFileName + "_" + course.id

        class ConfirmationOfParticipation(url: String?, course: de.xikolo.models.Course) : Certificate(url, "confirmation_of_participation.pdf", course) {
            override val title = App.instance.getString(R.string.course_confirmation_of_participation) + ": " + course.title
        }

        class RecordOfAchievement(url: String?, course: de.xikolo.models.Course) : Certificate(url, "record_of_achievement.pdf", course) {
            override val title = App.instance.getString(R.string.course_record_of_achievement) + ": " + course.title
        }

        class QualifiedCertificate(url: String?, course: de.xikolo.models.Course) : Certificate(url, "qualified_certificate.pdf", course) {
            override val title = App.instance.getString(R.string.course_qualified_certificate) + ": " + course.title
        }
    }

    sealed class Course(url: String?, override val fileName: String, val course: de.xikolo.models.Course) : DownloadAsset(url, fileName) {

        override val fileFolder
            get() = super.fileFolder + File.separator + "Courses" + File.separator + course.title.asEscapedFileName + "_" + course.id

        sealed class Item(url: String?, fileName: String, val item: de.xikolo.models.Item) : Course(url, fileName, item.section.course) {

            override val fileFolder
                get() = super.fileFolder + File.separator + item.section.title.asEscapedFileName + "_" + item.section.id

            override val fileName = item.title.asEscapedFileName + "_" + fileName

            class Slides(item: de.xikolo.models.Item, video: Video) : Item(video.slidesUrl, "slides_${item.id}.pdf", item) {
                override val title = "Slides: " + item.title
                override val size = video.slidesSize.toLong()
            }

            class Transcript(item: de.xikolo.models.Item, video: Video) : Item(video.transcriptUrl, "transcript_${item.id}.pdf", item) {
                override val title = "Transcript: " + item.title
                override val size = video.transcriptSize.toLong()
            }

            class VideoSD(item: de.xikolo.models.Item, val video: Video) : Item(video.streamToPlay.sdUrl, "video_sd_${item.id}.mp4", item) {
                override val title = "Video (SD): " + item.title
                override val mimeType = "video/mp4"
                override val size = video.streamToPlay.sdSize.toLong()

                override val secondaryAssets: MutableSet<DownloadAsset>
                    get() {
                        val subtitles = mutableSetOf<DownloadAsset>()
                        video.subtitles.forEach {
                            subtitles.add(Subtitles(it, item))
                        }
                        return subtitles
                    }
                override val deleteSecondaryAssets: (DownloadAsset, DownloadManager) -> Boolean = { _, downloadManager ->
                    !downloadManager.downloadExists(VideoSD(item, video)) && !downloadManager.downloadExists(VideoHD(item, video))
                }
            }

            class VideoHD(item: de.xikolo.models.Item, val video: Video) : Item(video.streamToPlay.hdUrl, "video_hd_${item.id}.mp4", item) {
                override val title = "Video (HD): " + item.title
                override val mimeType = "video/mp4"
                override val size = video.streamToPlay.hdSize.toLong()

                override val secondaryAssets: MutableSet<DownloadAsset>
                    get() {
                        val subtitles = mutableSetOf<DownloadAsset>()
                        video.subtitles.forEach {
                            subtitles.add(Subtitles(it, item))
                        }
                        return subtitles
                    }
                override val deleteSecondaryAssets: (DownloadAsset, DownloadManager) -> Boolean = { _, downloadManager ->
                    !downloadManager.downloadExists(VideoSD(item, video)) && !downloadManager.downloadExists(VideoHD(item, video))
                }
            }

            class Audio(item: de.xikolo.models.Item, video: Video) : Item(video.audioUrl, "audio_${item.id}.mp3", item) {
                override val title = "Audio: " + item.title
                override val mimeType = "audio/mpeg"
                override val size = video.audioSize.toLong()
            }

            class Subtitles(videoSubtitles: VideoSubtitles, item: de.xikolo.models.Item) : Item(videoSubtitles.vttUrl, "subtitles_${videoSubtitles.language}_${item.id}.vtt", item) {
                override val fileFolder
                    get() = super.fileFolder + File.separator + "Subtitles"

                override val showNotification = false
                override val mimeType = "text/vtt"
            }
        }
    }
}
