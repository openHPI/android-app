package de.xikolo.download

import androidx.fragment.app.FragmentActivity

interface DownloadItem<out D, I : DownloadIdentifier> {

    val identifier: I

    val download: D?

    val downloadable: Boolean

    val title: String

    val openAction: ((FragmentActivity) -> Unit)?

    val size: Long

    val status: DownloadStatus.DownloadStatusLiveData

    fun start(activity: FragmentActivity, callback: ((Boolean) -> Unit)? = null)

    fun delete(activity: FragmentActivity, callback: ((Boolean) -> Unit)? = null)
}
