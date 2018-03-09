package de.xikolo.jobs

import android.util.Log
import de.xikolo.config.Config
import de.xikolo.jobs.base.RequestJob
import de.xikolo.jobs.base.RequestJobCallback
import de.xikolo.models.Channel
import de.xikolo.models.base.Sync
import de.xikolo.network.ApiService
import ru.gildor.coroutines.retrofit.awaitResponse

class ListChannelsJob(callback: RequestJobCallback) : RequestJob(callback) {

    companion object {
        val TAG: String = ListChannelsJob::class.java.simpleName
    }

    override suspend fun onRun() {
        val response = ApiService.getInstance().listChannels().awaitResponse()

        if (response.isSuccessful) {
            if (Config.DEBUG) Log.i(TAG, "Channels received")

            Sync.Data.with(Channel::class.java, *response.body()!!).run()

            callback?.success()
        } else {
            if (Config.DEBUG) Log.e(TAG, "Error while fetching channels list")
            callback?.error(RequestJobCallback.ErrorCode.ERROR)
        }
    }

}