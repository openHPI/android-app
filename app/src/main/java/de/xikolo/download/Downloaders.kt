package de.xikolo.download

import de.xikolo.download.filedownload.FileDownloadHandler
import de.xikolo.download.filedownload.FileDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.models.Storage
import java.util.concurrent.atomic.AtomicInteger

/**
 * Convenience class for combination of multiple download handlers.
 */
object Downloaders {

    private lateinit var fileDownloader: FileDownloadHandler

    private lateinit var hlsDownloader: HlsVideoDownloadHandler

    /**
     * Initializes all download handlers.
     */
    fun initialize() {
        fileDownloader = FileDownloadHandler
        hlsDownloader = HlsVideoDownloadHandler
    }

    /**
     * Checks whether any download handler currently downloads something.
     *
     * @param callback An asynchronous callback either returning true or false.
     * This callback is always invoked if not null.
     */
    fun isDownloadingAnything(callback: (Boolean) -> Unit) {
        fileDownloader.isDownloadingAnything { a ->
            hlsDownloader.isDownloadingAnything { b ->
                callback.invoke(a || b)
            }
        }
    }

    /**
     * Queries all download handlers for downloads that have been successfully downloaded.
     *
     * @param storage The storage location of downloads to query.
     * @param callback An asynchronous callback that returns a Map from download identifier to
     * download status and category.
     * This callback is always invoked if not null.
     */
    fun getDownloads(
        storage: Storage,
        callback: (Map<DownloadIdentifier, Pair<DownloadStatus, DownloadCategory>>) -> Unit
    ) {
        fileDownloader.getDownloads(storage) { a ->
            hlsDownloader.getDownloads(storage) { b ->
                callback.invoke(a + b)
            }
        }
    }

    /**
     * Deletes a collection of downloads.
     *
     * @param identifiers The collection of download identifiers.
     * @param callback An asynchronous callback that returns true if all downloads were deleted
     * successfully, otherwise false.
     * This callback is always invoked if not null.
     */
    fun deleteDownloads(
        identifiers: Collection<DownloadIdentifier>,
        callback: ((Boolean) -> Unit)? = null
    ) {
        val lock = AtomicInteger(identifiers.size)
        var globalSuccess = true
        val globalCallback: (Boolean) -> Unit = { success ->
            globalSuccess = globalSuccess and success
            if (lock.getAndDecrement() == 1) {
                callback?.invoke(globalSuccess)
            }
        }
        if (identifiers.isEmpty()) {
            callback?.invoke(true)
            return
        }
        identifiers.forEach {
            when (it) {
                is FileDownloadIdentifier -> fileDownloader.delete(it, globalCallback)
                is HlsVideoDownloadIdentifier -> hlsDownloader.delete(it, globalCallback)
                else -> globalCallback(false)
            }
        }
    }
}
