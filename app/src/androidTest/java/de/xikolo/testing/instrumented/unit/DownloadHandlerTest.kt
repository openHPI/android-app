package de.xikolo.testing.instrumented.unit

import android.Manifest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import de.xikolo.controllers.main.MainActivity
import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadHandler
import de.xikolo.download.DownloadIdentifier
import de.xikolo.download.DownloadRequest
import de.xikolo.download.DownloadStatus
import de.xikolo.testing.instrumented.mocking.base.BaseTest
import de.xikolo.utils.extensions.preferredStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

abstract class DownloadHandlerTest<T : DownloadHandler<I, R>,
    I : DownloadIdentifier, R : DownloadRequest> : BaseTest() {

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule(MainActivity::class.java, false, true)

    @Rule
    @JvmField
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    abstract var downloadHandler: T

    abstract var successfulTestRequest: R
    abstract var successfulTestRequest2: R
    abstract var failingTestRequest: R

    @Before
    fun deleteAllDownloads() {
        fun deleteDownload(request: R) {
            val identifier = downloadHandler.identify(request)
            var status: DownloadStatus? = null
            downloadHandler.listen(identifier) {
                status = it
            }
            downloadHandler.delete(identifier)
            waitWhile({ status?.state?.equals(DownloadStatus.State.DELETED) == false }, 3000)
            downloadHandler.listen(identifier, null)
        }

        deleteDownload(successfulTestRequest)
        deleteDownload(successfulTestRequest2)
        deleteDownload(failingTestRequest)
    }

    @Test
    fun testDownloadIdentification() {
        downloadHandler.identify(successfulTestRequest)
        downloadHandler.identify(successfulTestRequest2)
        downloadHandler.identify(failingTestRequest)
    }

    @Test
    fun testDownloadListenerRegistration() {
        val identifier = downloadHandler.identify(successfulTestRequest)

        var called = false
        // register listener
        downloadHandler.listen(identifier) {
            called = true
        }
        assertTrue(called)

        // reset `called` to false
        called = false
        // unregister listener
        downloadHandler.listen(identifier, null)
        // perform an action
        downloadHandler.download(successfulTestRequest)
        try {
            // wait for listener to set `called` to true, if this is the case then fail
            waitWhile({ !called }, 3000)
            fail("Unregistered listener has been invoked")
        } catch (e: Exception) {
            // unregistered listener has not been invoked, which is expected behavior
        }
    }

    @Test
    fun testDownloadStarting() {
        var result = false
        downloadHandler.download(successfulTestRequest) {
            result = it
        }
        // wait for `result` to become true
        waitWhile({ !result }, 3000)
    }

    @Test
    fun testDownloadStatusAfterStart() {
        val identifier = downloadHandler.identify(successfulTestRequest)

        // register listener
        var status: DownloadStatus? = null
        downloadHandler.listen(identifier) {
            status = it
        }
        // start download
        downloadHandler.download(successfulTestRequest)
        // wait for download to start
        waitWhile({
            status?.state?.equals(DownloadStatus.State.DELETED) == true ||
                (status?.state?.equals(DownloadStatus.State.PENDING) == false &&
                    status?.state?.equals(DownloadStatus.State.RUNNING) == false)
        }, 10000)
        assertNotNull(status!!.totalBytes)
        assertNotNull(status!!.downloadedBytes)
        if (status!!.totalBytes!! >= 0L) {
            assertTrue(
                status!!.downloadedBytes!! <= status!!.totalBytes!!
            )
        }
    }

    @Test
    fun testDownloadStatusAfterCancel() {
        val identifier = downloadHandler.identify(successfulTestRequest)

        // register listener
        var status: DownloadStatus? = null
        downloadHandler.listen(identifier) {
            status = it
        }

        var result = false
        // start download
        downloadHandler.download(successfulTestRequest) {
            // cancel running download and check status
            downloadHandler.delete(identifier) {
                result = true
            }
        }

        // wait for `result` to become true
        waitWhile({ !result }, 3000)
        waitWhile({ status!!.state != DownloadStatus.State.DELETED }, 3000)
    }

    @Test
    fun testDownloadStatusAfterSuccess() {
        val identifier = downloadHandler.identify(successfulTestRequest)

        var status: DownloadStatus? = null
        downloadHandler.listen(identifier) {
            status = it
        }
        // start download
        downloadHandler.download(successfulTestRequest)
        // wait for download to finish
        waitWhile({ status?.state?.equals(DownloadStatus.State.DOWNLOADED) == false })
        assertNotNull(status!!.totalBytes)
        assertNotNull(status!!.downloadedBytes)
        assertEquals(status!!.totalBytes, status!!.downloadedBytes)
    }

    @Test
    fun testDownloadStatusAfterDelete() {
        val identifier = downloadHandler.identify(successfulTestRequest)

        var status: DownloadStatus? = null
        downloadHandler.listen(identifier) {
            status = it
        }
        // start download
        downloadHandler.download(successfulTestRequest)
        // wait for download to finish
        waitWhile({ status?.state?.equals(DownloadStatus.State.DOWNLOADED) == false })

        var result = false
        downloadHandler.delete(identifier) {
            result = it
        }
        // wait for `result` to become true
        waitWhile({ !result }, 3000)
        waitWhile({ status!!.state != DownloadStatus.State.DELETED }, 3000)
    }

    @Test
    fun testDownloadStatusAfterFailure() {
        val identifier = downloadHandler.identify(failingTestRequest)

        var status: DownloadStatus? = null
        downloadHandler.listen(identifier) {
            status = it
        }
        // start download
        downloadHandler.download(successfulTestRequest)
        // wait for download to fail
        waitWhile({ status?.state?.equals(DownloadStatus.State.DELETED) == false })
    }

    @Test
    fun testIsDownloadingAnything() {
        var result = true
        downloadHandler.isDownloadingAnything {
            result = it
        }
        // assert the result is false
        waitWhile({ result }, 1000)

        // register listener
        var status: DownloadStatus? = null
        downloadHandler.listen(downloadHandler.identify(successfulTestRequest)) {
            status = it
        }
        downloadHandler.download(successfulTestRequest)
        waitWhile({
            status?.state?.equals(DownloadStatus.State.DELETED) == true ||
                (status?.state?.equals(DownloadStatus.State.PENDING) == false &&
                    status?.state?.equals(DownloadStatus.State.RUNNING) == false)
        }, 10000)

        result = false
        downloadHandler.isDownloadingAnything {
            result = it
        }
        // assert the result is true
        waitWhile({ !result }, 1000)
    }

    @Test
    fun testGettingDownloads() {
        var result: Map<I, Pair<DownloadStatus, DownloadCategory>>? = null
        downloadHandler.getDownloads(context.preferredStorage) {
            result = it
        }
        // wait for and check result
        waitWhile({ result?.size?.equals(0) == false }, 1000)

        var status: DownloadStatus? = null
        downloadHandler.listen(downloadHandler.identify(successfulTestRequest)) {
            status = it
        }
        // start download
        downloadHandler.download(successfulTestRequest)
        // wait for download to finish
        waitWhile({ status?.state?.equals(DownloadStatus.State.DOWNLOADED) == false })

        result = null
        downloadHandler.getDownloads(context.preferredStorage) {
            result = it
        }
        // wait for result
        waitWhile({ result?.size?.equals(1) == false }, 1000)
    }

    @Test
    fun testParallelDownloading() {
        var result = false
        downloadHandler.download(successfulTestRequest) {
            result = it
        }
        var result2 = false
        downloadHandler.download(successfulTestRequest2) {
            result2 = it
        }
        // wait for `result` and `result2` to become true
        waitWhile({ !result || !result2 }, 3000)
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
