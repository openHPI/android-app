package de.xikolo.download.hlsvideodownload

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Requirements
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.gson.Gson
import de.xikolo.App
import de.xikolo.config.Config
import de.xikolo.download.DownloadCategory
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
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * DownloadHandler class for ExoPlayer HLS video downloads.
 */
object HlsVideoDownloadHandler :
    DownloadHandler<HlsVideoDownloadIdentifier, HlsVideoDownloadRequest> {

    val TAG: String = HlsVideoDownloadHandler::class.java.simpleName

    private val context: Context
        get() = App.instance

    private val downloads: MutableMap<String, Download> = mutableMapOf()
    private val listeners: MutableMap<String, ((DownloadStatus) -> Unit)?> =
        mutableMapOf()

    private var databaseProvider: DatabaseProvider? = null

    private var internalCache: Cache? = null
    private var sdcardCache: Cache? = null
    private var managers: MutableMap<Long, DownloadManager?> = mutableMapOf()

    private var notifierThreadsRunning: MutableMap<DownloadManager, Boolean> = mutableMapOf()

    val dataSourceFactory = DefaultHttpDataSourceFactory(Config.HEADER_USER_AGENT_VALUE)

    init {
        Log.i(TAG, "Starting $TAG")
        DownloadService.startForeground(
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
                        Executors.newCachedThreadPool()
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

                                    val args = ArgumentWrapper.decode(download.request.data)
                                    if (download.state == Download.STATE_COMPLETED &&
                                        args.showNotification
                                    ) {
                                        NotificationUtil.getInstance(context)
                                            .showDownloadCompletedNotification(
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
                                    Log.d(
                                        TAG,
                                        "Download successfully removed: ${download.request.id}"
                                    )
                                    val identifier = download.request.id
                                    val listener = listeners[identifier]
                                    listeners.remove(identifier)
                                    downloads.remove(identifier)
                                    listener?.invoke(
                                        getDownloadStatus(null)
                                    )
                                }
                            }
                        )
                    }.also {
                        managers[cache.uid] = it
                    }
                }
            )
            .apply {
                if (ApplicationPreferences().isDownloadNetworkLimitedOnMobile) {
                    requirements = Requirements(Requirements.NETWORK_UNMETERED)
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

    override fun identify(request: HlsVideoDownloadRequest): HlsVideoDownloadIdentifier {
        return HlsVideoDownloadIdentifier(request.url, request.quality)
    }

    override fun download(request: HlsVideoDownloadRequest, callback: ((Boolean) -> Unit)?) {
        Log.i(TAG, "Received download request: ${request.url}")
        val helper = DownloadHelper.forMediaItem(
            context,
            MediaItem.Builder().setUri(Uri.parse(request.url)).build(),
            DefaultRenderersFactory(context),
            dataSourceFactory
        )
        helper.prepare(
            object : DownloadHelper.Callback {
                override fun onPrepared(helper: DownloadHelper) {
                    val manifest = helper.manifest as HlsManifest

                    val formats = manifest.masterPlaylist.variants.map { it.format }
                    val lowestBitrate = formats.minOfOrNull { it.bitrate } ?: 0
                    val highestBitrate = formats.maxOfOrNull { it.bitrate } ?: 0
                    val targetBitrate = lowestBitrate + request.quality *
                        (highestBitrate - lowestBitrate)
                    val closestFormat = formats.minByOrNull {
                        abs(it.bitrate - targetBitrate).roundToInt()
                    }
                    val closestBitrate = closestFormat?.bitrate

                    val estimatedSize = (closestFormat?.averageBitrate
                        ?.takeUnless { it == Format.NO_VALUE }
                        ?: closestBitrate)
                        ?.times(manifest.mediaPlaylist.durationUs)
                        ?.div(8000000) // to bytes and seconds

                    val subtitles = manifest.masterPlaylist.subtitles
                        .mapNotNull {
                            it.format.language
                        }
                        .toTypedArray()

                    helper.clearTrackSelections(0)
                    helper.addTrackSelection(
                        0,
                        DefaultTrackSelector.ParametersBuilder(context)
                            .apply {
                                if (closestBitrate != null) {
                                    setMinVideoBitrate(closestBitrate - 1)
                                    setMaxVideoBitrate(closestBitrate + 1)
                                }
                            }
                            .build()
                    )
                    helper.addTextLanguagesToSelection(
                        true,
                        *subtitles
                    )

                    val downloadRequest = helper.getDownloadRequest(
                        identify(request).get(),
                        ArgumentWrapper(
                            request.title,
                            request.showNotification,
                            request.category,
                            estimatedSize
                        ).encode()
                    )
                    downloadRequest.customCacheKey
                    helper.release()

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
                                Log.i(
                                    TAG,
                                    "Starting downloading to internal storage: ${request.url} aka $identifier"
                                )
                                HlsVideoDownloadInternalStorageForegroundService::class.java
                            } else if (request.storage == context.sdcardStorage &&
                                getSdcardStorageCache(context) != null
                            ) {
                                Log.i(
                                    TAG,
                                    "Starting downloading to sdcard storage: ${request.url} aka $identifier"
                                )
                                HlsVideoDownloadSdcardStorageForegroundService::class.java
                            } else {
                                throw Exception("Error during storage selection")
                            }

                        DownloadService.sendAddDownload(
                            context,
                            service,
                            downloadRequest,
                            true
                        )

                        callback?.invoke(true)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Starting downloading failed: ${request.url} aka $identifier ($e)"
                        )
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
                        callback?.invoke(false)
                    }
                }

                override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                    Log.e(
                        TAG,
                        "Starting downloading failed in helper preparation: ${request.url} ($e)"
                    )
                    callback?.invoke(false)
                    helper.release()
                }
            }
        )
    }

    override fun delete(identifier: HlsVideoDownloadIdentifier, callback: ((Boolean) -> Unit)?) {
        Log.i(TAG, "Received deletion request: $identifier")
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
        Log.i(TAG, "Registering listener $listener for $identifier")
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
        callback: (Map<HlsVideoDownloadIdentifier, Pair<DownloadStatus, DownloadCategory>>) -> Unit
    ) {
        Log.i(
            TAG,
            "Querying all downloads in ${storage.file.absolutePath}"
        )
        val sdcardStorageCache = getSdcardStorageCache(context)
        val cache =
            if (storage == context.internalStorage) {
                getInternalStorageCache(context)
            } else if (storage == context.sdcardStorage && sdcardStorageCache != null) {
                sdcardStorageCache
            } else {
                Log.w(TAG, "Storage ${storage.file.absolutePath} not supported")
                callback.invoke(mapOf())
                return
            }

        callback.invoke(
            getManager(context, cache).downloadIndex.getDownloads(Download.STATE_COMPLETED).let {
                val map = mutableMapOf<HlsVideoDownloadIdentifier,
                    Pair<DownloadStatus, DownloadCategory>>()
                it.moveToFirst()
                while (!it.isAfterLast) {
                    map[
                        HlsVideoDownloadIdentifier.from(it.download.request.id)
                    ] = getDownloadStatus(it.download) to ArgumentWrapper
                        .decode(it.download.request.data).category
                    it.moveToNext()
                }
                map
            }
        )
    }

    private fun getDownloadStatus(download: Download?): DownloadStatus {
        if (download == null) {
            Log.w(
                TAG, "getDownloadStatus(): Download not found, default status is generated: " +
                    "${download?.request?.id}"
            )
            return DownloadStatus(null, null, DownloadStatus.State.DELETED, null)
        }

        val totalSize = download.contentLength.takeUnless { it <= 0 }
            ?: if (download.state == Download.STATE_COMPLETED) {
                download.bytesDownloaded
            } else {
                ArgumentWrapper.decode(download.request.data).estimatedSize
                    ?: download.bytesDownloaded * 100 / download.percentDownloaded
            }.toLong()
        val state = when (download.state) {
            Download.STATE_QUEUED, Download.STATE_RESTARTING, Download.STATE_REMOVING -> DownloadStatus.State.PENDING
            Download.STATE_DOWNLOADING -> DownloadStatus.State.RUNNING
            Download.STATE_COMPLETED -> DownloadStatus.State.DOWNLOADED
            else -> DownloadStatus.State.DELETED
        }
        val downloaded = if (state == DownloadStatus.State.DELETED) 0 else download.bytesDownloaded
        val error = if (download.state == Download.STATE_FAILED) {
            Exception("Download failed with reason ${download.failureReason}")
        } else null

        Log.d(
            TAG, "getDownloadStatus(): Generated download status [${state.name}]" +
                "${downloaded}/${totalSize} B (error: ${error}) for ${download.request.id}"
        )
        return DownloadStatus(totalSize, downloaded, state, error)
    }

    private fun notifyStatus(identifier: String) {
        Log.d(TAG, "Notifying of new status: $identifier")
        listeners[identifier]?.invoke(
            getDownloadStatus(downloads[identifier])
        ) ?: Log.w(TAG, "Listener is null: $identifier")
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

    internal data class ArgumentWrapper(
        val title: String,
        val showNotification: Boolean,
        val category: DownloadCategory,
        val estimatedSize: Long?
    ) {
        companion object {
            fun decode(data: ByteArray): ArgumentWrapper =
                Gson().fromJson(data.toString(Charsets.UTF_8), ArgumentWrapper::class.java)
        }

        fun encode(): ByteArray = Gson().toJson(this).toByteArray(Charsets.UTF_8)
    }
}
