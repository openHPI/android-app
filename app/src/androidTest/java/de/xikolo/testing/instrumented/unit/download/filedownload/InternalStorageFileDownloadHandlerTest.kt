package de.xikolo.testing.instrumented.unit.download.filedownload

import de.xikolo.utils.extensions.internalStorage

class InternalStorageFileDownloadHandlerTest : AbstractFileDownloadHandlerTest() {

    override val storage = context.internalStorage
}
