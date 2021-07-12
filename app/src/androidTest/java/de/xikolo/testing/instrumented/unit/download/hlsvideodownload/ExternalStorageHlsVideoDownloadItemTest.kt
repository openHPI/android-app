package de.xikolo.testing.instrumented.unit.download.hlsvideodownload

import de.xikolo.utils.extensions.sdcardStorage

class ExternalStorageHlsVideoDownloadItemTest : AbstractHlsVideoDownloadItemTest() {

    override val storage = context.sdcardStorage!!
}
