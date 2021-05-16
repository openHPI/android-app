package de.xikolo.testing.instrumented.unit

import com.google.android.exoplayer2.source.MediaSource
import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.download.DownloadCategory
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadItem
import de.xikolo.utils.extensions.preferredStorage
import io.mockk.every

class HlsVideoDownloadItemTest : DownloadItemTest<HlsVideoDownloadItem,
    MediaSource, HlsVideoDownloadIdentifier>() {

    private val storage = context.preferredStorage

    override val testDownloadItem = HlsVideoDownloadItem(
        "https://open.hpi.de/playlists/93a84211-e40a-416a-b224-4d3ecdbb12f9.m3u8?embed_subtitles_for_video=d7e056da-756f-4437-b64a-16970a33d5ef",
        DownloadCategory.Other,
        VideoSettingsHelper.VideoQuality.HIGH.qualityFraction,
        storage
    )
    override val testDownloadItemNotDownloadable = HlsVideoDownloadItem(
        null,
        DownloadCategory.Other,
        0.0f,
        storage
    )

    init {
        every { testDownloadItem.downloader } returns HSLS().downloadHandler
        every { testDownloadItemNotDownloadable.downloader } returns HSLS().downloadHandler
    }
}
