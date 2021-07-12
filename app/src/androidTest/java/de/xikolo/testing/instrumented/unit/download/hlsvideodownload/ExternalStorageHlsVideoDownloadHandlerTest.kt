package de.xikolo.testing.instrumented.unit.download.hlsvideodownload

import de.xikolo.utils.extensions.sdcardStorage

class ExternalStorageHlsVideoDownloadHandlerTest : AbstractHlsVideoDownloadHandlerTest() {

    override val storage = context.sdcardStorage!!
}
