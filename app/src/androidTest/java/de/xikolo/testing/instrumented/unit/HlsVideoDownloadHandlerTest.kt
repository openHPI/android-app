package de.xikolo.testing.instrumented.unit

import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.download.DownloadCategory
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadRequest
import de.xikolo.utils.extensions.preferredStorage
import io.mockk.every
import io.mockk.spyk

class HlsVideoDownloadHandlerTest : DownloadHandlerTest<HlsVideoDownloadHandler,
    HlsVideoDownloadIdentifier, HlsVideoDownloadRequest>() {

    override var downloadHandler = spyk(HlsVideoDownloadHandler, recordPrivateCalls = true) {
        every { this@spyk getProperty "context" } answers { context }
    }

    override var successfulTestRequest = HlsVideoDownloadRequest(
        "https://open.hpi.de/playlists/93a84211-e40a-416a-b224-4d3ecdbb12f9.m3u8?embed_subtitles_for_video=d7e056da-756f-4437-b64a-16970a33d5ef",
        VideoSettingsHelper.VideoQuality.LOW.qualityFraction,
        context.preferredStorage,
        "Video 1",
        true,
        DownloadCategory.Other
    )
    override var successfulTestRequest2 = HlsVideoDownloadRequest(
        "https://open.hpi.de/playlists/04012fde-be48-47b6-a742-0edc69a9c2a9.m3u8?embed_subtitles_for_video=d7e056da-756f-4437-b64a-16970a33d5ef",
        VideoSettingsHelper.VideoQuality.BEST.qualityFraction,
        context.preferredStorage,
        "Video 2",
        true,
        DownloadCategory.Other
    )
    override var failingTestRequest = HlsVideoDownloadRequest(
        "https://www.example.com/notfoundfilehwqnqkdrzn42r.m3u8",
        VideoSettingsHelper.VideoQuality.BEST.qualityFraction,
        context.preferredStorage,
        "Failing Video",
        true,
        DownloadCategory.Other
    )
}
