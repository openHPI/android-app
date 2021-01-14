package de.xikolo.download

interface DownloadRequest {

    val title: String

    val showNotification: Boolean

    val category: String?
}
