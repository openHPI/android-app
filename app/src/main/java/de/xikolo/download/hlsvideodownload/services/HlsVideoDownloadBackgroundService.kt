package de.xikolo.download.hlsvideodownload.services

import android.app.Notification
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.google.android.exoplayer2.upstream.cache.Cache
import de.xikolo.App
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler

abstract class HlsVideoDownloadBackgroundService : DownloadService(
    FOREGROUND_NOTIFICATION_ID_NONE,
    0,
    null,
    0,
    0
) {

    abstract val cache: Cache

    override fun getDownloadManager(): DownloadManager {
        return HlsVideoDownloadHandler.getManager(applicationContext, cache)
    }

    override fun getForegroundNotification(downloads: MutableList<Download>): Notification {
        return NotificationCompat.Builder(App.instance, "").build()
    }

    override fun getScheduler(): PlatformScheduler? {
        return null
    }
}
