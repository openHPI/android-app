package de.xikolo.testing.instrumented.unit.download.hlsvideodownload

import de.xikolo.utils.extensions.internalStorage

class InternalStorageHlsVideoDownloadHandlerTest : AbstractHlsVideoDownloadHandlerTest() {

    override val storage = context.internalStorage
}
