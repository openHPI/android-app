package de.xikolo.download

import de.xikolo.models.Storage

/**
 * Definition of a download handler,
 * the component which handles the actual downloading and download management.
 *
 * @param I The [DownloadIdentifier] type.
 * @param R The [DownloadRequest] type.
 */
interface DownloadHandler<I : DownloadIdentifier, R : DownloadRequest> {

    /**
     * Checks whether anything is currently being downloaded.
     *
     * @param callback An asynchronous callback either returning true or false.
     * This callback is always invoked if not null.
     */
    fun isDownloadingAnything(callback: (Boolean) -> Unit)

    /**
     * Returns the identifier for a download request without downloading it.
     *
     * @param request The download request.
     * @return The [DownloadIdentifier] for the request.
     */
    fun identify(request: R): I

    /**
     * Initiates the downloading process.
     *
     * @param request The download request which specifies the downloading.
     * @param callback An asynchronous callback to deliver a return value.
     * It returns true when the download was initiated successfully, otherwise false.
     * This callback is always invoked if not null.
     */
    fun download(
        request: R,
        callback: ((Boolean) -> Unit)? = null
    )

    /**
     * Deletes a download. When the download is pending or running, it is canceled first.
     *
     * @param identifier The identifier of the download.
     * @param callback An asynchronous callback to deliver a return value.
     * It returns true when the download deletion was initiated successfully, otherwise false.
     * This callback is always invoked if not null.
     */
    fun delete(
        identifier: I,
        callback: ((Boolean) -> Unit)? = null
    )

    /**
     * Registers a listener for a download that notifies when the download status changes.
     * Overrides the previous listener.
     * The listener is always invoked immediately after registering it.
     *
     * @param identifier The identifier of the download.
     * @param listener An asynchronous callback that is invoked regularly with the most recent
     * download status. There does not necessarily have to be a status change between calls.
     * Supplying null here removes any listener.
     */
    fun listen(
        identifier: I,
        listener: ((DownloadStatus) -> Unit)?
    )

    /**
     * Queries all downloads that have been successfully downloaded.
     *
     * @param storage The storage location of downloads to query.
     * @param callback An asynchronous callback that returns a Map from download identifier to
     * download status and category.
     * This callback is always invoked if not null.
     */
    fun getDownloads(
        storage: Storage,
        callback: (Map<I, Pair<DownloadStatus, DownloadCategory>>) -> Unit
    )
}
