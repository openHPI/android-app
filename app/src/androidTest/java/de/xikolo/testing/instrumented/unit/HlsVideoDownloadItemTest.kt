package de.xikolo.testing.instrumented.unit

import com.google.android.exoplayer2.source.MediaSource
import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadStatus
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadItem
import de.xikolo.testing.instrumented.mocking.SampleMockData
import de.xikolo.utils.extensions.preferredStorage
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class HlsVideoDownloadItemTest : DownloadItemTest<HlsVideoDownloadItem,
    MediaSource, HlsVideoDownloadIdentifier>() {

    private val storage = context.preferredStorage

    override val testDownloadItem = HlsVideoDownloadItem(
        SampleMockData.mockVideoStreamHlsUrl,
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
    private val testDownloadItemOtherQuality = HlsVideoDownloadItem(
        SampleMockData.mockVideoStreamHlsUrl,
        DownloadCategory.Other,
        VideoSettingsHelper.VideoQuality.LOW.qualityFraction,
        storage
    )

    @Before
    fun deleteItem() {
        var deleted = false
        testDownloadItemOtherQuality.status.observe(activityTestRule.activity){
            if(it.state == DownloadStatus.State.DELETED) {
                deleted = true
            }
        }
        testDownloadItemOtherQuality.delete(activityTestRule.activity)
        waitWhile({ !deleted }, 3000)
    }

    @Test
    fun testQualitySelection() {
        assertNotEquals(testDownloadItem.identifier, testDownloadItemOtherQuality.identifier)

        var downloaded = false
        testDownloadItem.status.observe(activityTestRule.activity){
            if(it.state == DownloadStatus.State.DOWNLOADED){
                downloaded = true
            }
        }
        var downloadedOther = false
        testDownloadItemOtherQuality.status.observe(activityTestRule.activity){
            if(it.state == DownloadStatus.State.DOWNLOADED){
                downloadedOther = true
            }
        }

        testDownloadItem.start(activityTestRule.activity)
        testDownloadItemOtherQuality.start(activityTestRule.activity)

        waitWhile({!downloaded && !downloadedOther})

        assertNotEquals(
            testDownloadItem.size,
            testDownloadItemOtherQuality.size
        )

        assertNotEquals(
            testDownloadItem.status.totalBytes,
            testDownloadItemOtherQuality.status.totalBytes
        )
    }
}
