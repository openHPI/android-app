package de.xikolo.controllers.helper

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import butterknife.BindView
import butterknife.ButterKnife
import de.xikolo.R
import de.xikolo.controllers.dialogs.ConfirmDeleteDialog
import de.xikolo.controllers.dialogs.ConfirmDeleteDialogAutoBundle
import de.xikolo.controllers.dialogs.MobileDownloadDialog
import de.xikolo.download.DownloadItem
import de.xikolo.download.DownloadStatus
import de.xikolo.storages.ApplicationPreferences
import de.xikolo.utils.extensions.ConnectivityType
import de.xikolo.utils.extensions.asFormattedFileSize
import de.xikolo.utils.extensions.connectivityType
import de.xikolo.utils.extensions.isOnline
import de.xikolo.utils.extensions.showToast

/**
 * When the url of the DownloadAsset's URL is null, the urlNotAvailableMessage is shown and the UI will be disabled.
 */
class DownloadViewHelper(
    private val activity: FragmentActivity,
    private val download: DownloadItem<*, *>,
    title: CharSequence? = null,
    description: CharSequence? = null,
    urlNotAvailableMessage: CharSequence? = null,
    openText: CharSequence? = null,
    openClick: (() -> Unit)? = null,
    downloadClick: (() -> Unit)? = null,
    private val onDeleted: (() -> Unit)? = null
) {

    companion object {
        val TAG: String = DownloadViewHelper::class.java.simpleName
        private const val MILLISECONDS = 1000L
    }

    val view: View

    @BindView(R.id.textFileName)
    lateinit var textFileName: TextView

    @BindView(R.id.textFileSize)
    lateinit var textFileSize: TextView

    @BindView(R.id.textDescription)
    lateinit var textDescription: TextView

    @BindView(R.id.downloadStartContainer)
    lateinit var viewDownloadStart: View

    @BindView(R.id.buttonDownloadStart)
    lateinit var buttonDownloadStart: Button

    @BindView(R.id.downloadRunningContainer)
    lateinit var viewDownloadRunning: View

    @BindView(R.id.buttonDownloadCancel)
    lateinit var buttonDownloadCancel: TextView

    @BindView(R.id.progressDownload)
    lateinit var progressBarDownload: ProgressBar

    @BindView(R.id.downloadEndContainer)
    lateinit var viewDownloadEnd: View

    @BindView(R.id.buttonDownloadOpen)
    lateinit var buttonOpenDownload: Button

    @BindView(R.id.buttonDownloadDelete)
    lateinit var buttonDeleteDownload: Button

    private val progressBarUpdater: Runnable
    private var progressBarUpdaterRunning = false

    init {
        view = activity.layoutInflater.inflate(R.layout.item_download_helper, null)

        ButterKnife.bind(this, view)

        val appPreferences = ApplicationPreferences()

        buttonDownloadStart.setOnClickListener {
            downloadClick?.invoke()
            if (activity.isOnline) {
                if (activity.connectivityType == ConnectivityType.CELLULAR && appPreferences.isDownloadNetworkLimitedOnMobile) {
                    val dialog = MobileDownloadDialog()
                    dialog.listener = object : MobileDownloadDialog.MobileDownloadGrantedListener {
                        override fun onGranted(dialog: DialogFragment) {
                            appPreferences.isDownloadNetworkLimitedOnMobile = false
                            startDownload()
                        }
                    }
                    dialog.show(activity.supportFragmentManager, MobileDownloadDialog.TAG)
                } else {
                    startDownload()
                }
            } else {
                activity.showToast(R.string.toast_no_network)
            }
        }

        buttonDownloadCancel.setOnClickListener {
            download.delete(activity)
            showStartState()
        }

        buttonDeleteDownload.setOnClickListener { _ ->
            if (appPreferences.confirmBeforeDeleting) {
                val dialog = ConfirmDeleteDialogAutoBundle.builder(false).build()
                dialog.listener = object : ConfirmDeleteDialog.Listener {
                    override fun onDialogPositiveClick(dialog: DialogFragment) {
                        deleteDownload()
                    }

                    override fun onDialogPositiveAndAlwaysClick(dialog: DialogFragment) {
                        appPreferences.confirmBeforeDeleting = false
                        deleteDownload()
                    }
                }
                dialog.show(activity.supportFragmentManager, ConfirmDeleteDialog.TAG)
            } else {
                deleteDownload()
            }
        }

        if (title != null) {
            textFileName.text = title
        } else {
            textFileName.text = download.title
        }

        if (description != null) {
            textDescription.text = description
            textDescription.visibility = View.VISIBLE
        } else {
            textDescription.visibility = View.GONE
        }

        if (!download.downloadable) {
            showStartState()
            view.isEnabled = false
            buttonDownloadStart.isEnabled = false

            if (urlNotAvailableMessage != null) {
                buttonDownloadStart.text = urlNotAvailableMessage
            } else {
                buttonDownloadStart.text = activity.getString(R.string.not_available)
            }
        }

        if (openText != null) {
            buttonOpenDownload.text = openText
        }

        when {
            openClick != null -> buttonOpenDownload.setOnClickListener { openClick() }
            download.openAction != null -> buttonOpenDownload.setOnClickListener {
                download.openAction?.invoke(activity)
            }
            else -> buttonOpenDownload.visibility = View.GONE
        }

        progressBarUpdater =
            object : Runnable {
                override fun run() {
                    Handler(Looper.getMainLooper()).post {
                        onStatusChanged(download.status.value)

                        if (progressBarUpdaterRunning) {
                            progressBarDownload.postDelayed(this, MILLISECONDS)
                        }
                    }
                }
            }

        if (download.downloadable) {
            download.status.observe(activity) {
                onStatusChanged(it)
            }
        }
    }

    private fun deleteDownload() {
        download.delete(activity) {
            if (it) {
                showStartState()
                onDeleted?.invoke()
            }
        }
    }

    private fun startDownload() {
        download.start(activity) {
            if (it) {
                showRunningState()
            }
        }
    }

    private fun showStartState() {
        viewDownloadStart.visibility = View.VISIBLE
        viewDownloadRunning.visibility = View.INVISIBLE
        viewDownloadEnd.visibility = View.INVISIBLE

        progressBarDownload.progress = 0
        progressBarUpdaterRunning = false

        if (download.size != 0L) {
            textFileSize.visibility = View.VISIBLE
            textFileSize.text = download.size.asFormattedFileSize
        } else {
            textFileSize.visibility = View.GONE
        }
    }

    private fun showPendingState() {
        viewDownloadStart.visibility = View.INVISIBLE
        viewDownloadRunning.visibility = View.VISIBLE
        viewDownloadEnd.visibility = View.INVISIBLE

        buttonDownloadCancel.visibility = View.INVISIBLE
        progressBarDownload.isIndeterminate = true
    }

    private fun showRunningState() {
        viewDownloadStart.visibility = View.INVISIBLE
        viewDownloadRunning.visibility = View.VISIBLE
        viewDownloadEnd.visibility = View.INVISIBLE

        buttonDownloadCancel.visibility = View.VISIBLE
        progressBarDownload.isIndeterminate = false
        textFileSize.visibility = View.VISIBLE

        if (!progressBarUpdaterRunning) {
            progressBarUpdaterRunning = true
            Thread(progressBarUpdater).start()
        }
    }

    private fun showEndState() {
        viewDownloadStart.visibility = View.INVISIBLE
        viewDownloadRunning.visibility = View.INVISIBLE
        viewDownloadEnd.visibility = View.VISIBLE

        textFileSize.visibility = View.VISIBLE

        if (download.size != 0L) {
            textFileSize.visibility = View.VISIBLE
            textFileSize.text = download.size.asFormattedFileSize
        } else {
            textFileSize.visibility = View.GONE
        }

        progressBarUpdaterRunning = false
    }

    private fun onStatusChanged(status: DownloadStatus) {
        when (status.state) {
            DownloadStatus.State.PENDING -> showPendingState()
            DownloadStatus.State.RUNNING -> {
                showRunningState()

                val downloadedBytes = status.downloadedBytes ?: 0L
                val totalBytes = status.totalBytes ?: 0L

                if (totalBytes == 0L) {
                    progressBarDownload.progress = 0
                    textFileSize.text = ""
                } else {
                    progressBarDownload.progress =
                        (downloadedBytes * 100 / totalBytes).toInt()
                    textFileSize.text = activity.getString(
                        R.string.download_slash,
                        downloadedBytes.asFormattedFileSize,
                        totalBytes.asFormattedFileSize
                    )
                }
            }
            DownloadStatus.State.DOWNLOADED -> showEndState()
            DownloadStatus.State.DELETED -> showStartState()
        }
    }
}
