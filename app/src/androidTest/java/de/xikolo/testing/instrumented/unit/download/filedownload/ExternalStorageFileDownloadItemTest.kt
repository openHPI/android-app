package de.xikolo.testing.instrumented.unit.download.filedownload

import de.xikolo.utils.extensions.sdcardStorage

class ExternalStorageFileDownloadItemTest : AbstractFileDownloadItemTest() {

    override val storage = context.sdcardStorage!!
}
