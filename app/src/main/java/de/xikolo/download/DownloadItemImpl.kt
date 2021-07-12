package de.xikolo.download

import android.util.Log
import androidx.fragment.app.FragmentActivity
import de.xikolo.App
import de.xikolo.extensions.observeOnce
import de.xikolo.managers.PermissionManager
import de.xikolo.models.DownloadAsset
import de.xikolo.models.Storage
import de.xikolo.states.PermissionStateLiveData
import de.xikolo.utils.LanalyticsUtil
import de.xikolo.utils.extensions.buildWriteErrorMessage
import de.xikolo.utils.extensions.showToast

/**
 * Abstract implementation of [DownloadItem] behavior.
 *
 * @param D The type of the download object.
 * @param I The [DownloadIdentifier] type.
 * @param R The [DownloadRequest] type.
 *
 * @param storage The storage to persist the download on.
 */
abstract class DownloadItemImpl<out D, I : DownloadIdentifier, R : DownloadRequest>(
    var storage: Storage
) : DownloadItem<D, I> {

    companion object {
        val TAG: String? = DownloadItemImpl::class.simpleName
    }

    protected val context = App.instance

    /**
     * The download handler.
     */
    abstract val downloader: DownloadHandler<I, R>

    final override val identifier: I
        get() = downloader.identify(request)

    override val openAction: ((FragmentActivity) -> Unit)? = null

    /**
     * The download request.
     * Must not be accessed when [downloadable] is false.
     */
    abstract val request: R

    private val statusCache: DownloadStatus.DownloadStatusLiveData by lazy {
        // this has to be lazy initialized because [download] might not be available at class init
        DownloadStatus.DownloadStatusLiveData(
            null,
            null,
            if (download != null) DownloadStatus.State.DOWNLOADED else DownloadStatus.State.DELETED
        )
    }

    final override val status: DownloadStatus.DownloadStatusLiveData by lazy {
        // register a listener here which updates the cache and always return the cached status to
        // realize synchronous function
        downloader.listen(identifier) {
            onStatusChanged(it)
        }
        statusCache
    }

    protected open fun onStatusChanged(newStatus: DownloadStatus) {
        // update the status cache
        statusCache.value = newStatus
    }

    override fun start(activity: FragmentActivity, callback: ((Boolean) -> Unit)?) {
        performAction(activity) {
            when {
                !downloadable ||
                    status.state == DownloadStatus.State.PENDING ||
                    status.state == DownloadStatus.State.RUNNING ||
                    status.state == DownloadStatus.State.DOWNLOADED -> callback?.invoke(false)
                downloadable -> {
                    downloader.download(
                        request,
                        { success ->
                            if (success) {
                                if (this is DownloadAsset.Course.Item) {
                                    LanalyticsUtil.trackDownloadedFile(this)
                                }

                                callback?.invoke(true)
                            } else {
                                callback?.invoke(false)
                            }
                        }
                    )
                    downloader.listen(identifier) {
                        onStatusChanged(it)
                    }
                    status.state = DownloadStatus.State.PENDING
                }
                else -> callback?.invoke(false)
            }
        }
    }

    override fun delete(activity: FragmentActivity, callback: ((Boolean) -> Unit)?) {
        performAction(activity) {
            val existed = status.state == DownloadStatus.State.DOWNLOADED
            downloader.delete(identifier) {
                /*if (it) {
                    status.state = DownloadStatus.State.DELETED
                }*/
                callback?.invoke(it && existed)
            }
        }
    }

    /**
     * Checks and requests permissions.
     *
     * @param activity The context activity for permission management.
     * @param action Executable block which is invoked when permissions are fine.
     */
    private fun performAction(activity: FragmentActivity, action: () -> Unit): Boolean {
        return if (storage.isWritable) {
            if (
                PermissionManager(activity)
                    .requestPermission(PermissionManager.WRITE_EXTERNAL_STORAGE) == 1
            ) {
                action()
                true
            } else {
                context.state.permission.of(
                    PermissionManager.REQUEST_CODE_WRITE_EXTERNAL_STORAGE
                )
                    .observeOnce(activity) { state ->
                        return@observeOnce if (
                            state == PermissionStateLiveData.PermissionStateCode.GRANTED
                        ) {
                            performAction(activity, action)
                            true
                        } else false
                    }
                false
            }
        } else {
            val msg = context.buildWriteErrorMessage()
            Log.w(TAG, msg)
            activity.showToast(msg)
            false
        }
    }
}
