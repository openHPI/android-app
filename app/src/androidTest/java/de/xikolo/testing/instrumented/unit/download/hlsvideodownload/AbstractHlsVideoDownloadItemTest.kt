package de.xikolo.testing.instrumented.unit.download.hlsvideodownload

import com.google.android.exoplayer2.source.MediaSource
import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.download.DownloadCategory
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadItem
import de.xikolo.models.Storage
import de.xikolo.testing.instrumented.unit.download.DownloadItemTest

abstract class AbstractHlsVideoDownloadItemTest : DownloadItemTest<HlsVideoDownloadItem,
    MediaSource, HlsVideoDownloadIdentifier>() {

    abstract val storage: Storage

    override val testDownloadItem
        get() = HlsVideoDownloadItem(
            "https://open.hpi.de/playlists/93a84211-e40a-416a-b224-4d3ecdbb12f9.m3u8?embed_subtitles_for_video=d7e056da-756f-4437-b64a-16970a33d5ef",
            DownloadCategory.Other,
            VideoSettingsHelper.VideoQuality.HIGH.qualityFraction,
            storage
        )

    override val testDownloadItemNotDownloadable
        get() = HlsVideoDownloadItem(
            null,
            DownloadCategory.Other,
            0.0f,
            storage
        )
}
