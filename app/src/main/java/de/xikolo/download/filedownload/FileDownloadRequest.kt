package de.xikolo.download.filedownload

import androidx.core.net.toUri
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.Extras
import de.xikolo.App
import de.xikolo.R
import de.xikolo.config.Config
import de.xikolo.download.DownloadRequest
import de.xikolo.managers.UserManager
import de.xikolo.storages.ApplicationPreferences
import java.io.File
import java.util.Locale

data class FileDownloadRequest(
    val url: String,
    val localFile: File,
    override val title: String,
    override val showNotification: Boolean,
    override val category: String?
) : DownloadRequest {

    companion object {
        const val REQUEST_EXTRA_TITLE = "title"
        const val REQUEST_EXTRA_SHOW_NOTIFICATION = "showNotification"
        const val REQUEST_EXTRA_CATEGORY = "category"
    }

    fun buildRequest(): Request {
        return Request(url, localFile.toUri()).apply {
            networkType =
                if (ApplicationPreferences().isDownloadNetworkLimitedOnMobile) {
                    NetworkType.WIFI_ONLY
                } else {
                    NetworkType.ALL
                }

            val extrasMap =
                mutableMapOf(
                    REQUEST_EXTRA_TITLE to title,
                    REQUEST_EXTRA_SHOW_NOTIFICATION to showNotification.toString()
                ).apply {
                    if (category != null) {
                        put(REQUEST_EXTRA_CATEGORY, category)
                    }
                }

            extras = Extras(extrasMap)
            groupId = 0

            addHeader(Config.HEADER_USER_AGENT, Config.HEADER_USER_AGENT_VALUE)
            addHeader(
                Config.HEADER_ACCEPT,
                Config.MEDIA_TYPE_JSON_API + "; xikolo-version=" + Config.XIKOLO_API_VERSION
            )
            addHeader(Config.HEADER_CONTENT_TYPE, Config.MEDIA_TYPE_JSON_API)
            addHeader(Config.HEADER_USER_PLATFORM, Config.HEADER_USER_PLATFORM_VALUE)
            addHeader(Config.HEADER_ACCEPT_LANGUAGE, Locale.getDefault().language)

            if (url.toUri().host == App.instance.getString(R.string.app_host) &&
                UserManager.isAuthorized
            ) {
                addHeader(
                    Config.HEADER_AUTH,
                    Config.HEADER_AUTH_VALUE_PREFIX_JSON_API + UserManager.token!!
                )
            }
        }
    }
}
