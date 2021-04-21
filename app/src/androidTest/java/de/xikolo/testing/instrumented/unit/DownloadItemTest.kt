package de.xikolo.testing.instrumented.unit

import android.Manifest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import de.xikolo.controllers.main.MainActivity
import de.xikolo.download.DownloadIdentifier
import de.xikolo.download.DownloadItem
import de.xikolo.download.DownloadStatus
import de.xikolo.testing.instrumented.mocking.base.BaseTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

abstract class DownloadItemTest<T : DownloadItem<D, I>,
    D, I : DownloadIdentifier> : BaseTest() {

    @Rule
    @JvmField
    var activityTestRule =
        ActivityTestRule(MainActivity::class.java, false, true)

    @Rule
    @JvmField
    var permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    abstract val testDownloadItem: T
    abstract val testDownloadItemNotDownloadable: T

    @Before
    fun deleteAllItems() {
        fun deleteItem(item: DownloadItem<D, I>) {
            var deleted = false
            testDownloadItem.status.observeForever {
                if(it.state == DownloadStatus.State.DELETED) {
                    deleted = true
                }
            }
            item.delete(activityTestRule.activity)
            waitWhile({ !deleted }, 3000)
        }

        deleteItem(testDownloadItem)
        deleteItem(testDownloadItemNotDownloadable)
    }

    @Test
    fun testIdentifier() {
        testDownloadItem.identifier
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
        testDownloadItem.status
        testDownloadItemNotDownloadable.status
    }

    @Test
    fun testStatusBefore() {
        testDownloadItem.status.observe(activityTestRule.activity){
            assertNotNull(it)
            assertNull(testDownloadItem.download)
        }
    }

    @Test
    fun testDownloadAndDelete() {
        var downloaded = false
        testDownloadItem.status.observe(activityTestRule.activity){
            if(it.state == DownloadStatus.State.DOWNLOADED) {
                downloaded = true
            }
        }

        assertNull(testDownloadItem.download)

        var startResult = false
        testDownloadItem.start(activityTestRule.activity){
            startResult = it
        }

        waitWhile({ !startResult }, 3000)
        waitWhile({ !downloaded })
        assertNotNull(testDownloadItem.download)

        var deleted = false
        testDownloadItem.status.observe(activityTestRule.activity){
            if(it.state == DownloadStatus.State.DELETED) {
                deleted = true
            }
        }

        var deleteResult = false
        testDownloadItem.delete(activityTestRule.activity){
            deleteResult = it
        }

        waitWhile({ !deleteResult }, 3000)
        waitWhile({ !deleted })
        assertNull(testDownloadItem.download)
    }

    protected fun waitWhile(condition: () -> Boolean, timeout: Long = 300000) {
        var waited = 0
        while (condition()) {
            Thread.sleep(100)
            waited += 100
            if (waited > timeout) {
                throw Exception("Condition timeout")
            }
        }
    }
}
