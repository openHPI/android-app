package de.xikolo.download.hlsvideodownload

import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
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
    //val subtitles: Map<String, String>?,
    storage: Storage = App.instance.preferredStorage
) : DownloadItemImpl<MediaSource, HlsVideoDownloadIdentifier, HlsVideoDownloadRequest>(storage) {

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

    private fun getMediaSource(storage: Storage): MediaSource? {
        return getIndexEntry(storage)?.let {
            return HlsMediaSource.Factory(
                CacheDataSource.Factory()
                    .setCache(getCache(storage))
                    .setCacheWriteDataSinkFactory(null)
            ).createMediaSource(
                it.request.toMediaItem()
            )
        }
    }

    override val size: Long
        get() = getIndexEntry(storage)?.bytesDownloaded ?: 0L

    final override val download: MediaSource?
        get() = getMediaSource(context.internalStorage)
            ?: context.sdcardStorage?.let { getMediaSource(it) }

    /*val subs: Map<String, MediaSource>?
        get() {
            fun getMediaSource(language: String, url: String): MediaSource =
                SingleSampleMediaSource.Factory(
                    CacheDataSource.Factory()
                        .setCache(cache)
                )
                    .createMediaSource(
                        MediaItem.Subtitle(
                            Uri.parse(url),
                            MimeTypes.TEXT_VTT,
                            language,
                            C.SELECTION_FLAG_DEFAULT
                        ),
                        C.TIME_UNSET
                    )

            val originalStorage = storage
            return try {
                storage = context.internalStorage
                indexEntry!!
                subtitles?.entries?.associate {
                    it.key to getMediaSource(it.key, it.value)
                }
            } catch (e: Exception) {
                storage = context.sdcardStorage!!
                indexEntry!!
                subtitles?.entries?.associate {
                    it.key to getMediaSource(it.key, it.value)
                }
            } finally {
                storage = originalStorage
            }
        }*/

    final override val request
        get() = HlsVideoDownloadRequest(
            url!!,
            quality,
            //subtitles,
            storage,
            title,
            true,
            category
        )
}

