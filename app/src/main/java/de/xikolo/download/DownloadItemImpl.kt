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

abstract class DownloadItemImpl<out D, I : DownloadIdentifier, R : DownloadRequest>(
    var storage: Storage
) : DownloadItem<D, I> {

    companion object {
        val TAG: String? = DownloadItemImpl::class.simpleName
    }

    protected val context = App.instance

    abstract val downloader: DownloadHandler<I, R>

    private var downloaderIdentifier: I? = null
    final override val identifier: I
        get() = downloaderIdentifier ?: itemIdentifier

    override val openAction: ((FragmentActivity) -> Unit)? = null

    abstract val request: R

    abstract val itemIdentifier: I

    private val statusCache: DownloadStatus.DownloadStatusLiveData by lazy {
        DownloadStatus.DownloadStatusLiveData(
            null,
            null,
            if (download != null) DownloadStatus.State.DOWNLOADED else DownloadStatus.State.DELETED
        )
    }

    final override val status: DownloadStatus.DownloadStatusLiveData by lazy {
        downloader.listen(identifier) {
            onStatusChanged(it)
        }
        statusCache
    }

    protected open fun onStatusChanged(newStatus: DownloadStatus) {
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
                        { identifier ->
                            if (identifier != null) {
                                downloaderIdentifier = identifier

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
                if (it) {
                    status.state = DownloadStatus.State.DELETED
                }
                callback?.invoke(it && existed)
            }
        }
    }

    private fun performAction(activity: FragmentActivity, action: () -> Unit): Boolean {
        return if (storage.isWritable) {
            if (PermissionManager(activity).requestPermission(
                    PermissionManager.WRITE_EXTERNAL_STORAGE
                ) == 1
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
