package de.xikolo.download.filedownload

import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadRequest
import java.io.File

/**
 * DownloadRequest class for file downloads
 *
 * @param url The URL of the file to download.
 * @param localFile The local File object to download to.
 * @param title The title of the download.
 * @param showNotification Whether to show a notification while downloading.
 * @param category The download category.
 */
data class FileDownloadRequest(
    val url: String,
    val localFile: File,
    override val title: String,
    override val showNotification: Boolean,
    override val category: DownloadCategory
) : DownloadRequest
