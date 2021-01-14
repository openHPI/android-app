package de.xikolo.download.hlsvideodownload.services

import com.google.android.exoplayer2.upstream.cache.Cache
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler

class HlsVideoDownloadInternalStorageBackgroundService : HlsVideoDownloadBackgroundService() {

    override val cache: Cache
        get() = HlsVideoDownloadHandler.getInternalStorageCache(applicationContext)
}
