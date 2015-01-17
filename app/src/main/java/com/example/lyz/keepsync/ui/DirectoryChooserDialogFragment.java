package com.example.lyz.keepsync.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.dropbox.client2.DropboxAPI;
import com.example.lyz.keepsync.AppConfig;
import com.example.lyz.keepsync.adapters.DbxFileListAdapter;
import com.example.lyz.keepsync.FileLoader;
import com.example.lyz.keepsync.adapters.JumpParentDirectoryAdapter;
import com.example.lyz.keepsync.R;
import com.example.lyz.keepsync.utils.DebugLog;
import com.example.lyz.keepsync.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lyz on 15-1-16.
 *
 */
public class DirectoryChooserDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<List<DropboxAPI.Entry>> {

    private ArrayList<String> current_path_array;
    private List<DropboxAPI.Entry> filtered_dbx_file_list;
    private ListView directory_list_view;
    private ListView jump_to_parent_dir_list_view;
    private ProgressBar progress_bar;
    private DirectoryChooserCallback chooser_callback;

    public static DirectoryChooserDialogFragment newInstance(ArrayList<String> path_array) {
        DirectoryChooserDialogFragment directory_chooser_dialog_fragment = new DirectoryChooserDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(AppConfig.CURRENT_PATH, path_array);
        directory_chooser_dialog_fragment.setArguments(bundle);
        return directory_chooser_dialog_fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if(savedInstanceState != null) {
//            current_path_array = savedInstanceState.getStringArrayList(AppConfig.CURRENT_PATH);
//        } else {
            current_path_array = new ArrayList<>();
            current_path_array.add(AppConfig.DBX_PATH_ROOT);
//        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layout_inflater = getActivity().getLayoutInflater();
        View directory_chooser = layout_inflater.inflate(R.layout.directory_chooser, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        directory_list_view = (ListView) directory_chooser.findViewById(R.id.directory_list_view);
        jump_to_parent_dir_list_view = (ListView) directory_chooser.findViewById(R.id.jump_parent_dir_list_view);
        progress_bar = (ProgressBar) directory_chooser.findViewById(R.id.loading_dir_progress_bar);

        DebugLog.i("Before builder creating.");

        builder.setTitle("Choose Directory")
                .setView(directory_chooser)
                .setPositiveButton("Choose", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DebugLog.i("Positive button clicked.");
                        chooser_callback.onChooseDirectory(Utils.combineCurrentPath(current_path_array));
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

        directory_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DropboxAPI.Entry selected_entry = filtered_dbx_file_list.get(position);
                current_path_array.add(selected_entry.fileName());
                getLoaderManager().restartLoader(AppConfig.GET_FILE_LIST_LOADER_ID, null, DirectoryChooserDialogFragment.this);
            }
        });

        ArrayList<String> jump_parent_dir_array = new ArrayList<>();
        jump_parent_dir_array.add("..");
        JumpParentDirectoryAdapter jump_to_parent_dir_adapter = new JumpParentDirectoryAdapter(getActivity(), R.layout.jump_parent_directory, jump_parent_dir_array);
        jump_to_parent_dir_list_view.setAdapter(jump_to_parent_dir_adapter);
        jump_to_parent_dir_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(current_path_array.size() > 1) {
                    current_path_array.remove(current_path_array.size() - 1);
                    getLoaderManager().restartLoader(AppConfig.GET_FILE_LIST_LOADER_ID, null, DirectoryChooserDialogFragment.this);

                }
            }
        });

        getLoaderManager().initLoader(AppConfig.GET_FILE_LIST_LOADER_ID, null, this);
    }

    @Override
    public Loader<List<DropboxAPI.Entry>> onCreateLoader(int id, Bundle args) {
        DebugLog.i("current path: " + Utils.combineCurrentPath(current_path_array));
        startLoading();
        DebugLog.i("onCreateLoader called.");
        return new FileLoader(getActivity(), Utils.combineCurrentPath(current_path_array));
    }

    @Override
    public void onLoadFinished(Loader<List<DropboxAPI.Entry>> loader, List<DropboxAPI.Entry> data) {
        filtered_dbx_file_list = Utils.filterFileList(data, AppConfig.SHOW_DIRECTORY_ONLY);
        DebugLog.i("filtered_dbx_file_list has " + filtered_dbx_file_list.size() + " items.");
        DbxFileListAdapter dbx_file_list_adapter = new DbxFileListAdapter(getActivity(), R.layout.dropbox_online_file_info, filtered_dbx_file_list);
        stopLoading();
        directory_list_view.setAdapter(dbx_file_list_adapter);
        dbx_file_list_adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<DropboxAPI.Entry>> loader) {
//        filtered_dbx_file_list.clear();
//        dbx_file_list_adapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            chooser_callback = (DirectoryChooserCallback)activity;
        } catch (ClassCastException e) {
            DebugLog.e(e.getMessage());
        }
    }

    private void startLoading() {
        directory_list_view.setVisibility(View.GONE);
        progress_bar.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        directory_list_view.setVisibility(View.VISIBLE);
        progress_bar.setVisibility(View.GONE);
    }

    public interface DirectoryChooserCallback {
        public void onChooseDirectory(String new_path);
    }
}
