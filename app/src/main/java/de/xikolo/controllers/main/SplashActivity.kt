package de.xikolo.controllers.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import de.xikolo.controllers.dialogs.*
import de.xikolo.extensions.observe
import de.xikolo.network.jobs.CheckHealthJob
import de.xikolo.network.jobs.base.HealthCheckNetworkState
import de.xikolo.network.jobs.base.HealthCheckNetworkStateLiveData
import de.xikolo.network.jobs.base.NetworkCode
import java.util.*
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    companion object {
        val TAG: String = SplashActivity::class.java.simpleName
    }

    private val networkState: HealthCheckNetworkStateLiveData by lazy {
        HealthCheckNetworkStateLiveData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkState
            .observe(this) {
                if (it.code == NetworkCode.STARTED) {
                    return@observe
                }

                when (it.code) {
                    NetworkCode.SUCCESS                -> startApp()
                    NetworkCode.NO_NETWORK             -> startApp()
                    NetworkCode.API_VERSION_EXPIRED    -> showApiVersionExpiredDialog()
                    NetworkCode.MAINTENANCE            -> showServerMaintenanceDialog()
                    NetworkCode.API_VERSION_DEPRECATED -> {
                        it as HealthCheckNetworkState

                        it.deprecationDate?.let { deprecationDate ->
                            val now = Date()
                            val distance = deprecationDate.time - now.time
                            val days = TimeUnit.DAYS.convert(distance, TimeUnit.MILLISECONDS)

                            if (days <= 14) {
                                showApiVersionDeprecatedDialog(deprecationDate)
                            } else {
                                startApp()
                            }
                        } ?: run {
                            startApp()
                        }
                    }
                    else                               -> showServerErrorDialog()
                }
            }

        runHealthJob()
    }

    private fun runHealthJob() {
        CheckHealthJob(networkState, false).run()
    }

    private fun showApiVersionExpiredDialog() {
        val dialog = ApiVersionExpiredDialog()
        dialog.listener = object : ApiVersionExpiredDialog.Listener {
            override fun onOpenPlayStoreClicked() {
                openPlayStore()
            }

            override fun onDismissed() {
                closeApp()
            }
        }
        showDialog(dialog, ApiVersionExpiredDialog.TAG)
    }

    private fun showApiVersionDeprecatedDialog(deprecationDate: Date) {
        val dialog = ApiVersionDeprecatedDialogAutoBundle.builder(deprecationDate).build()
        dialog.listener = object : ApiVersionDeprecatedDialog.Listener {
            override fun onOpenPlayStoreClicked() {
                openPlayStore()
            }

            override fun onDismissed() {
                startApp()
            }
        }
        showDialog(dialog, ApiVersionDeprecatedDialog.TAG)
    }

    private fun showServerMaintenanceDialog() {
        val dialog = ServerMaintenanceDialog()
        dialog.listener = object : ServerMaintenanceDialog.Listener {
            override fun onDismissed() {
                closeApp()
            }
        }
        showDialog(dialog, ServerMaintenanceDialog.TAG)
    }

    private fun showServerErrorDialog() {
        val dialog = ServerErrorDialog()
        dialog.listener = object : ServerErrorDialog.Listener {
            override fun onDismissed() {
                closeApp()
            }
        }
        showDialog(dialog, ServerErrorDialog.TAG)
    }

    private fun startApp() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }

    private fun closeApp() {
        finish()
    }

    private fun openPlayStore() {
        val appPackageName = packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (e: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }

        finish()
    }

    // bug fix workaround
    private fun showDialog(dialogFragment: DialogFragment, tag: String) {
        val ft = supportFragmentManager.beginTransaction()
        ft.add(dialogFragment, tag)
        ft.commitAllowingStateLoss()
    }

}
