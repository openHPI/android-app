package de.xikolo.download

import de.xikolo.models.Storage

interface DownloadHandler<I : DownloadIdentifier, R : DownloadRequest> {

    fun isDownloadingAnything(callback: (Boolean) -> Unit)

    fun download(
        request: R,
        callback: ((I?) -> Unit)? = null
    )

    fun delete(
        identifier: I,
        callback: ((Boolean) -> Unit)? = null
    )

    fun listen(
        identifier: I,
        listener: ((DownloadStatus) -> Unit)?
    )

    fun getDownloads(
        storage: Storage,
        callback: (Map<I, Pair<DownloadStatus, String?>>) -> Unit
    )
}
