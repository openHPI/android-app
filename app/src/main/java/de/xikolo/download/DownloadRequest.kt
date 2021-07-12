package de.xikolo.download

/**
 * Definition of a download request.
 */
interface DownloadRequest {

    /**
     * The title of the download. Might be shown in a notification.
     */
    val title: String

    /**
     * Whether a notification should be shown for the download.
     */
    val showNotification: Boolean

    /**
     * The category of the download.
     */
    val category: DownloadCategory
}
