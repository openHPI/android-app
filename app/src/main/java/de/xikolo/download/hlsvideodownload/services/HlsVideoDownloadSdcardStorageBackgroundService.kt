package de.xikolo.download.hlsvideodownload.services

import com.google.android.exoplayer2.upstream.cache.Cache
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler

class HlsVideoDownloadSdcardStorageBackgroundService : HlsVideoDownloadBackgroundService() {

    override val cache: Cache
        get() = HlsVideoDownloadHandler.getSdcardStorageCache(applicationContext)!!
}
