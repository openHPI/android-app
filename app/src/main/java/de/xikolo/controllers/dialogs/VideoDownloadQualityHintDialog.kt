package de.xikolo.controllers.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import de.xikolo.R
import de.xikolo.controllers.dialogs.base.BaseDialogFragment

class VideoDownloadQualityHintDialog : BaseDialogFragment() {

    companion object {
        @JvmField
        val TAG: String = VideoDownloadQualityHintDialog::class.java.simpleName
    }

    var listener: Listener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
            .setTitle(R.string.dialog_download_quality_hint_intent_title)
            .setMessage(
                getString(
                    R.string.dialog_download_quality_hint_intent_message,
                    getString(R.string.settings_title_video_download_quality)
                )
            )
            .setPositiveButton(
                getString(R.string.dialog_download_quality_hint_intent_yes)
            ) { _, _ ->
                listener?.onOpenSettingsClicked()
            }
            .setNegativeButton(
                getString(R.string.dialog_download_quality_hint_intent_no)
            ) { _, _ ->
                listener?.onDismissed()
            }
            .setCancelable(true)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        listener?.onDismissed()
    }

    interface Listener {
        fun onOpenSettingsClicked()
        fun onDismissed()
    }
}
