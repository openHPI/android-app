package de.xikolo.download.hlsvideodownload

import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist
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
 * // @param subtitles The subtitles to download along the video.
 * @param storage The storage location for the download.
 * @param title The title of the download.
 * @param showNotification Whether to show a notification while downloading.
 * @param category The download category.
 */
class HlsVideoDownloadRequest(
    val url: String,
    val quality: Float,
    //val subtitles: Map<String, String>?,
    val storage: Storage,
    override val title: String,
    override val showNotification: Boolean,
    override val category: DownloadCategory
) : DownloadRequest {

    val mediaItem = MediaItem.Builder()
        .setUri(Uri.parse(url))
        /*.setSubtitles(
            subtitles?.map {
                MediaItem.Subtitle(
                    Uri.parse(it.value),
                    MimeTypes.TEXT_VTT,
                    it.key,
                    C.SELECTION_FLAG_DEFAULT
                )
            }
        )*/
        .setStreamKeys(
            listOf(
                StreamKey(HlsMasterPlaylist.GROUP_INDEX_VARIANT, 1),
                StreamKey(HlsMasterPlaylist.GROUP_INDEX_AUDIO, 1),
                StreamKey(HlsMasterPlaylist.GROUP_INDEX_SUBTITLE, 1)
            )
        )
        .build()
}
