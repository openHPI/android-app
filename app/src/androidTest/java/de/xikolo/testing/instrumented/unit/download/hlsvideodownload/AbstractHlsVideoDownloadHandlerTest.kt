package de.xikolo.testing.instrumented.unit.download.hlsvideodownload

import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadStatus
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadHandler
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadRequest
import de.xikolo.testing.instrumented.unit.download.DownloadHandlerTest
import de.xikolo.utils.extensions.preferredStorage
import org.junit.Assert
import org.junit.Test

abstract class AbstractHlsVideoDownloadHandlerTest : DownloadHandlerTest<HlsVideoDownloadHandler,
    HlsVideoDownloadIdentifier, HlsVideoDownloadRequest>() {

    override val downloadHandler = HlsVideoDownloadHandler

    override val successfulTestRequest
        get() = HlsVideoDownloadRequest(
            "https://open.hpi.de/playlists/93a84211-e40a-416a-b224-4d3ecdbb12f9.m3u8?embed_subtitles_for_video=d7e056da-756f-4437-b64a-16970a33d5ef",
            VideoSettingsHelper.VideoQuality.LOW.qualityFraction,
            context.preferredStorage,
            "Video 1",
            true,
            DownloadCategory.Other
        )
    override val successfulTestRequest2 = HlsVideoDownloadRequest(
        "https://open.hpi.de/playlists/04012fde-be48-47b6-a742-0edc69a9c2a9.m3u8?embed_subtitles_for_video=d7e056da-756f-4437-b64a-16970a33d5ef",
        VideoSettingsHelper.VideoQuality.BEST.qualityFraction,
        context.preferredStorage,
        "Video 2",
        true,
        DownloadCategory.Other
    )
    override val failingTestRequest = HlsVideoDownloadRequest(
        "https://www.example.com/notfoundfilehwqnqkdrzn42r.m3u8",
        VideoSettingsHelper.VideoQuality.BEST.qualityFraction,
        context.preferredStorage,
        "Failing Video",
        true,
        DownloadCategory.Other
    )

    @Test
    fun testQualitySelection() {
        val identifier1 = downloadHandler.identify(successfulTestRequest)
        val identifier2 = downloadHandler.identify(successfulTestRequest2)

        Assert.assertNotEquals(identifier1, identifier2)

        var status1: DownloadStatus? = null
        downloadHandler.listen(identifier1) {
            status1 = it
        }
        var status2: DownloadStatus? = null
        downloadHandler.listen(identifier2) {
            status2 = it
        }

        // start downloads
        downloadHandler.download(successfulTestRequest)
        downloadHandler.download(successfulTestRequest2)

        // wait for download to finish
        waitWhile({
            status1?.state?.equals(DownloadStatus.State.DOWNLOADED) != true ||
                status2?.state?.equals(DownloadStatus.State.DOWNLOADED) != true
        })

        // test status after end
        Assert.assertNotEquals(
            status1?.downloadedBytes,
            status2?.downloadedBytes
        )

        Assert.assertNotEquals(
            status1?.totalBytes,
            status2?.totalBytes
        )
    }
}
