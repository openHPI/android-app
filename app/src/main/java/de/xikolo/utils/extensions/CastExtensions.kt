package de.xikolo.utils.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.TextTrackStyle
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.images.WebImage
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.xikolo.controllers.cast.CastActivity
import de.xikolo.models.Video
import de.xikolo.utils.LanguageUtil

val <T : Context> T.isCastConnected: Boolean
    get() {
        return try {
            if (hasPlayServices) {
                val castContext = CastContext.getSharedInstance(this)
                val sessionManager = castContext.sessionManager

                sessionManager.currentCastSession != null && sessionManager.currentCastSession.isConnected
            } else {
                false
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }

val <T : Context> T.isCastAvailable: Boolean
    get() {
        return try {
            if (hasPlayServices) {
                val castContext = CastContext.getSharedInstance(this)

                castContext.castState != CastState.NO_DEVICES_AVAILABLE
            } else {
                false
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }

fun <T : Video> T.cast(activity: Activity, autoPlay: Boolean): PendingResult<RemoteMediaClient.MediaChannelResult>? {
    if (!activity.hasPlayServices) {
        return null
    }

    val castContext = CastContext.getSharedInstance(activity)
    val sessionManager = castContext.sessionManager
    val session = sessionManager.currentCastSession

    if (session != null) {
        val remoteMediaClient = session.remoteMediaClient

        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                val intent = Intent(activity, CastActivity::class.java)
                activity.startActivity(intent)
                remoteMediaClient.unregisterCallback(this)
            }

            override fun onMetadataUpdated() {

            }

            override fun onQueueStatusUpdated() {

            }

            override fun onPreloadStatusUpdated() {

            }

            override fun onSendingRemoteMediaRequest() {

            }

            override fun onAdBreakStatusUpdated() {}
        })

        //build cast metadata
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, item?.title)

        val image = WebImage(Uri.parse(thumbnailUrl))

        // small size image used for notification, mini­controller and Lock Screen on JellyBean
        mediaMetadata.addImage(image)

        // large image, used on the Cast Player page and Lock Screen on KitKat
        mediaMetadata.addImage(image)

        val subtitleTracks = subtitles.mapIndexed { index, videoSubtitles ->
            MediaTrack.Builder(index.toLong(), MediaTrack.TYPE_TEXT)
                .setName(LanguageUtil.toLocaleName(videoSubtitles.language))
                .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                .setContentId(videoSubtitles.vttUrl)
                .setContentType("text/vtt")
                .setLanguage(videoSubtitles.language)
                .build()
        }

        val castMetadata = MediaInfo.Builder(streamToPlay?.hdUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("application/x-mpegURL")
            .setMetadata(mediaMetadata)
            .setMediaTracks(subtitleTracks)
            .setTextTrackStyle(TextTrackStyle.fromSystemSettings(activity))
            .build()

        return remoteMediaClient.load(
            castMetadata,
            MediaLoadOptions.Builder()
                .setAutoplay(autoPlay)
                .setPlayPosition(progress.toLong())
                .build()
        )
    } else {
        return null
    }
}
