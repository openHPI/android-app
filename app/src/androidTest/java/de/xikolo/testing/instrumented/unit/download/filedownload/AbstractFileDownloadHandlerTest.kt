package de.xikolo.testing.instrumented.unit.download.filedownload

import de.xikolo.download.DownloadCategory
import de.xikolo.download.DownloadStatus
import de.xikolo.download.filedownload.FileDownloadHandler
import de.xikolo.download.filedownload.FileDownloadIdentifier
import de.xikolo.download.filedownload.FileDownloadRequest
import de.xikolo.testing.instrumented.unit.download.DownloadHandlerTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

abstract class AbstractFileDownloadHandlerTest : DownloadHandlerTest<FileDownloadHandler,
    FileDownloadIdentifier, FileDownloadRequest>() {

    override val downloadHandler = FileDownloadHandler

    override val successfulTestRequest: FileDownloadRequest
        get() = FileDownloadRequest(
            "https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_1280_10MG.mp4",
            File(storage.file, "file1"),
            "File 1",
            true,
            DownloadCategory.Other
        )
    override val successfulTestRequest2
        get() = FileDownloadRequest(
            "https://file-examples-com.github.io/uploads/2017/10/file-example_PDF_1MB.pdf",
            File(storage.file, "file2"),
            "File 2",
            true,
            DownloadCategory.Other
        )
    override val failingTestRequest
        get() = FileDownloadRequest(
            "https://www.example.com/notfoundfilehwqnqkdrzn42r.mp4",
            File(storage.file, "failingfile"),
            "Failing File",
            true,
            DownloadCategory.Other
        )

    @Test
    fun testSizeAfterDownload() {
        var status: DownloadStatus? = null
        downloadHandler.listen(downloadHandler.identify(successfulTestRequest)) {
            status = it
        }
        // start download
        downloadHandler.download(successfulTestRequest)
        // wait for download to finish
        waitWhile({ status?.state?.equals(DownloadStatus.State.DOWNLOADED) != true })

        assertTrue(successfulTestRequest.localFile.length() > 0)
    }
}
