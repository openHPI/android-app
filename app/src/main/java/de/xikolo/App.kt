package de.xikolo

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.preference.PreferenceManager
import de.xikolo.config.Config
import de.xikolo.lanalytics.Lanalytics
import de.xikolo.models.migrate.RealmSchemaMigration
import de.xikolo.states.ConnectivityStateLiveData
import de.xikolo.states.DownloadStateLiveData
import de.xikolo.states.LoginStateLiveData
import de.xikolo.states.PermissionStateLiveData
import de.xikolo.states.base.LiveDataEvent
import de.xikolo.utils.ClientUtil
import io.realm.Realm
import io.realm.RealmConfiguration

class App : Application() {

    companion object {
        val TAG: String = App::class.java.simpleName

        @JvmStatic
        lateinit var instance: App
            private set
    }

    inner class State {

        val connectivity: ConnectivityStateLiveData by lazy {
            ConnectivityStateLiveData(this@App, true)
        }

        val login: LoginStateLiveData by lazy {
            LoginStateLiveData()
        }

        val downloadCancellation by lazy {
            LiveDataEvent()
        }

        val download: DownloadStateLiveData.Companion by lazy {
            DownloadStateLiveData
        }

        val permission: PermissionStateLiveData.Companion by lazy {
            PermissionStateLiveData
        }
    }

    val state = State()

    val lanalytics: Lanalytics by lazy {
        Lanalytics.getInstance(this, Config.API_URL + Config.LANALYTICS_PATH)
    }

    val clientId: String by lazy {
        ClientUtil.id(this)
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        configureRealm()
        configureDefaultSettings()
        configureWebView()
    }

    private fun configureRealm() {
        Realm.init(this)
        val config = RealmConfiguration.Builder()
            .schemaVersion(Config.REALM_SCHEMA_VERSION.toLong())
            .migration(RealmSchemaMigration())
            .build()
        Realm.setDefaultConfiguration(config)
    }

    private fun configureDefaultSettings() {
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)
    }

    private fun configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true)

        if (Config.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    fun syncCookieSyncManager() {
        CookieManager.getInstance().flush()
    }

    fun clearCookieSyncManager() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().removeSessionCookies(null)
        CookieManager.getInstance().flush()
    }

}
