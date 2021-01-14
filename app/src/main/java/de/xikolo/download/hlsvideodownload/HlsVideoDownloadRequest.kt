package de.xikolo.download.hlsvideodownload

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.MimeTypes
import com.google.gson.Gson
import de.xikolo.download.DownloadRequest
import de.xikolo.models.Storage
import java.io.IOException

class HlsVideoDownloadRequest(
    val url: String,
    val desiredBitrate: Int,
    val subtitles: Map<String, String>?,
    val identifier: HlsVideoDownloadIdentifier,
    val storage: Storage,
    override val title: String,
    override val showNotification: Boolean,
    override val category: String?
) : DownloadRequest {

    val mediaItem = MediaItem.Builder()
        .setUri(Uri.parse(url))
        .setSubtitles(
            subtitles?.map {
                MediaItem.Subtitle(
                    Uri.parse(it.value),
                    MimeTypes.TEXT_VTT,
                    it.key,
                    C.SELECTION_FLAG_DEFAULT
                )
            }
        )
        .setStreamKeys(
            listOf(
                StreamKey(HlsMasterPlaylist.GROUP_INDEX_VARIANT, 1),
                StreamKey(HlsMasterPlaylist.GROUP_INDEX_AUDIO, 1),
                StreamKey(HlsMasterPlaylist.GROUP_INDEX_SUBTITLE, 1)
            )
        )
        .build()

    fun buildRequest(
        context: Context,
        callback: (com.google.android.exoplayer2.offline.DownloadRequest?) -> Unit
    ) {
        val helper = DownloadHelper.forMediaItem(
            context,
            mediaItem,
            DefaultRenderersFactory(context),
            HlsVideoDownloadHandler.dataSourceFactory
        )
        helper.prepare(
            object : DownloadHelper.Callback {
                override fun onPrepared(helper: DownloadHelper) {
                    helper.clearTrackSelections(0)
                    helper.addTrackSelection(
                        0,
                        DefaultTrackSelector.ParametersBuilder(context)
                            .setForceHighestSupportedBitrate(true)
                            .build()
                    )
                    helper.addTextLanguagesToSelection(
                        true,
                        *(subtitles?.keys?.toTypedArray() ?: arrayOf())
                    )
                    val request = helper.getDownloadRequest(
                        identifier.get(),
                        ArgumentWrapper(title, showNotification, category).encode()
                    )
                    helper.release()
                    callback(request)
                }

                override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                    callback(null)
                    helper.release()
                }
            }
        )
    }

    internal data class ArgumentWrapper(
        val title: String,
        val showNotification: Boolean,
        val category: String?
    ) {
        companion object {
            fun decode(data: ByteArray): ArgumentWrapper =
                Gson().fromJson(data.toString(Charsets.UTF_8), ArgumentWrapper::class.java)
        }

        fun encode(): ByteArray = Gson().toJson(this).toByteArray(Charsets.UTF_8)
    }
}
