package de.xikolo.download.hlsvideodownload.services

import android.app.Notification
import android.app.PendingIntent
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.google.android.exoplayer2.upstream.cache.Cache
import de.xikolo.R
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadRequest
import de.xikolo.utils.NotificationUtil

abstract class HlsVideoDownloadForegroundService : DownloadService(
    NotificationUtil.DOWNLOAD_RUNNING_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    NotificationUtil.DOWNLOADS_CHANNEL_ID,
    R.string.notification_channel_downloads,
    0
) {

    abstract val cache: Cache

    override fun getDownloadManager(): DownloadManager {
        return HlsVideoDownloadHandler.getManager(applicationContext, cache)
    }

    override fun getForegroundNotification(downloads: List<Download>): Notification {
        val notificationUtil = NotificationUtil(applicationContext)
        val notifications = downloads.mapNotNull {
            val args = HlsVideoDownloadRequest.ArgumentWrapper.decode(it.request.data)
            if (args.showNotification && it.state != Download.STATE_REMOVING) {
                args.hashCode() to notificationUtil.updateDownloadRunningNotification(
                    notificationUtil.getDownloadRunningNotification(),
                    args.title,
                    it.percentDownloaded.toInt(),
                    PendingIntent.getService(
                        this,
                        0,
                        buildRemoveDownloadIntent(
                            applicationContext,
                            HlsVideoDownloadForegroundService::class.java,
                            it.request.id,
                            true
                        ),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                ).build()
            } else null
        }

        notifications.forEach {
            notificationUtil.notify(it.first, it.second)
        }
        return notificationUtil.getDownloadRunningGroupNotification(downloads.size)
    }

    override fun getScheduler(): PlatformScheduler? {
        return null
    }
}
