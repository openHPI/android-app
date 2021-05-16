package de.xikolo.testing.instrumented.unit

import androidx.test.annotation.UiThreadTest
import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadStatus
import de.xikolo.download.filedownload.FileDownloadHandler
import de.xikolo.download.filedownload.FileDownloadIdentifier
import de.xikolo.download.filedownload.FileDownloadItem
import de.xikolo.utils.extensions.preferredStorage
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileDownloadItemTest : DownloadItemTest<FileDownloadItem,
    File, FileDownloadIdentifier>() {

    private val storage = context.preferredStorage

    override val testDownloadItem = spyk(
        FileDownloadItem(
            "https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_1280_10MG.mp4",
            DownloadCategory.Other,
            "video.mp4",
            storage
        )
    )
    override val testDownloadItemNotDownloadable = spyk(
        FileDownloadItem(
            null,
            DownloadCategory.Other,
            "null.null"
        )
    )

    init {
        every { testDownloadItem.downloader } returns spyk(FileDownloadHandler, recordPrivateCalls = true) {
            every { this@spyk getProperty "context" } answers { context }
        }
        every { testDownloadItemNotDownloadable.downloader } returns FileDownloadHandlerTest().downloadHandler
    }

    @Test
    @UiThreadTest
    fun testFileName() {
        testDownloadItem.fileName
        testDownloadItemNotDownloadable.fileName
    }

    @Test
    fun testFileHandling() {
        var downloaded = false
        var downloadCallbackCalled = false
        onUi {
            testDownloadItem.status.observe(activityTestRule.activity) {
                if (it.state == DownloadStatus.State.DOWNLOADED) {
                    downloaded = true
                }
            }

            testDownloadItem.start(activityTestRule.activity) {
                downloadCallbackCalled = true
            }
        }

        // wait for download to start
        waitWhile({ !downloadCallbackCalled }, 10000)

        assertFalse(
            File(testDownloadItem.filePath).exists()
        )
        assertTrue(
            File(testDownloadItem.filePath + ".tmp").exists()
        )

        // wait for completion
        waitWhile({ !downloaded })

        assertTrue(
            File(testDownloadItem.filePath).exists()
        )
        assertFalse(
            File(testDownloadItem.filePath + ".tmp").exists()
        )
        assertTrue(
            testDownloadItem.download?.exists() == true
        )

        var deleted = false
        onUi {
            testDownloadItem.status.observe(activityTestRule.activity) {
                if (it.state == DownloadStatus.State.DELETED) {
                    deleted = true
                }
            }

            testDownloadItem.delete(activityTestRule.activity)
        }

        waitWhile({ !deleted }, 3000)

        assertFalse(
            File(testDownloadItem.filePath).exists()
        )
        assertFalse(
            testDownloadItem.download?.exists() == true
        )
    }
}
