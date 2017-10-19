package de.xikolo.controllers.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import de.xikolo.R;
import de.xikolo.controllers.dialogs.base.BaseDialogFragment;
import de.xikolo.utils.DisplayUtil;

public class ConfirmDeleteDialog extends BaseDialogFragment {

    public static final String TAG = ConfirmDeleteDialog.class.getSimpleName();

    private ConfirmDeleteDialogListener listener;

    public static final String ARG_MULTIPLE_FILES = "multiple_files";

    private boolean multipleFiles;

    public void setConfirmDeleteDialogListener(ConfirmDeleteDialogListener listener) {
        this.listener = listener;
    }

    public ConfirmDeleteDialog() {
    }

    public static ConfirmDeleteDialog getInstance(boolean multipleFiles) {
        ConfirmDeleteDialog fragment = new ConfirmDeleteDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_MULTIPLE_FILES, multipleFiles);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            multipleFiles = getArguments().getBoolean(ARG_MULTIPLE_FILES);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog);
        builder.setMessage(multipleFiles ? R.string.dialog_confirm_delete_message_multi : R.string.dialog_confirm_delete_message)
                .setTitle(multipleFiles ? R.string.dialog_confirm_delete_title_multi : R.string.dialog_confirm_delete_title)
                .setPositiveButton(R.string.dialog_confirm_delete_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) {
                            listener.onDialogPositiveClick(ConfirmDeleteDialog.this);
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ConfirmDeleteDialog.this.getDialog().cancel();
                    }
                })
                .setCancelable(true);

        if (DisplayUtil.is7inchTablet(getActivity())) {
            builder.setNeutralButton(R.string.dialog_confirm_delete_yes_always, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (listener != null) {
                        listener.onDialogPositiveAndAlwaysClick(ConfirmDeleteDialog.this);
                    }
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    public interface ConfirmDeleteDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);

        void onDialogPositiveAndAlwaysClick(DialogFragment dialog);
    }

}
