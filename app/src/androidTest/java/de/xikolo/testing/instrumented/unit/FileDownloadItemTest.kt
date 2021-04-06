package de.xikolo.testing.instrumented.unit

import de.xikolo.download.filedownload.FileDownloadIdentifier
import de.xikolo.download.filedownload.FileDownloadItem
import java.io.File

class FileDownloadItemTest : DownloadItemTest<FileDownloadItem,
    File, FileDownloadIdentifier>() {

    /*private val storage = context.preferredStorage

    override var testDownloadItem =
        FileDownloadItem(SampleMockData.mockVideoStreamSdUrl, "sdvideo.mp4", storage)
    override var testDownloadItemNotDownloadable = FileDownloadItem(null, "null")

    private var testSecondaryItem = FileDownloadItem(
        SampleMockData.mockVideoStreamThumbnailUrl,
        "thumb.jpg",
        storage
    )
    private var testDownloadItemWithSecondary =
        object : FileDownloadItem(
            SampleMockData.mockVideoStreamSdUrl,
            "sdvideo2.mp4",
            storage
        ) {
            override val secondaryDownloadItems: Set<FileDownloadItem> = setOf(testSecondaryItem)
        }
    private var testDownloadItemWithSecondaryNotDeletingSecondary =
        object : FileDownloadItem(
            SampleMockData.mockVideoStreamSdUrl,
            "sdvideo3.mp4",
            storage
        ) {
            override val secondaryDownloadItems: Set<FileDownloadItem> = setOf(testSecondaryItem)
            override val deleteSecondaryDownloadItemPredicate: (FileDownloadItem) -> Boolean =
                { false }
        }

    @Before
    fun deleteSecondaryItems() {
        testDownloadItemWithSecondary.delete(activityTestRule.activity)

        testSecondaryItem.delete(activityTestRule.activity)
    }

    @Test
    fun testDeletesTempFile() {
        var completedMain = false
        testDownloadItem.stateListener = object : DownloadItem.StateListener {
            override fun onStarted() {}

            override fun onCompleted() {
                completedMain = true
            }

            override fun onDeleted() {}
        }
        testDownloadItem.start(activityTestRule.activity) {
            assertTrue(
                File(testDownloadItem.filePath + ".tmp").exists()
            )

            waitWhile({ !completedMain })

            assertFalse(
                File(testDownloadItem.filePath + ".tmp").exists()
            )

            testDownloadItem.delete(activityTestRule.activity) {
                testDownloadItem.start(activityTestRule.activity) {
                    assertTrue(
                        File(testDownloadItem.filePath + ".tmp").exists()
                    )

                    testDownloadItem.cancel(activityTestRule.activity) {
                        assertFalse(
                            File(testDownloadItem.filePath + ".tmp").exists()
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testStartDownloadWithSecondary() {
        var completedMain = false
        testDownloadItemWithSecondary.stateListener = object : DownloadItem.StateListener {
            override fun onStarted() {}

            override fun onCompleted() {
                completedMain = true
            }

            override fun onDeleted() {}
        }
        var completedSecondary = false
        testSecondaryItem.stateListener = object : DownloadItem.StateListener {
            override fun onStarted() {}

            override fun onCompleted() {
                completedSecondary = true
            }

            override fun onDeleted() {}
        }

        testDownloadItemWithSecondary.start(activityTestRule.activity) {
            assertNotNull(it)

            testDownloadItemWithSecondary.isDownloadRunning {
                assertTrue(it)

                testSecondaryItem.isDownloadRunning {
                    assertTrue(it)
                }

                testDownloadItemWithSecondary.start(activityTestRule.activity) {
                    assertNull(it)

                    testSecondaryItem.start(activityTestRule.activity) {
                        assertNull(it)

                        waitWhile({ !completedMain })

                        assertTrue(completedSecondary)
                        assertTrue(testDownloadItemWithSecondary.downloadExists)
                        assertTrue(testSecondaryItem.downloadExists)
                        assertNotNull(testDownloadItemWithSecondary.download)
                        assertNotNull(testSecondaryItem.download)

                        testDownloadItemWithSecondary.start(activityTestRule.activity) {
                            assertNull(it)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testCancelDownloadWithSecondary() {
        testDownloadItemWithSecondary.start(activityTestRule.activity) {
            assertNotNull(it)

            testDownloadItemWithSecondary.cancel(activityTestRule.activity) {
                assertTrue(it)

                testDownloadItemWithSecondary.isDownloadRunning {
                    assertFalse(it)

                    assertFalse(testDownloadItemWithSecondary.downloadExists)
                    assertNull(testDownloadItemWithSecondary.download)

                    testSecondaryItem.isDownloadRunning {
                        assertFalse(it)

                        assertFalse(testSecondaryItem.downloadExists)
                        assertNull(testSecondaryItem.download)
                    }
                }
            }
        }
    }

    @Test
    fun testDeleteDownloadWithSecondary() {
        var completedMain = false
        var deletedMain = false
        testDownloadItemWithSecondary.stateListener = object : DownloadItem.StateListener {
            override fun onStarted() {}

            override fun onCompleted() {
                completedMain = true
            }

            override fun onDeleted() {
                deletedMain = true
            }
        }

        var deletedSecondary = false
        testSecondaryItem.stateListener = object : DownloadItem.StateListener {
            override fun onStarted() {}

            override fun onCompleted() {}

            override fun onDeleted() {
                deletedSecondary = true
            }
        }

        testDownloadItemWithSecondary.delete(activityTestRule.activity) {
            assertFalse(it)

            testDownloadItemWithSecondary.start(activityTestRule.activity) {
                waitWhile({ !completedMain })

                assertTrue(testDownloadItemWithSecondary.downloadExists)
                assertTrue(testSecondaryItem.downloadExists)

                testDownloadItemWithSecondary.delete(activityTestRule.activity) {
                    assertTrue(it)

                    assertTrue(deletedMain)
                    assertTrue(deletedSecondary)
                    assertFalse(testDownloadItemWithSecondary.downloadExists)
                    assertFalse(testSecondaryItem.downloadExists)
                    assertNull(testDownloadItemWithSecondary.download)
                    assertNull(testSecondaryItem.download)
                }
            }
        }
    }

    @Test
    fun testDeleteDownloadWithoutSecondary() {
        var completedMain = false
        var deletedMain = false
        testDownloadItemWithSecondaryNotDeletingSecondary.stateListener =
            object : DownloadItem.StateListener {
                override fun onStarted() {}

                override fun onCompleted() {
                    completedMain = true
                }

                override fun onDeleted() {
                    deletedMain = true
                }
            }

        var deletedSecondary = false
        testSecondaryItem.stateListener =
            object : DownloadItem.StateListener {
                override fun onStarted() {}

                override fun onCompleted() {}

                override fun onDeleted() {
                    deletedSecondary = true
                }
            }

        testDownloadItemWithSecondaryNotDeletingSecondary.start(activityTestRule.activity) {
            assertNotNull(it)
            waitWhile({ !completedMain })

            testDownloadItemWithSecondaryNotDeletingSecondary.delete(activityTestRule.activity) {
                assertTrue(it)

                assertTrue(deletedMain)
                assertFalse(deletedSecondary)
                assertFalse(testDownloadItemWithSecondaryNotDeletingSecondary.downloadExists)
                assertTrue(testSecondaryItem.downloadExists)
                assertNull(testDownloadItemWithSecondaryNotDeletingSecondary.download)
                assertNotNull(testSecondaryItem.download)
            }
        }
    }*/
}
