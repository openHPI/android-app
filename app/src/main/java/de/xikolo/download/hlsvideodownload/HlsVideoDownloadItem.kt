package de.xikolo.download.hlsvideodownload

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.MimeTypes
import de.xikolo.App
import de.xikolo.download.DownloadItemImpl
import de.xikolo.models.Storage
import de.xikolo.utils.extensions.internalStorage
import de.xikolo.utils.extensions.preferredStorage
import de.xikolo.utils.extensions.sdcardStorage

open class HlsVideoDownloadItem(
    val url: String?,
    val category: String?,
    val quality: Int,
    val subtitles: Map<String, String>?,
    storage: Storage = App.instance.preferredStorage
) : DownloadItemImpl<MediaSource, HlsVideoDownloadIdentifier, HlsVideoDownloadRequest>(storage) {

    final override val downloader = HlsVideoDownloadHandler

    final override val downloadable: Boolean
        get() = url != null

    override val title: String
        get() = url!!

    private val cache: Cache
        get() = if (storage == context.sdcardStorage) {
            HlsVideoDownloadHandler.getSdcardStorageCache(context)!!
        } else {
            HlsVideoDownloadHandler.getInternalStorageCache(context)
        }

    private val indexEntry: Download?
        get() = HlsVideoDownloadHandler.getManager(context, cache)
            .downloadIndex
            .getDownload(identifier.get())

    private val mediaSource: MediaSource
        get() = HlsMediaSource.Factory(
            CacheDataSource.Factory()
                .setCache(cache)
        ).createMediaSource(request.mediaItem)

    override val size: Long
        get() = indexEntry?.bytesDownloaded ?: 0L

    final override val download: MediaSource?
        get() {
            val originalStorage = storage
            return try {
                storage = context.internalStorage
                indexEntry!!
                mediaSource
            } catch (e: Exception) {
                try {
                    storage = context.sdcardStorage!!
                    indexEntry!!
                    mediaSource
                } catch (e: Exception) {
                    null
                }
            } finally {
                storage = originalStorage
            }
        }

    val subs: Map<String, MediaSource>?
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
        }

    final override val request
        get() = HlsVideoDownloadRequest(
            url!!,
            quality,
            subtitles,
            identifier,
            storage,
            title,
            true,
            category
        )

    final override val itemIdentifier: HlsVideoDownloadIdentifier
        get() = HlsVideoDownloadIdentifier(url!!, quality)
}

