package de.xikolo.download.hlsvideodownload

import de.xikolo.download.DownloadIdentifier

/**
 * DownloadIdentifier class for HLS video downloads.
 *
 * @param url The HLS master playlist URL.
 * @param quality The selected quality as in [VideoSettingsHelper.VideoQuality] bitratePercent.
 */
data class HlsVideoDownloadIdentifier(
    private val url: String,
    private val quality: Float
) : DownloadIdentifier {

    companion object {

        /**
         * Constructs a HlsVideoDownloadIdentifier object from an internal identifier string.
         *
         * @param identifier The internal identifier.
         */
        fun from(identifier: String): HlsVideoDownloadIdentifier {
            val parts = identifier.split(";", limit = 2)
            return HlsVideoDownloadIdentifier(parts[1], parts[0].toInt() / 100.0f)
        }
    }

    /**
     * Returns the internal identifier.
     */
    fun get(): String {
        return "${(quality * 100).toInt()};$url"
    }
}
