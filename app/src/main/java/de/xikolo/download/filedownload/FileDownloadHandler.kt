package de.xikolo.download.filedownload

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.gson.Gson
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.DownloadNotification
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Extras
import de.xikolo.App
import de.xikolo.R
import de.xikolo.config.Config
import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadHandler
import de.xikolo.download.DownloadStatus
import de.xikolo.managers.UserManager
import de.xikolo.models.Storage
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.NotificationUtil
import de.xikolo.utils.extensions.createIfNotExists
import java.io.File
import java.util.Locale

/**
 * DownloadHandler for progressive downloads to a file.
 * Based on [Fetch].
 */
object FileDownloadHandler : DownloadHandler<FileDownloadIdentifier, FileDownloadRequest> {

    const val REQUEST_EXTRA_TITLE = "title"
    const val REQUEST_EXTRA_SHOW_NOTIFICATION = "showNotification"
    const val REQUEST_EXTRA_CATEGORY = "category"

    private val context: Context
        get() = App.instance

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
                        // initialize NotificationUtil to create Channels
                        NotificationUtil.getInstance(context)
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
                        val util = NotificationUtil.getInstance(context)
                        if (downloadNotifications.isNotEmpty()) {
                            util.notify(
                                NotificationUtil.DOWNLOAD_RUNNING_NOTIFICATION_ID,
                                util.getDownloadRunningGroupNotification(
                                    this,
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
                        NotificationUtil.getInstance(context).updateDownloadRunningNotification(
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
                        return NotificationUtil.getInstance(context)
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
                    NotificationUtil.getInstance(context).showDownloadCompletedNotification(
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

    override fun identify(request: FileDownloadRequest): FileDownloadIdentifier {
        return FileDownloadIdentifier(Request(request.url, request.localFile.toUri()).id)
    }

    override fun download(request: FileDownloadRequest, callback: ((Boolean) -> Unit)?) {
        val req = Request(request.url, request.localFile.toUri()).apply {
            networkType =
                if (ApplicationPreferences().isDownloadNetworkLimitedOnMobile) {
                    NetworkType.WIFI_ONLY
                } else {
                    NetworkType.ALL
                }

            val extrasMap =
                mutableMapOf(
                    REQUEST_EXTRA_TITLE to request.title,
                    REQUEST_EXTRA_SHOW_NOTIFICATION to request.showNotification.toString(),
                    REQUEST_EXTRA_CATEGORY to Gson().toJson(
                        request.category,
                        DownloadCategory::class.java
                    )
                )

            extras = Extras(extrasMap)
            groupId = 0

            addHeader(Config.HEADER_USER_AGENT, Config.HEADER_USER_AGENT_VALUE)
            addHeader(
                Config.HEADER_ACCEPT,
                Config.MEDIA_TYPE_JSON_API + "; xikolo-version=" + Config.XIKOLO_API_VERSION
            )
            addHeader(Config.HEADER_CONTENT_TYPE, Config.MEDIA_TYPE_JSON_API)
            addHeader(Config.HEADER_USER_PLATFORM, Config.HEADER_USER_PLATFORM_VALUE)
            addHeader(Config.HEADER_ACCEPT_LANGUAGE, Locale.getDefault().language)

            if (url.toUri().host == App.instance.getString(R.string.app_host) &&
                UserManager.isAuthorized
            ) {
                addHeader(
                    Config.HEADER_AUTH,
                    Config.HEADER_AUTH_VALUE_PREFIX_JSON_API + UserManager.token!!
                )
            }
        }

        File(req.file).parentFile?.createIfNotExists()

        if (req.extras.getBoolean(REQUEST_EXTRA_SHOW_NOTIFICATION, true)) {
            enabledNotificationsManager
        } else {
            disabledNotificationsManager
        }.enqueue(
            req,
            {
                callback?.invoke(true)
            },
            {
                callback?.invoke(false)
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

        var called = false
        disabledNotificationsManager.getDownload(identifier.get()) { a ->
            enabledNotificationsManager.getDownload(identifier.get()) { b ->
                listener?.invoke(
                    getDownloadStatus(a ?: b)
                )
                called = true
            }
        }
        while (!called) {
            Thread.sleep(100)
        }
    }

    override fun getDownloads(
        storage: Storage,
        callback: (Map<FileDownloadIdentifier, Pair<DownloadStatus, DownloadCategory>>) -> Unit
    ) {
        disabledNotificationsManager.getDownloads { a ->
            enabledNotificationsManager.getDownloads { b ->
                callback.invoke(
                    a
                        .filter { it.file.contains(storage.file.absolutePath) }
                        .associate {
                            Pair(
                                FileDownloadIdentifier(it.id),
                                getDownloadStatus(it) to Gson().fromJson(
                                    it.extras.getString(
                                        REQUEST_EXTRA_CATEGORY,
                                        ""
                                    ),
                                    DownloadCategory::class.java
                                )
                            )
                        } +
                        b
                            .filter { it.file.contains(storage.file.absolutePath) }
                            .associate {
                                Pair(
                                    FileDownloadIdentifier(it.id),
                                    getDownloadStatus(it) to Gson().fromJson(
                                        it.extras.getString(
                                            REQUEST_EXTRA_CATEGORY,
                                            ""
                                        ),
                                        DownloadCategory::class.java
                                    )
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
                },
                download.error.throwable
            )
        } catch (e: Exception) {
            DownloadStatus(null, null, DownloadStatus.State.DELETED, null)
        }
    }
}
