package de.xikolo.testing.instrumented.unit.download

import de.xikolo.download.DownloadHandler
import de.xikolo.download.DownloadIdentifier
import de.xikolo.download.DownloadRequest
import de.xikolo.download.DownloadStatus
import de.xikolo.models.Storage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

abstract class DownloadHandlerTest<T : DownloadHandler<I, R>,
    I : DownloadIdentifier, R : DownloadRequest> : BaseDownloadTest() {

    abstract val downloadHandler: T

    abstract val successfulTestRequest: R
    abstract val successfulTestRequest2: R
    abstract val failingTestRequest: R
    abstract val storage: Storage

    @Before
    fun deleteAllDownloads() {
        fun deleteDownload(request: R) {
            val identifier = downloadHandler.identify(request)
            var status: DownloadStatus? = null
            downloadHandler.listen(identifier) {
                status = it
            }
            downloadHandler.delete(identifier)
            waitWhile({ status?.state?.equals(DownloadStatus.State.DELETED) != true }, 3000)
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

        var listenerCalled = false
        // register listener
        downloadHandler.listen(identifier) {
            listenerCalled = true
        }
        waitWhile({ !listenerCalled }, 3000)

        // reset `listenerCalled` to false
        listenerCalled = false
        // unregister listener
        downloadHandler.listen(identifier, null)
        // perform an action
        downloadHandler.download(successfulTestRequest)
        try {
            // wait for listener to set `listenerCalled` to true, if this is the case then fail
            waitWhile({ !listenerCalled }, 3000)
            fail("Unregistered listener has been invoked")
        } catch (e: Exception) {
            // unregistered listener has not been invoked, which is expected behavior
        }
    }

    @Test
    fun testDownloadStatusDuringProcess() {
        val identifier = downloadHandler.identify(successfulTestRequest)

        var status: DownloadStatus? = null
        downloadHandler.listen(identifier) {
            status = it
        }

        // start download
        var downloadCallbackCalled = false
        downloadHandler.download(successfulTestRequest) {
            downloadCallbackCalled = it
        }
        // assert that the download callback has been called
        waitWhile({ !downloadCallbackCalled }, 3000)

        // wait for download to start
        waitWhile({
            status?.state?.equals(DownloadStatus.State.DELETED) != false ||
                (status?.state?.equals(DownloadStatus.State.PENDING) != true &&
                    status?.state?.equals(DownloadStatus.State.RUNNING) != true)
        })

        // test status after start
        assertNotNull(status!!.totalBytes)
        assertNotNull(status!!.downloadedBytes)
        if (status!!.totalBytes!! >= 0L) {
            assertTrue(
                status!!.downloadedBytes!! <= status!!.totalBytes!!
            )
        }

        var isDownloadingAnythingCallbackCalled = false
        downloadHandler.isDownloadingAnything {
            isDownloadingAnythingCallbackCalled = it
        }
        // assert that isDownloadingAnything returns true
        waitWhile({ !isDownloadingAnythingCallbackCalled }, 3000)

        // wait for download to finish
        waitWhile({ status?.state?.equals(DownloadStatus.State.DOWNLOADED) != true })

        // test status after end
        assertNotNull(status!!.totalBytes)
        assertNotNull(status!!.downloadedBytes)
        assertEquals(status!!.totalBytes, status!!.downloadedBytes)

        var deleteCallbackCalled = false
        downloadHandler.delete(identifier) {
            deleteCallbackCalled = it
        }
        // assert that the delete callback has been called
        waitWhile({ !deleteCallbackCalled }, 3000)
        waitWhile({ status!!.state != DownloadStatus.State.DELETED }, 3000)
    }

    @Test
    fun testDownloadStatusAfterCancel() {
        val identifier = downloadHandler.identify(successfulTestRequest)

        // register listener
        var status: DownloadStatus? = null
        downloadHandler.listen(identifier) {
            status = it
        }

        var deleteCallbackCalled = false
        // start download
        downloadHandler.download(successfulTestRequest) {
            // cancel running download and check status
            downloadHandler.delete(identifier) {
                deleteCallbackCalled = true
            }
        }

        // assert that the delete callback has been called
        waitWhile({ !deleteCallbackCalled }, 3000)
        waitWhile({ status?.state?.equals(DownloadStatus.State.DELETED) != true }, 3000)
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
        waitWhile({ status?.state?.equals(DownloadStatus.State.DELETED) != true })
    }

    @Test
    fun testGettingDownloads() {
        var count: Int? = null
        downloadHandler.getDownloads(storage) {
            count = it.size
        }
        // wait for and check result
        waitWhile({ count == null }, 1000)

        var downloaded = false
        downloadHandler.listen(downloadHandler.identify(successfulTestRequest)) {
            if(it.state == DownloadStatus.State.DOWNLOADED){
                downloaded = true
            }
        }
        // start download
        downloadHandler.download(successfulTestRequest)
        // wait for download to finish
        waitWhile({ !downloaded })

        var nextCount: Int? = null
        downloadHandler.getDownloads(storage) {
            nextCount = it.size
        }
        // wait for result
        waitWhile({ nextCount == null }, 1000)

        assertEquals(count!! + 1, nextCount);
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
        waitWhile({ !result || !result2 })
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
}
