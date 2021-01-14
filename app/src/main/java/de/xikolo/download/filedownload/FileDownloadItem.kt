package de.xikolo.download.filedownload

import androidx.fragment.app.FragmentActivity
import de.xikolo.App
import de.xikolo.download.DownloadItemImpl
import de.xikolo.download.DownloadStatus
import de.xikolo.models.Storage
import de.xikolo.utils.extensions.fileSize
import de.xikolo.utils.extensions.internalStorage
import de.xikolo.utils.extensions.open
import de.xikolo.utils.extensions.preferredStorage
import de.xikolo.utils.extensions.sdcardStorage
import java.io.File

open class FileDownloadItem(
    val url: String?,
    val category: String?,
    open val fileName: String,
    storage: Storage = App.instance.preferredStorage
) : DownloadItemImpl<File, FileDownloadIdentifier, FileDownloadRequest>(storage) {

    final override val downloader = FileDownloadHandler

    final override val downloadable: Boolean
        get() = url != null

    override val title: String
        get() = fileName

    override val size: Long
        get() = download?.fileSize ?: 0L

    final override val download: File?
        get() {
            val originalStorage: Storage = storage

            storage = App.instance.internalStorage
            val internalFile = File(filePath)
            if (internalFile.exists() && internalFile.isFile) {
                storage = originalStorage
                return internalFile
            }

            App.instance.sdcardStorage?.let {
                storage = it
                val sdcardFile = File(filePath)
                if (sdcardFile.exists() && sdcardFile.isFile) {
                    storage = originalStorage
                    return sdcardFile
                }
            }

            storage = originalStorage
            return null
        }

    override val openAction: ((FragmentActivity) -> Unit)?
        get() = { activity ->
            download?.open(activity, mimeType, false)
        }

    final override val request
        get() = FileDownloadRequest(
            url!!,
            File("$filePath.tmp"),
            title,
            showNotification,
            category
        )

    final override val itemIdentifier: FileDownloadIdentifier
        get() = FileDownloadIdentifier(request.buildRequest().id)

    override fun onStatusChanged(newStatus: DownloadStatus) {
        super.onStatusChanged(newStatus)
        when (newStatus.state) {
            DownloadStatus.State.DOWNLOADED -> {
                File("$filePath.tmp").renameTo(File(filePath))
            }
            DownloadStatus.State.DELETED -> {
                File(filePath).delete()
            }
        }
    }

    protected open fun getFileFolder(): String {
        return storage.file.absolutePath
    }

    val filePath: String
        get() = getFileFolder() + File.separator + fileName

    protected open val mimeType = "application/pdf"

    protected open val showNotification = true
}
