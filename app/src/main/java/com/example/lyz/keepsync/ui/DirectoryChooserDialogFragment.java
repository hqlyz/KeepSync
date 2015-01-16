package com.example.lyz.keepsync.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.Bundle;
import android.preference.DialogPreference;

import com.dropbox.client2.DropboxAPI;
import com.example.lyz.keepsync.AppConfig;
import com.example.lyz.keepsync.DbxFileListAdapter;
import com.example.lyz.keepsync.FileLoader;
import com.example.lyz.keepsync.utils.DebugLog;

import java.util.List;

/**
 * Created by lyz on 15-1-16.
 *
 */
public class DirectoryChooserDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<List<DropboxAPI.Entry>> {

    private String current_path;
    AlertDialog.Builder builder;
    DbxFileListAdapter dbx_file_list_adapter;

    public static DirectoryChooserDialogFragment newInstance(String path) {
        DirectoryChooserDialogFragment directory_chooser_dialog_fragment = new DirectoryChooserDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AppConfig.CURRENT_PATH, path);
        directory_chooser_dialog_fragment.setArguments(bundle);
        return directory_chooser_dialog_fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            current_path = savedInstanceState.getString(AppConfig.CURRENT_PATH);
        } else {
            current_path = AppConfig.DBX_PATH_ROOT;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        builder.setTitle("Choose Directory")
                .setPositiveButton("Choose", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DebugLog.i("Positive button clicked.");
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DebugLog.i("Negative button clicked.");
                    }
                });

        return builder.create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(AppConfig.GET_FILE_LIST_LOADER_ID, null, this);
    }

    @Override
    public Loader<List<DropboxAPI.Entry>> onCreateLoader(int id, Bundle args) {
        return new FileLoader(getActivity(), current_path);
    }

    @Override
    public void onLoadFinished(Loader<List<DropboxAPI.Entry>> loader, List<DropboxAPI.Entry> data) {

    }

    @Override
    public void onLoaderReset(Loader<List<DropboxAPI.Entry>> loader) {

    }
}
