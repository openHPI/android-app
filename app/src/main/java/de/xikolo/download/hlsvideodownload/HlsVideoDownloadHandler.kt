package de.xikolo.download.hlsvideodownload

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Requirements
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import de.xikolo.App
import de.xikolo.config.Config
import de.xikolo.download.DownloadHandler
import de.xikolo.download.DownloadStatus
import de.xikolo.download.hlsvideodownload.services.HlsVideoDownloadForegroundService
import de.xikolo.download.hlsvideodownload.services.HlsVideoDownloadInternalStorageBackgroundService
import de.xikolo.download.hlsvideodownload.services.HlsVideoDownloadInternalStorageForegroundService
import de.xikolo.download.hlsvideodownload.services.HlsVideoDownloadSdcardStorageBackgroundService
import de.xikolo.download.hlsvideodownload.services.HlsVideoDownloadSdcardStorageForegroundService
import de.xikolo.models.Storage
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.NotificationUtil
import de.xikolo.utils.extensions.internalStorage
import de.xikolo.utils.extensions.sdcardStorage
import java.io.File
import java.util.concurrent.Executors

object HlsVideoDownloadHandler :
    DownloadHandler<HlsVideoDownloadIdentifier, HlsVideoDownloadRequest> {

    private val downloads: MutableMap<String, Download> = mutableMapOf()
    private val listeners: MutableMap<String, ((DownloadStatus) -> Unit)?> =
        mutableMapOf()

    private var databaseProvider: DatabaseProvider? = null

    private var internalCache: Cache? = null
    private var sdcardCache: Cache? = null
    private var managers: MutableMap<Long, DownloadManager?> = mutableMapOf()

    private var notifierThreadsRunning: MutableMap<DownloadManager, Boolean> = mutableMapOf()

    val dataSourceFactory = DefaultHttpDataSourceFactory(Config.HEADER_USER_AGENT_VALUE)

    val context = App.instance

    init {
        DownloadService.start(
            context,
            HlsVideoDownloadForegroundService::class.java
        )
    }

    fun getDatabaseProvider(context: Context): DatabaseProvider {
        return databaseProvider ?: synchronized(this) {
            databaseProvider ?: ExoDatabaseProvider(context).also {
                databaseProvider = it
            }
        }
    }

    fun getInternalStorageCache(context: Context): Cache {
        return internalCache ?: synchronized(this) {
            internalCache ?: SimpleCache(
                File(context.internalStorage.file.absolutePath + File.separator + "Videos"),
                NoOpCacheEvictor(),
                getDatabaseProvider(context)
            ).also {
                internalCache = it
            }
        }
    }

    fun getSdcardStorageCache(context: Context): Cache? {
        return context.sdcardStorage?.let {
            sdcardCache ?: synchronized(this) {
                sdcardCache ?: SimpleCache(
                    File(it.file.absolutePath + File.separator + "Videos"),
                    NoOpCacheEvictor(),
                    getDatabaseProvider(context)
                ).also {
                    sdcardCache = it
                }
            }
        }
    }

    fun getManager(context: Context, cache: Cache): DownloadManager {
        return (
            managers[cache.uid]
                ?: synchronized(this) {
                    managers[cache.uid] ?: DownloadManager(
                        context,
                        getDatabaseProvider(context),
                        cache,
                        dataSourceFactory,
                        Executors.newSingleThreadExecutor()
                    ).apply {
                        maxParallelDownloads = 5
                        minRetryCount = 1

                        addListener(
                            object : DownloadManager.Listener {
                                override fun onDownloadChanged(
                                    downloadManager: DownloadManager,
                                    download: Download,
                                    finalException: java.lang.Exception?
                                ) {
                                    val identifier = download.request.id
                                    downloads[identifier] = download
                                    notifyStatus(identifier)

                                    val args = HlsVideoDownloadRequest.ArgumentWrapper.decode(
                                        download.request.data
                                    )
                                    if (download.state == Download.STATE_COMPLETED
                                        && args.showNotification
                                    ) {
                                        NotificationUtil(context).showDownloadCompletedNotification(
                                            args.title
                                        )
                                    }

                                    startNotifierThread(downloadManager)
                                }

                                override fun onIdle(downloadManager: DownloadManager) {
                                    stopNotifierThread(downloadManager)
                                }

                                override fun onDownloadRemoved(
                                    downloadManager: DownloadManager,
                                    download: Download
                                ) {
                                    val identifier = download.request.id
                                    notifyStatus(identifier)
                                    listeners.remove(identifier)
                                    downloads.remove(identifier)
                                }
                            }
                        )
                    }.also {
                        managers[cache.uid] = it
                    }
                }
            ).apply {
                if (ApplicationPreferences().isDownloadNetworkLimitedOnMobile) {
                    requirements = Requirements(
                        Requirements.NETWORK_UNMETERED
                    )
                }
            }
    }

    override fun isDownloadingAnything(callback: (Boolean) -> Unit) {
        callback.invoke(
            downloads.values.map { getDownloadStatus(it) }.any {
                it.state == DownloadStatus.State.RUNNING ||
                    it.state == DownloadStatus.State.PENDING
            }
        )
    }

    override fun download(
        request: HlsVideoDownloadRequest,
        callback: ((HlsVideoDownloadIdentifier?) -> Unit)?
    ) {
        request.buildRequest(context) { downloadRequest ->
            if (downloadRequest == null) {
                callback?.invoke(null)
                return@buildRequest
            }

            val identifier = downloadRequest.id
            downloads[identifier] = Download(
                downloadRequest,
                Download.STATE_QUEUED,
                0,
                0,
                -1,
                Download.STOP_REASON_NONE,
                Download.FAILURE_REASON_NONE
            )

            try {
                val service =
                    if (request.storage == context.internalStorage) {
                        HlsVideoDownloadInternalStorageForegroundService::class.java
                    } else if (request.storage == context.sdcardStorage &&
                        getSdcardStorageCache(context) != null
                    ) {
                        HlsVideoDownloadSdcardStorageForegroundService::class.java
                    } else {
                        throw Exception()
                    }

                DownloadService.sendAddDownload(
                    context,
                    service,
                    downloadRequest,
                    true
                )

                callback?.invoke(request.identifier)
            } catch (e: Exception) {
                downloads[identifier] = Download(
                    downloadRequest,
                    Download.STATE_FAILED,
                    0,
                    0,
                    -1,
                    Download.STOP_REASON_NONE,
                    Download.FAILURE_REASON_UNKNOWN
                )
                notifyStatus(identifier)
                callback?.invoke(null)
            }
        }
    }

    override fun delete(identifier: HlsVideoDownloadIdentifier, callback: ((Boolean) -> Unit)?) {
        DownloadService.sendRemoveDownload(
            context,
            HlsVideoDownloadInternalStorageBackgroundService::class.java,
            identifier.get(),
            false
        )

        if (getSdcardStorageCache(context) != null) {
            DownloadService.sendRemoveDownload(
                context,
                HlsVideoDownloadSdcardStorageBackgroundService::class.java,
                identifier.get(),
                false
            )
        }

        callback?.invoke(
            true
        )
    }

    override fun listen(
        identifier: HlsVideoDownloadIdentifier,
        listener: ((DownloadStatus) -> Unit)?
    ) {
        listeners[identifier.get()] = listener
        listener?.invoke(
            getDownloadStatus(
                getManager(context, getInternalStorageCache(context))
                    .downloadIndex.getDownload(identifier.get())
                    ?: getSdcardStorageCache(context)?.let { sdcardCache ->
                        getManager(context, sdcardCache)
                            .downloadIndex.getDownload(identifier.get())
                    }
            )
        )
    }

    override fun getDownloads(
        storage: Storage,
        callback: (Map<HlsVideoDownloadIdentifier, Pair<DownloadStatus, String?>>) -> Unit
    ) {
        val sdcardStorageCache = getSdcardStorageCache(context)
        val cache =
            if (storage == context.internalStorage) {
                getInternalStorageCache(context)
            } else if (storage == context.sdcardStorage && sdcardStorageCache != null) {
                sdcardStorageCache
            } else {
                callback.invoke(mapOf())
                return
            }

        callback.invoke(
            getManager(context, cache).downloadIndex.getDownloads().let {
                val map = mutableMapOf<HlsVideoDownloadIdentifier, Pair<DownloadStatus, String?>>()
                it.moveToFirst()
                while (!it.isAfterLast) {
                    map[HlsVideoDownloadIdentifier.from(it.download.request.id)] =
                        getDownloadStatus(it.download) to
                            HlsVideoDownloadRequest.ArgumentWrapper.decode(
                                it.download.request.data
                            ).category
                    it.moveToNext()
                }
                map
            }
        )
    }

    private fun getDownloadStatus(download: Download?): DownloadStatus {
        if (download == null) {
            return DownloadStatus(null, null, DownloadStatus.State.DELETED)
        }

        val totalSize = download.contentLength.takeUnless { it < 0 }
            ?: if (download.state == Download.STATE_COMPLETED) {
                download.bytesDownloaded
            } else {
                download.bytesDownloaded * 100 / download.percentDownloaded
            }.toLong()

        return DownloadStatus(
            totalSize,
            download.bytesDownloaded,
            when (download.state) {
                Download.STATE_QUEUED, Download.STATE_RESTARTING -> DownloadStatus.State.PENDING
                Download.STATE_DOWNLOADING -> DownloadStatus.State.RUNNING
                Download.STATE_COMPLETED -> DownloadStatus.State.DOWNLOADED
                else -> DownloadStatus.State.DELETED
            }
        )
    }

    private fun notifyStatus(identifier: String) {
        listeners[identifier]?.invoke(
            getDownloadStatus(downloads[identifier])
        )
    }

    private fun startNotifierThread(manager: DownloadManager) {
        if (notifierThreadsRunning[manager] == false) {
            notifierThreadsRunning[manager] = true
            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    manager.currentDownloads.forEach {
                        notifyStatus(it.request.id)
                    }
                    if (notifierThreadsRunning[manager] == true) {
                        handler.postDelayed(this, 1000)
                    }
                }
            }
            runnable.run()
        }
    }

    private fun stopNotifierThread(manager: DownloadManager) {
        notifierThreadsRunning[manager] = false
    }
}
