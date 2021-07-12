package de.xikolo.download.hlsvideodownload

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.MimeTypes
import de.xikolo.App
import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadItemImpl
import de.xikolo.models.Storage
import de.xikolo.utils.extensions.internalStorage
import de.xikolo.utils.extensions.preferredStorage
import de.xikolo.utils.extensions.sdcardStorage

open class HlsVideoDownloadItem(
    val url: String?,
    val category: DownloadCategory,
    val quality: Float,
    val subtitles: Map<String, String>,
    storage: Storage = App.instance.preferredStorage
) : DownloadItemImpl<Pair<HlsMediaSource, Map<String, SingleSampleMediaSource>>,
    HlsVideoDownloadIdentifier, HlsVideoDownloadRequest>(storage) {

    final override val downloader = HlsVideoDownloadHandler

    final override val downloadable: Boolean
        get() = url != null

    override val title: String
        get() = url ?: ""

    private fun getCache(storage: Storage): Cache {
        return if (storage == context.sdcardStorage) {
            HlsVideoDownloadHandler.getSdcardStorageCache(context)!!
        } else {
            HlsVideoDownloadHandler.getInternalStorageCache(context)
        }
    }

    private fun getIndexEntry(storage: Storage): Download? {
        return HlsVideoDownloadHandler.getManager(context, getCache(storage))
            .downloadIndex
            .getDownload(
                identifier.get()
            )
    }

    private fun getMediaSource(storage: Storage): Pair<HlsMediaSource, Map<String, SingleSampleMediaSource>>? {
        return getIndexEntry(storage)?.let { indexEntry ->
            HlsMediaSource.Factory(
                CacheDataSource.Factory()
                    .setCache(getCache(storage))
                    .setCacheWriteDataSinkFactory(null)
            ).createMediaSource(
                indexEntry.request.toMediaItem()
            ) to subtitles.mapValues { (language, url) ->
                SingleSampleMediaSource.Factory(
                    CacheDataSource.Factory()
                        .setCache(getCache(storage))
                        .setCacheWriteDataSinkFactory(null)
                ).createMediaSource(
                    MediaItem.Subtitle(
                        Uri.parse(url), // requires that the HLS playlist links to the API resource
                        MimeTypes.TEXT_VTT,
                        language,
                        C.SELECTION_FLAG_DEFAULT
                    ),
                    C.TIME_UNSET
                )
            }
        }
    }

    override val size: Long
        get() = getIndexEntry(storage)?.bytesDownloaded ?: 0L

    final override val download: Pair<HlsMediaSource, Map<String, SingleSampleMediaSource>>?
        get() = getMediaSource(context.internalStorage)
            ?: context.sdcardStorage?.let { getMediaSource(it) }

    final override val request
        get() = HlsVideoDownloadRequest(
            url!!,
            quality,
            storage,
            title,
            true,
            category
        )
}

