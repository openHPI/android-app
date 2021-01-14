package de.xikolo.download.hlsvideodownload

import de.xikolo.download.DownloadIdentifier

data class HlsVideoDownloadIdentifier(
    private val url: String,
    private val quality: Int
) : DownloadIdentifier {

    companion object {
        fun from(identifier: String): HlsVideoDownloadIdentifier {
            val parts = identifier.split(";", limit = 2)
            return HlsVideoDownloadIdentifier(parts[1], parts[0].toInt())
        }
    }

    fun get(): String {
        return "$quality;$url"
    }
}
