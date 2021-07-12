package de.xikolo.controllers.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.yatatsu.autobundle.AutoBundleField
import de.xikolo.R
import de.xikolo.controllers.dialogs.base.BaseDialogFragment

class ModuleDownloadDialog : BaseDialogFragment() {

    companion object {
        @JvmField val TAG: String = ModuleDownloadDialog::class.java.simpleName
    }

    var listener: ItemSelectionListener? = null

    @AutoBundleField(required = false)
    var video: Boolean = false

    @AutoBundleField(required = false)
    var slides: Boolean = false

    @AutoBundleField
    lateinit var moduleTitle: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = String.format(activity!!.getString(R.string.dialog_module_downloads_title), moduleTitle)

        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(title)
            .setMultiChoiceItems(
                R.array.dialog_module_downloads_array,
                null
            ) { _, which, isChecked ->
                when (which) {
                    0 -> video = isChecked
                    1 -> slides = isChecked
                }
            }
            .setPositiveButton(R.string.download) { _, _ ->
                listener?.onSelected(this, video, slides)
            }
            .setNegativeButton(R.string.dialog_negative) { _, _ -> dialog?.cancel() }
            .setCancelable(true)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(true)

        return dialog
    }

    interface ItemSelectionListener {
        fun onSelected(dialog: DialogFragment, video: Boolean, slides: Boolean)
    }

}
