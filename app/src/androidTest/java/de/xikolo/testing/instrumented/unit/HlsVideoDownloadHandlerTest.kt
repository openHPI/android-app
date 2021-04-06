package de.xikolo.testing.instrumented.unit

import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadStatus
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadRequest
import de.xikolo.testing.instrumented.mocking.SampleMockData
import de.xikolo.utils.extensions.preferredStorage
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HlsVideoDownloadHandlerTest : DownloadHandlerTest<HlsVideoDownloadHandler,
    HlsVideoDownloadIdentifier, HlsVideoDownloadRequest>() {

    override var downloadHandler = spyk(HlsVideoDownloadHandler, recordPrivateCalls = true) {
        every { this@spyk getProperty "context" } answers { context }
    }

    override var successfulTestRequest = HlsVideoDownloadRequest(
        SampleMockData.mockVideoStreamHlsUrl,
        VideoSettingsHelper.VideoQuality.LOW.percent,
        context.preferredStorage,
        "Video 1",
        true,
        DownloadCategory.Other
    )
    override var successfulTestRequest2 = HlsVideoDownloadRequest(
        SampleMockData.mockVideoStreamHlsUrl,
        VideoSettingsHelper.VideoQuality.BEST.percent,
        context.preferredStorage,
        "Video 2",
        true,
        DownloadCategory.Other
    )
    override var failingTestRequest = HlsVideoDownloadRequest(
        "https://www.example.com/notfoundfilehwqnqkdrzn42r.m3u8",
        VideoSettingsHelper.VideoQuality.BEST.percent,
        context.preferredStorage,
        "Failing Video",
        true,
        DownloadCategory.Other
    )

    @Test
    fun testDifferentQualities() {
        // download 1
        var status1: DownloadStatus? = null
        downloadHandler.listen(downloadHandler.identify(successfulTestRequest)) {
            status1 = it
        }
        downloadHandler.download(successfulTestRequest)

        // download 2
        var status2: DownloadStatus? = null
        downloadHandler.listen(downloadHandler.identify(successfulTestRequest2)) {
            status2 = it
        }
        downloadHandler.download(successfulTestRequest2)

        // wait for downloads to finish
        waitWhile({
            status1?.state?.equals(DownloadStatus.State.DOWNLOADED) == false &&
                status2?.state?.equals(DownloadStatus.State.DOWNLOADED) == false
        })

        assertNotNull(status1!!.totalBytes)
        assertNotNull(status2!!.totalBytes)
        assertNotNull(status1!!.downloadedBytes)
        assertNotNull(status2!!.downloadedBytes)
        assertNotEquals(status1!!.totalBytes, status2!!.totalBytes)
        assertNotEquals(status1!!.downloadedBytes, status2!!.downloadedBytes)
    }
}
