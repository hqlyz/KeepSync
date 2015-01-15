package com.example.lyz.keepsync.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.example.lyz.keepsync.R;
import com.example.lyz.keepsync.utils.DebugLog;

/**
 * Created by lyz on 15-1-14.
 */
public class FileRenameDialogFragment extends DialogFragment {

    private FileRenameCallback file_rename_callback;
    private View view;
    private EditText edit_text;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layout_inflater = getActivity().getLayoutInflater();
        view = layout_inflater.inflate(R.layout.file_rename, null);
        edit_text = (EditText)view.findViewById(R.id.new_file_name_edittext);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.file_rename_title)
                .setPositiveButton(R.string.common_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DebugLog.i("Positive button was clicked.");
                        String new_file_name = edit_text.getText().toString().trim();
                        DebugLog.i(new_file_name);
                        if (!new_file_name.equals("")) {
                            DebugLog.i("Start renaming.");
                            file_rename_callback.positiveButtonClicked(new_file_name);
                        }
                    }
                })
                .setNegativeButton(R.string.common_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DebugLog.i("Negative button was clicked.");
                        FileRenameDialogFragment.this.dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            file_rename_callback = (FileRenameCallback)activity;
        } catch (ClassCastException e) {
            DebugLog.i(e.getMessage());
        }
    }

    public interface FileRenameCallback {
        public void positiveButtonClicked(String new_file_name);
    }
}
