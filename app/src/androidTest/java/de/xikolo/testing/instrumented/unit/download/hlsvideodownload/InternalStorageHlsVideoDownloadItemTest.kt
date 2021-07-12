package de.xikolo.testing.instrumented.unit.download.hlsvideodownload

import de.xikolo.utils.extensions.internalStorage

class InternalStorageHlsVideoDownloadItemTest : AbstractHlsVideoDownloadItemTest() {

    override val storage = context.internalStorage
}
