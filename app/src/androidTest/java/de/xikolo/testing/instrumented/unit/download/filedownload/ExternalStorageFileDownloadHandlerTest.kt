package de.xikolo.testing.instrumented.unit.download.filedownload

import de.xikolo.utils.extensions.sdcardStorage

class ExternalStorageFileDownloadHandlerTest : AbstractFileDownloadHandlerTest() {

    override val storage = context.sdcardStorage!!
}
