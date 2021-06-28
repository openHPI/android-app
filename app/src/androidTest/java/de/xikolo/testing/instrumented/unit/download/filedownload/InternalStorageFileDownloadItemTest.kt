package de.xikolo.testing.instrumented.unit.download.filedownload

import de.xikolo.utils.extensions.internalStorage

class InternalStorageFileDownloadItemTest : AbstractFileDownloadItemTest() {

    override val storage = context.internalStorage
}
