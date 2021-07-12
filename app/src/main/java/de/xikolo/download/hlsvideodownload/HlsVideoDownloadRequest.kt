package de.xikolo.download.hlsvideodownload

import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadRequest
import de.xikolo.models.Storage

/**
 * DownloadRequest class for HLS video downloads.
 * Video, audio and subtitle tracks are downloaded.
 *
 * @param url The HLS master playlist URL.
 * @param quality The desired percentage of the maximum available bitrate of the video as in
 * [VideoSettingsHelper.VideoQuality.qualityFraction].
 * This does not necessarily need to correspond to the bitrates in the master playlist.
 * Based on this parameter, the track with the closest calculated target bitrate in the master
 * playlist is selected.
 * @param storage The storage location for the download.
 * @param title The title of the download.
 * @param showNotification Whether to show a notification while downloading.
 * @param category The download category.
 */
class HlsVideoDownloadRequest(
    val url: String,
    val quality: Float,
    val storage: Storage,
    override val title: String,
    override val showNotification: Boolean,
    override val category: DownloadCategory
) : DownloadRequest
