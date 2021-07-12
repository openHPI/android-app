package de.xikolo.download.filedownload

import de.xikolo.download.DownloadIdentifier

/**
 * The DownloadIdentifier class for file downloads.
 *
 * @param id The internal download identifier supplied by Fetch.
 */
data class FileDownloadIdentifier(
    private val id: Int
) : DownloadIdentifier {

    /**
     * Returns the internal identifier.
     */
    fun get(): Int {
        return id
    }
}
