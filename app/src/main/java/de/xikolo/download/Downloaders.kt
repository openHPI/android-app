package de.xikolo.download

import de.xikolo.download.filedownload.FileDownloadHandler
import de.xikolo.download.filedownload.FileDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.models.Storage
import java.util.concurrent.atomic.AtomicInteger

object Downloaders {

    private lateinit var fileDownloader: FileDownloadHandler

    private lateinit var hlsDownloader: HlsVideoDownloadHandler

    fun initialize() {
        fileDownloader = FileDownloadHandler
        hlsDownloader = HlsVideoDownloadHandler
    }

    fun isDownloadingAnything(callback: (Boolean) -> Unit) {
        fileDownloader.isDownloadingAnything { a ->
            hlsDownloader.isDownloadingAnything { b ->
                callback.invoke(a || b)
            }
        }
    }

    fun getDownloads(
        storage: Storage,
        callback: (Map<DownloadIdentifier, Pair<DownloadStatus, String?>>) -> Unit
    ) {
        fileDownloader.getDownloads(storage) { a ->
            hlsDownloader.getDownloads(storage) { b ->
                callback.invoke(a + b)
            }
        }
    }

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
