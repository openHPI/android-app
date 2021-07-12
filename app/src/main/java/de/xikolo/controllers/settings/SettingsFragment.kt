package de.xikolo.controllers.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import de.psdev.licensesdialog.LicensesDialog
import de.xikolo.App
import de.xikolo.BuildConfig
import de.xikolo.R
import de.xikolo.config.Feature
import de.xikolo.controllers.login.LoginActivityAutoBundle
import de.xikolo.download.Downloaders
import de.xikolo.extensions.observe
import de.xikolo.managers.PermissionManager
import de.xikolo.managers.UserManager
import de.xikolo.utils.extensions.getString
import de.xikolo.utils.extensions.getStringArray
import de.xikolo.utils.extensions.showToast
import de.xikolo.utils.extensions.storages
import java.util.Calendar

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        val TAG: String = SettingsFragment::class.java.simpleName
    }

    private var loginOut: Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState ?: Bundle())

        App.instance.state.login
            .observe(this) {
                if (it) {
                    buildLogoutView(loginOut)
                } else {
                    buildLoginView(loginOut)
                }
            }
    }

    override fun onResume() {
        refreshPipStatus()
        super.onResume()
    }

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)

        findPreference<ListPreference>(getString(R.string.preference_storage))?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                Downloaders.isDownloadingAnything {
                    if (it) {
                        showToast(R.string.notification_storage_locked)
                    } else {
                        val listener = preference.onPreferenceChangeListener
                        preference.onPreferenceChangeListener = null
                        (preference as ListPreference).value = newValue as String
                        preference.onPreferenceChangeListener = listener
                    }
                }
                false
            }

        if (App.instance.storages.size < 2) {
            val general = findPreference<PreferenceCategory>(getString(R.string.preference_category_general))
            val storagePref = findPreference<ListPreference>(getString(R.string.preference_storage))
            general?.removePreference(storagePref)
        }

        val pipSettings = findPreference<Preference>(getString(R.string.preference_video_pip))
        if (!Feature.PIP) {
            val video = findPreference<PreferenceCategory>(getString(R.string.preference_category_video_playback_speed))
            video?.removePreference(pipSettings)
        } else {
            pipSettings?.setOnPreferenceClickListener {
                try {
                    val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS")
                    val uri = Uri.fromParts("package", activity?.packageName, null)
                    intent.data = uri
                    activity?.startActivity(intent)
                } catch (e: RuntimeException) {
                    PermissionManager.startAppInfo(activity)
                }
                true
            }
            refreshPipStatus()
        }

        // programmatically build info preferences

        val info = findPreference<PreferenceCategory>(getString(R.string.preference_category_info))!!

        val copyright = Preference(preferenceScreen.context)
        copyright.title = String.format(getString(R.string.settings_copyright), Calendar.getInstance().get(Calendar.YEAR))
        if (Feature.enabled("url_copyright")) {
            copyright.setOnPreferenceClickListener { _ ->
                openUrl(App.instance.getString("url_copyright"))
                true
            }
        } else {
            copyright.isEnabled = false
        }

        info.addPreference(copyright)

        if (Feature.enabled("legal_links_urls")) {
            val titles = App.instance.getStringArray("legal_links_titles")

            App.instance.getStringArray("legal_links_urls").forEachIndexed { i, url ->
                val pref = Preference(preferenceScreen.context)
                pref.title = titles[i]
                pref.setOnPreferenceClickListener { _ ->
                    openUrl(url)
                    true
                }

                info.addPreference(pref)
            }
        }

        if (Feature.enabled("url_faq")) {
            val faq = Preference(preferenceScreen.context)
            faq.title = getString(R.string.settings_title_faq)
            faq.setOnPreferenceClickListener { _ ->
                openUrl(App.instance.getString("url_faq"))
                true
            }

            info.addPreference(faq)
        }

        val licenses = Preference(preferenceScreen.context)
        licenses.title = getString(R.string.settings_title_licenses)
        licenses.summary = getString(R.string.settings_summary_licenses)
        licenses.setOnPreferenceClickListener {
            LicensesDialog.Builder(activity)
                .setNotices(R.raw.notices)
                .setTitle(R.string.settings_title_licenses)
                .build()
                .show()
            true
        }

        info.addPreference(licenses)

        val buildVersion = Preference(preferenceScreen.context)
        buildVersion.title = getString(R.string.settings_title_build)
        buildVersion.summary = getString(R.string.settings_summary_build) + " " + BuildConfig.VERSION_NAME
        buildVersion.isEnabled = false

        info.addPreference(buildVersion)

        loginOut = Preference(preferenceScreen.context)
        if (UserManager.isAuthorized) {
            buildLogoutView(loginOut)
        } else {
            buildLoginView(loginOut)
        }

        info.addPreference(loginOut)
    }

    private fun refreshPipStatus() {
        val pipSettings = findPreference<Preference>(getString(R.string.preference_video_pip))
        pipSettings?.let {
            if (!PermissionManager.hasPipPermission(context)) {
                it.summary = getString(R.string.settings_summary_video_pip_unavailable)
            } else {
                it.summary = ""
            }
        }
    }

    private fun buildLoginView(pref: Preference?) {
        if (pref != null) {
            pref.title = getString(R.string.login)
            pref.setOnPreferenceClickListener { _ ->
                val intent = LoginActivityAutoBundle.builder().build(activity!!)
                startActivity(intent)
                true
            }
        }
    }

    private fun buildLogoutView(pref: Preference?) {
        if (pref != null) {
            pref.title = getString(R.string.logout)
            pref.setOnPreferenceClickListener { _ ->
                UserManager.logout()
                showToast(R.string.toast_successful_logout)
                true
            }
        }
    }

    private fun openUrl(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setToolbarColor(ContextCompat.getColor(App.instance, R.color.apptheme_primary))
            .build()
        customTabsIntent.launchUrl(activity!!, Uri.parse(url))
    }
}
