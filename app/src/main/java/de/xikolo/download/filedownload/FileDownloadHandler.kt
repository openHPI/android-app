package de.xikolo.download.filedownload

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.DownloadNotification
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock
import de.xikolo.App
import de.xikolo.download.DownloadHandler
import de.xikolo.download.DownloadStatus
import de.xikolo.download.filedownload.FileDownloadRequest.Companion.REQUEST_EXTRA_CATEGORY
import de.xikolo.download.filedownload.FileDownloadRequest.Companion.REQUEST_EXTRA_SHOW_NOTIFICATION
import de.xikolo.download.filedownload.FileDownloadRequest.Companion.REQUEST_EXTRA_TITLE
import de.xikolo.models.Storage
import de.xikolo.utils.NotificationUtil
import de.xikolo.utils.extensions.createIfNotExists
import java.io.File

object FileDownloadHandler : DownloadHandler<FileDownloadIdentifier, FileDownloadRequest> {

    private val context = App.instance

    private val disabledNotificationsConfiguration =
        FetchConfiguration.Builder(context)
            .setAutoRetryMaxAttempts(1)
            .setDownloadConcurrentLimit(5)
            .enableFileExistChecks(false)
            .setNamespace("XIKOLO_FETCH_NO_NOTIFICATIONS")
            .build()

    private val enabledNotificationsConfiguration =
        FetchConfiguration.Builder(context)
            .setAutoRetryMaxAttempts(1)
            .setDownloadConcurrentLimit(5)
            .enableFileExistChecks(false)
            .setNamespace("XIKOLO_FETCH")
            .setNotificationManager(
                object : DefaultFetchNotificationManager(context) {
                    override fun getFetchInstanceForNamespace(namespace: String): Fetch {
                        return enabledNotificationsManager
                    }

                    override fun createNotificationChannels(
                        context: Context,
                        notificationManager: NotificationManager
                    ) {
                        NotificationUtil(context)
                    }

                    override fun getChannelId(notificationId: Int, context: Context): String {
                        return NotificationUtil.DOWNLOADS_CHANNEL_ID
                    }

                    override fun updateGroupSummaryNotification(
                        groupId: Int,
                        notificationBuilder: NotificationCompat.Builder,
                        downloadNotifications: List<DownloadNotification>,
                        context: Context
                    ): Boolean {
                        val util = NotificationUtil(context)
                        if (downloadNotifications.isNotEmpty()) {
                            util.notify(
                                NotificationUtil.DOWNLOAD_RUNNING_NOTIFICATION_ID,
                                util.getDownloadRunningGroupNotification(
                                    downloadNotifications.size
                                )
                            )
                        } else {
                            util.cancel(NotificationUtil.DOWNLOAD_RUNNING_NOTIFICATION_ID)
                        }
                        return false
                    }

                    override fun updateNotification(
                        notificationBuilder: NotificationCompat.Builder,
                        downloadNotification: DownloadNotification,
                        context: Context
                    ) {
                        NotificationUtil(context).updateDownloadRunningNotification(
                            notificationBuilder,
                            downloadNotification.title,
                            downloadNotification.progress,
                            getActionPendingIntent(
                                downloadNotification,
                                DownloadNotification.ActionType.CANCEL
                            )
                        )
                    }

                    override fun getDownloadNotificationTitle(download: Download): String {
                        return download.request.extras.getString(
                            REQUEST_EXTRA_TITLE,
                            download.request.url
                        )
                    }

                    override fun getNotificationBuilder(
                        notificationId: Int,
                        groupId: Int
                    ): NotificationCompat.Builder {
                        return NotificationUtil(context)
                            .getDownloadRunningNotification()
                    }

                    override fun shouldCancelNotification(
                        downloadNotification: DownloadNotification
                    ): Boolean {
                        return downloadNotification.isCompleted
                    }
                }
            )
            .build()

    private val enabledNotificationsManager: Fetch = Fetch.getInstance(
        enabledNotificationsConfiguration
    )

    private val disabledNotificationsManager: Fetch = Fetch.getInstance(
        disabledNotificationsConfiguration
    )

    init {
        val listener = object : FetchListener {
            override fun onAdded(download: Download) {
                notifyStatus(download)
            }

            override fun onCancelled(download: Download) {
                delete(FileDownloadIdentifier(download.id))
                notifyStatus(download)
            }

            override fun onCompleted(download: Download) {
                if (download.extras.getBoolean(REQUEST_EXTRA_SHOW_NOTIFICATION, true)) {
                    NotificationUtil(context).showDownloadCompletedNotification(
                        download.extras.getString(REQUEST_EXTRA_TITLE, download.file)
                    )
                }
                notifyStatus(download)
            }

            override fun onDeleted(download: Download) {
                notifyStatus(download)
            }

            override fun onDownloadBlockUpdated(
                download: Download,
                downloadBlock: DownloadBlock,
                totalBlocks: Int
            ) {
            }

            override fun onError(download: Download, error: Error, throwable: Throwable?) {
                notifyStatus(download)
            }

            override fun onPaused(download: Download) {
                notifyStatus(download)
            }

            override fun onProgress(
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long
            ) {
                notifyStatus(download)
            }

            override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                notifyStatus(download)
            }

            override fun onRemoved(download: Download) {}

            override fun onResumed(download: Download) {
                notifyStatus(download)
            }

            override fun onStarted(
                download: Download,
                downloadBlocks: List<DownloadBlock>,
                totalBlocks: Int
            ) {
                notifyStatus(download)
            }

            override fun onWaitingNetwork(download: Download) {}
        }

        disabledNotificationsManager.addListener(listener)
        enabledNotificationsManager.addListener(listener)
    }

    private val listeners: MutableMap<Int, ((DownloadStatus) -> Unit)?> = mutableMapOf()

    override fun isDownloadingAnything(callback: (Boolean) -> Unit) {
        disabledNotificationsManager.hasActiveDownloads(true) { a ->
            enabledNotificationsManager.hasActiveDownloads(true) { b ->
                callback(a || b)
            }
        }
    }

    override fun download(
        request: FileDownloadRequest,
        callback: ((FileDownloadIdentifier?) -> Unit)?
    ) {
        val req = request.buildRequest()
        File(req.file).parentFile?.createIfNotExists()

        if (req.extras.getBoolean(REQUEST_EXTRA_SHOW_NOTIFICATION, true)) {
            enabledNotificationsManager
        } else {
            disabledNotificationsManager
        }.enqueue(
            req,
            {
                callback?.invoke(FileDownloadIdentifier(it.id))
            },
            {
                callback?.invoke(null)
            }
        )
    }

    override fun delete(identifier: FileDownloadIdentifier, callback: ((Boolean) -> Unit)?) {
        enabledNotificationsManager.getDownload(identifier.get()) { d1 ->
            if (d1 != null) {
                enabledNotificationsManager.cancel(d1.id)
                enabledNotificationsManager.delete(d1.id)
                enabledNotificationsManager.remove(d1.id)
                callback?.invoke(true)
            } else {
                disabledNotificationsManager.getDownload(identifier.get()) { d2 ->
                    if (d2 != null) {
                        disabledNotificationsManager.cancel(d2.id)
                        disabledNotificationsManager.delete(d2.id)
                        disabledNotificationsManager.remove(d2.id)
                        callback?.invoke(true)
                    } else {
                        callback?.invoke(false)
                    }
                }
            }
        }
    }

    override fun listen(
        identifier: FileDownloadIdentifier,
        listener: ((DownloadStatus) -> Unit)?
    ) {
        listeners[identifier.get()] = listener
        disabledNotificationsManager.getDownload(identifier.get()) { a ->
            enabledNotificationsManager.getDownload(identifier.get()) { b ->
                listener?.invoke(
                    getDownloadStatus(a ?: b)
                )
            }
        }
    }

    override fun getDownloads(
        storage: Storage,
        callback: (Map<FileDownloadIdentifier, Pair<DownloadStatus, String?>>) -> Unit
    ) {
        disabledNotificationsManager.getDownloads { a ->
            enabledNotificationsManager.getDownloads { b ->
                callback.invoke(
                    a
                        .filter { it.file.contains(storage.file.absolutePath) }
                        .associate {
                            Pair(
                                FileDownloadIdentifier(it.id),
                                getDownloadStatus(it) to it.extras.getString(
                                    REQUEST_EXTRA_CATEGORY,
                                    ""
                                ).takeUnless { it.isEmpty() }
                            )
                        } +
                        b
                            .filter { it.file.contains(storage.file.absolutePath) }
                            .associate {
                                Pair(
                                    FileDownloadIdentifier(it.id),
                                    getDownloadStatus(it) to it.extras.getString(
                                        REQUEST_EXTRA_CATEGORY,
                                        ""
                                    ).takeUnless { it.isEmpty() }
                                )
                            }
                )
            }
        }
    }

    private fun notifyStatus(download: Download) {
        listeners[download.id]?.invoke(
            getDownloadStatus(download)
        )
    }

    private fun getDownloadStatus(download: Download?): DownloadStatus {
        return try {
            download!!
            DownloadStatus(
                download.total,
                download.downloaded,
                when (download.status) {
                    Status.ADDED, Status.QUEUED -> DownloadStatus.State.PENDING
                    Status.FAILED -> DownloadStatus.State.DELETED
                    Status.COMPLETED -> DownloadStatus.State.DOWNLOADED
                    Status.CANCELLED -> DownloadStatus.State.DELETED
                    Status.DOWNLOADING, Status.PAUSED -> DownloadStatus.State.RUNNING
                    else -> throw Exception()
                }
            )
        } catch (e: Exception) {
            DownloadStatus(null, null, DownloadStatus.State.DELETED)
        }
    }
}
