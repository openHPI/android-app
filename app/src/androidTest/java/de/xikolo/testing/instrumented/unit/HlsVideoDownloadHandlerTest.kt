package de.xikolo.testing.instrumented.unit

import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.download.DownloadCategory
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadRequest
import de.xikolo.testing.instrumented.mocking.SampleMockData
import de.xikolo.utils.extensions.preferredStorage
import io.mockk.every
import io.mockk.spyk

class HlsVideoDownloadHandlerTest : DownloadHandlerTest<HlsVideoDownloadHandler,
    HlsVideoDownloadIdentifier, HlsVideoDownloadRequest>() {

    override var downloadHandler = spyk(HlsVideoDownloadHandler, recordPrivateCalls = true) {
        every { this@spyk getProperty "context" } answers { context }
    }

    override var successfulTestRequest = HlsVideoDownloadRequest(
        SampleMockData.mockVideoStreamHlsUrl,
        VideoSettingsHelper.VideoQuality.LOW.qualityFraction,
        context.preferredStorage,
        "Video 1",
        true,
        DownloadCategory.Other
    )
    override var successfulTestRequest2 = HlsVideoDownloadRequest(
        SampleMockData.mockVideoStreamHlsUrl,
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
