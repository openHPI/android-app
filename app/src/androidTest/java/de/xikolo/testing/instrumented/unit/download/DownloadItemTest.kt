package de.xikolo.testing.instrumented.unit.download

import de.xikolo.download.DownloadIdentifier
import de.xikolo.download.DownloadItem
import de.xikolo.download.DownloadStatus
import de.xikolo.extensions.observe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

abstract class DownloadItemTest<T : DownloadItem<D, I>,
    D, I : DownloadIdentifier> : BaseDownloadTest() {

    abstract val testDownloadItem: T
    abstract val testDownloadItemNotDownloadable: T

    @Before
    fun deleteAllItems() {
        fun deleteItem(item: DownloadItem<D, I>) {
            var deleted = false
            onUiThread {
                item.status.observe(activityTestRule.activity) {
                    if (it.state == DownloadStatus.State.DELETED) {
                        deleted = true
                    }
                }
                item.delete(activityTestRule.activity)
            }

            waitWhile({ !deleted }, 10000)
        }

        deleteItem(testDownloadItem)
    }

    @Test
    fun testIdentifier() {
        testDownloadItem.identifier
        try {
            testDownloadItemNotDownloadable.identifier
            fail("Statement should fail")
        } catch (e: Exception) {
            // expected behavior
        }
    }

    @Test
    fun testDownload() {
        testDownloadItem.download
        assertNull(testDownloadItemNotDownloadable.download)
    }

    @Test
    fun testDownloadable() {
        assertTrue(testDownloadItem.downloadable)
        assertFalse(testDownloadItemNotDownloadable.downloadable)
    }

    @Test
    fun testTitle() {
        testDownloadItem.title
        testDownloadItemNotDownloadable.title
    }

    @Test
    fun testOpenAction() {
        testDownloadItem.openAction
        testDownloadItemNotDownloadable.openAction
    }

    @Test
    fun testSize() {
        testDownloadItem.size
        testDownloadItemNotDownloadable.size
    }

    @Test
    fun testStatus() {
        var called = false
        onUiThread {
            testDownloadItem.status
            called = true
        }

        waitWhile({ !called }, 1000)

        try {
            testDownloadItemNotDownloadable.status
            fail("Statement should fail")
        } catch (e: Exception) {
            // expected behavior
        }
    }

    @Test
    fun testStatusBefore() {
        var called = false
        onUiThread {
            testDownloadItem.status.observe(activityTestRule.activity) {
                assertNotNull(it)
                assertNull(testDownloadItem.download)
                called = true
            }
        }

        waitWhile({ !called }, 1000)
    }

    @Test
    fun testDownloadAndDelete() {
        var downloaded = false
        onUiThread {
            testDownloadItem.status.observe(activityTestRule.activity) {
                if (it.state == DownloadStatus.State.DOWNLOADED) {
                    downloaded = true
                }
            }
        }

        assertNull(testDownloadItem.download)

        var startResult = false
        onUiThread {
            testDownloadItem.start(activityTestRule.activity) {
                startResult = it
            }
        }

        waitWhile({ !startResult }, 3000)
        waitWhile({ !downloaded })
        assertNotNull(testDownloadItem.download)

        var deleted = false
        onUiThread {
            testDownloadItem.status.observe(activityTestRule.activity) {
                if (it.state == DownloadStatus.State.DELETED) {
                    deleted = true
                }
            }
        }

        var deleteResult = false
        onUiThread {
            testDownloadItem.delete(activityTestRule.activity) {
                deleteResult = it
            }
        }

        waitWhile({ !deleteResult }, 3000)
        waitWhile({ !deleted })
        assertNull(testDownloadItem.download)
    }

    @Test
    fun testDownloadStartForNotDownloadable() {
        var startResult = true
        onUiThread {
            testDownloadItemNotDownloadable.start(activityTestRule.activity) {
                startResult = it
            }
        }

        waitWhile({ startResult }, 3000)
    }

    protected fun waitWhile(condition: () -> Boolean, timeout: Long = 60000) {
        var waited = 0
        while (condition()) {
            Thread.sleep(100)
            waited += 100
            if (waited > timeout) {
                throw Exception("Condition timeout")
            }
        }
    }

    protected fun onUiThread(block: Runnable) {
        activityTestRule.activity.runOnUiThread(block)
    }
}
