package de.xikolo.testing.instrumented.unit

import com.google.android.exoplayer2.source.MediaSource
import de.xikolo.controllers.helper.VideoSettingsHelper
import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadStatus
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadIdentifier
import de.xikolo.download.hlsvideodownload.HlsVideoDownloadItem
import de.xikolo.utils.extensions.preferredStorage
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

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
    private val testDownloadItemOtherQuality = HlsVideoDownloadItem(
        "https://open.hpi.de/playlists/93a84211-e40a-416a-b224-4d3ecdbb12f9.m3u8?embed_subtitles_for_video=d7e056da-756f-4437-b64a-16970a33d5ef",
        DownloadCategory.Other,
        VideoSettingsHelper.VideoQuality.LOW.qualityFraction,
        storage
    )

    @Before
    fun deleteItem() {
        var deleted = false
        activityTestRule.activity.runOnUiThread {
            testDownloadItemOtherQuality.status.observe(activityTestRule.activity) {
                if (it.state == DownloadStatus.State.DELETED) {
                    deleted = true
                }
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
