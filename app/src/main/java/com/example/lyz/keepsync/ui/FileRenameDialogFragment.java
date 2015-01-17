package com.example.lyz.keepsync.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
 *
 */
public class FileRenameDialogFragment extends DialogFragment {

    private FileRenameCallback file_rename_callback;
    private EditText edit_text;
    private String old_file_name;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DebugLog.i("FileRenameDialogFragment onCreateDialog called.");
        LayoutInflater layout_inflater = getActivity().getLayoutInflater();
        View view = layout_inflater.inflate(R.layout.file_rename, null);
        edit_text = (EditText)view.findViewById(R.id.new_file_name_edittext);
        edit_text.setText(old_file_name);
        edit_text.setSelection(0, old_file_name.lastIndexOf("."));

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
                            file_rename_callback.onFileRename(new_file_name);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        DebugLog.i("FileRenameDialogFragment onCreateDialog called.");
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        DebugLog.i("FileRenameDialogFragment onAttach called.");
        super.onAttach(activity);
        try {
            file_rename_callback = (FileRenameCallback)activity;
            old_file_name = ((MainActivity)activity).getOldFileName();
        } catch (ClassCastException e) {
            DebugLog.i(e.getMessage());
        }
    }

    public interface FileRenameCallback {
        public void onFileRename(String new_file_name);
    }
}
