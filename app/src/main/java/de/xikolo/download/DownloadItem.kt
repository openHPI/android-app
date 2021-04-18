package de.xikolo.download

import androidx.fragment.app.FragmentActivity

/**
 * Definition of a downloadable thing.
 *
 * @param D The type of the download object.
 * @param I The [DownloadIdentifier] type.
 */
interface DownloadItem<out D, I : DownloadIdentifier> {

    /**
     * The identifier of the download.
     * Must not be accessed when [downloadable] is false.
     */
    val identifier: I

    /**
     * The download object.
     * Is null when the download is not available, e.g. it has not been downloaded,
     * or when an error occurred.
     */
    val download: D?

    /**
     * Whether the item can be downloaded.
     */
    val downloadable: Boolean

    /**
     * The title of the download.
     */
    val title: String

    /**
     * Executable block to open the download.
     */
    val openAction: ((FragmentActivity) -> Unit)?

    /**
     * Total size of the download.
     */
    val size: Long

    /**
     * Subscriptable [LiveData] status of the download.
     */
    val status: DownloadStatus.DownloadStatusLiveData

    /**
     * Starts the downloading process.
     *
     * @param activity The context activity for the download. Used to e.g. check permissions.
     * @param callback An asynchronous callback to deliver a return value.
     * It returns true when downloading started successfully, otherwise false.
     * This callback is always invoked if not null.
     */
    fun start(activity: FragmentActivity, callback: ((Boolean) -> Unit)? = null)

    /**
     * Deletes the download. When the download is pending or running, it is canceled first.
     *
     * @param activity The context activity for the download. Used to e.g. check permissions.
     * @param callback An asynchronous callback to deliver a return value.
     * It returns true when the download deletion was initiated successfully, otherwise false.
     * This callback is always invoked if not null.
     */
    fun delete(activity: FragmentActivity, callback: ((Boolean) -> Unit)? = null)
}
