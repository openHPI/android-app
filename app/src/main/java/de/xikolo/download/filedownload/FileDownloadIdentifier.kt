package de.xikolo.download.filedownload

import de.xikolo.download.DownloadIdentifier

data class FileDownloadIdentifier(
    private val id: Int
) : DownloadIdentifier {

    fun get(): Int {
        return id
    }
}
