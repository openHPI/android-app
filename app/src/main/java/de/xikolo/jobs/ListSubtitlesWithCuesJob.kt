package de.xikolo.jobs


import android.util.Log
import de.xikolo.config.Config
import de.xikolo.jobs.base.RequestJobCallback
import de.xikolo.jobs.base.RequestJob
import de.xikolo.models.SubtitleCue
import de.xikolo.models.SubtitleTrack
import de.xikolo.models.base.Sync
import de.xikolo.network.ApiService
import ru.gildor.coroutines.retrofit.awaitResponse

class ListSubtitlesWithCuesJob(callback: RequestJobCallback, private val videoId: String) : RequestJob(callback, Precondition.AUTH) {

    companion object {
        val TAG: String = ListSubtitlesWithCuesJob::class.java.simpleName
    }

    override suspend fun onRun() {
        val response = ApiService.getInstance().listSubtitlesWithCuesForVideo(videoId).awaitResponse()

        if (response.isSuccessful) {
            if (Config.DEBUG) Log.i(TAG, "Subtitles received")

            val ids = Sync.Data.with(SubtitleTrack::class.java, *response.body()!!)
                    .addFilter("videoId", videoId)
                    .run()
            Sync.Included.with(SubtitleCue::class.java, *response.body()!!)
                    .addFilter("subtitleId", ids)
                    .run()

            callback?.success()
        } else {
            if (Config.DEBUG) Log.e(TAG, "Error while fetching subtitle list")
            callback?.error(RequestJobCallback.ErrorCode.ERROR)
        }
    }

}
