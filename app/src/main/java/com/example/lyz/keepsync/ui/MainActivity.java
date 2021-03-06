package com.example.lyz.keepsync.ui;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.example.lyz.keepsync.AppConfig;
import com.example.lyz.keepsync.adapters.DbxFileListAdapter;
import com.example.lyz.keepsync.LocalFile;
import com.example.lyz.keepsync.services.DeleteService;
import com.example.lyz.keepsync.services.DownloadService;
import com.example.lyz.keepsync.services.FileRenameService;
import com.example.lyz.keepsync.services.MoveFileService;
import com.example.lyz.keepsync.utils.DebugLog;
import com.example.lyz.keepsync.R;
import com.example.lyz.keepsync.services.LocalFileObserverService;
import com.example.lyz.keepsync.utils.KeepSyncApplication;
import com.example.lyz.keepsync.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity
        implements DeleteService.DeleteServiceCallback, DownloadService.DownloadServiceCallback, FileRenameDialogFragment.FileRenameCallback, FileRenameService.RenameServiceCallback, DirectoryChooserDialogFragment.DirectoryChooserCallback, MoveFileService.MoveFileCallback {

    private static final int FILE_RENAME = 0;
    private static final int FILE_DELETE = 1;
    private static final int FILE_DOWNLOAD_ANYWAY = 2;
    private static final int FILE_MOVE_TO = 3;

    private ListView dropbox_online_file_list_view;
    private SwipeRefreshLayout file_swipe_refresh_layout;
    private TextView current_path_text_view;

    public static DropboxAPI<AndroidAuthSession> dropbox_api;
    private String access_token = "";
    private DbxFileListAdapter dbx_file_list_adapter;
    private List<DropboxAPI.Entry> filter_dbx_file_list;
    private ProgressDialog progress_dialog;
    private Intent intent_file_observer;
    private String remote_file_rev = "";
    private int selected_file_index = -1;
    private ArrayList<String> current_path_array;

    public String getOldFileName() {
        if(selected_file_index != -1) {
            return filter_dbx_file_list.get(selected_file_index).fileName();
        }

        return "";
    }

    private ServiceConnection delete_service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DeleteService.DeleteBinder delete_binder = (DeleteService.DeleteBinder)service;
            DeleteService delete_service = delete_binder.getService();
            delete_service.setCallback(MainActivity.this);
            delete_service.deleteSelectedFileDir();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private ServiceConnection download_service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownloadService.DownloadBinder download_binder = (DownloadService.DownloadBinder)service;
            DownloadService download_service = download_binder.getService();
            download_service.setCallback(MainActivity.this);
            download_service.startDownloadingFile();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private ServiceConnection rename_service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileRenameService.RenameBinder rename_binder = (FileRenameService.RenameBinder)service;
            FileRenameService file_rename_service = rename_binder.getService();
            file_rename_service.setRenameServiceCallback(MainActivity.this);
            file_rename_service.startFileRename();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private ServiceConnection move_file_service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MoveFileService move_file_service = ((MoveFileService.MoveFileBinder)service).getService();
            move_file_service.setCallback(MainActivity.this);
            move_file_service.startMoving();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private Handler main_thread_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AppConfig.UPDATE_LIST_VIEW_MSG_ID:
                    dbx_file_list_adapter.notifyDataSetChanged();
                    break;
                case AppConfig.OPEN_FILE_MSG_ID:
                    LocalFile local_file = (LocalFile)msg.obj;
                    openLocalFile(local_file.getFileName(), local_file.getMimeType());
                    break;
                case AppConfig.REFRESH_FILE_LIST_MSG_ID:
                    getFileList(true);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(KeepSyncApplication.shared_preferences == null) {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.init_failed), Toast.LENGTH_LONG).show();
            this.finish();
        }
        init();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        updateCurrentPath();
        getFileList(true);
    }

    private void init() {
        initView();
        initVariables();
        setupProgressDialog();
        getAccessToken();
        createDropboxApi();
    }

    private void initView() {
        dropbox_online_file_list_view = (ListView)findViewById(R.id.dropbox_online_file_list_view);
        dropbox_online_file_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DropboxAPI.Entry entry = filter_dbx_file_list.get(position);
                DebugLog.i(entry.path + "\t" + entry.parentPath());
                if(!entry.isDir) {
                    checkRemoteFileRevision(entry.fileName(), entry.mimeType);
                }
                else {
                    current_path_array.add(entry.fileName());
                    updateCurrentPath();
                    getFileList(true);
                }

            }
        });
        registerForContextMenu(dropbox_online_file_list_view);

        current_path_text_view = (TextView)findViewById(R.id.current_path_text_view);

        file_swipe_refresh_layout = (SwipeRefreshLayout)findViewById(R.id.file_swipe_refresh_layout);
        file_swipe_refresh_layout.setColorSchemeColors(
                KeepSyncApplication.app_resources.getColor(R.color.get_file_list_progress1),
                KeepSyncApplication.app_resources.getColor(R.color.get_file_list_progress2),
                KeepSyncApplication.app_resources.getColor(R.color.get_file_list_progress3)
        );
        file_swipe_refresh_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getFileList(false);
            }
        });
    }

    private void initVariables() {
        current_path_array = new ArrayList<>();
        current_path_array.add(AppConfig.DBX_PATH_ROOT);
    }

    private void getAccessToken() {
        if(KeepSyncApplication.shared_preferences != null)
            access_token = KeepSyncApplication.shared_preferences.getString(AppConfig.ACCESS_TOKEN_KEY, "");
    }

    private void setAccessToken(String param_token) {
        if(KeepSyncApplication.shared_preferences != null && !access_token.trim().equals("")) {
            KeepSyncApplication.shared_preferences.edit().putString(AppConfig.ACCESS_TOKEN_KEY, param_token).apply();
        }
    }

    private void createDropboxApi() {
        AppKeyPair key_pair = new AppKeyPair(AppConfig.APP_KEY, AppConfig.APP_SECRET);
        AndroidAuthSession auth_session;
        if(access_token.equals(""))
            auth_session = new AndroidAuthSession(key_pair);
        else
            auth_session = new AndroidAuthSession(key_pair, access_token);
        dropbox_api = new DropboxAPI<>(auth_session);
    }

    private void setupProgressDialog() {
        progress_dialog = new ProgressDialog(this);
        progress_dialog.setIndeterminate(true);
        progress_dialog.setMessage(getResources().getString(R.string.loading));
        progress_dialog.setCancelable(false);
    }



    private void getFileList(final boolean is_automatic) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(is_automatic) {
                    progress_dialog.setMessage(KeepSyncApplication.app_resources.getString(R.string.loading));
                    progress_dialog.show();
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    List<DropboxAPI.Entry> original_dbx_file_list = dropbox_api.metadata(
                            Utils.combineCurrentPath(current_path_array),
                            0,
                            null,
                            true,
                            null
                    ).contents;
                    filter_dbx_file_list = Utils.filterFileList(original_dbx_file_list, AppConfig.SHOW_ALL);
                    DebugLog.i("filter_dbx_file_list received.");
                } catch (DropboxException e) {
                    DebugLog.e(e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if(filter_dbx_file_list != null) {
                    dbx_file_list_adapter = new DbxFileListAdapter(MainActivity.this, R.layout.dropbox_online_file_info, filter_dbx_file_list);
                    dropbox_online_file_list_view.setAdapter(dbx_file_list_adapter);
                } else {
                    DebugLog.w("filter_dbx_file_list is null.");
                }
                if(file_swipe_refresh_layout.isRefreshing())
                    file_swipe_refresh_layout.setRefreshing(false);
                progress_dialog.dismiss();
            }
        }.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(access_token.equals("")) {
            dropbox_api.getSession().startOAuth2Authentication(MainActivity.this);
            if(dropbox_api.getSession().authenticationSuccessful()) {
                dropbox_api.getSession().finishAuthentication();
                access_token = dropbox_api.getSession().getOAuth2AccessToken();
                setAccessToken(access_token);
                getFileList(true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if(id == R.id.action_exit) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void downloadFile(String downloaded_file_name) {
        Toast.makeText(getApplicationContext(), "Start downloading...", Toast.LENGTH_SHORT).show();
        Intent download_intent = new Intent(MainActivity.this, DownloadService.class);
//        download_intent.setData(Uri.parse(downloaded_file_name));
        download_intent.putExtra(AppConfig.DOWNLOAD_FILE_NAME_KEY, downloaded_file_name);
        download_intent.putExtra(AppConfig.DOWNLOAD_FILE_PARENT_PATH_KEY, Utils.combineCurrentPath(current_path_array));
        bindService(download_intent, download_service_connection, BIND_AUTO_CREATE);
        DebugLog.i("Bind downloading service.");
    }

    private void openLocalFile(String file_name, String mime_type) {
        File local_file = new File(KeepSyncApplication.file_path_dir.getPath() + Utils.combineCurrentPath(current_path_array) + file_name);
        Intent intent_open_file = new Intent(Intent.ACTION_VIEW);
        intent_open_file.setDataAndType(Uri.fromFile(local_file), mime_type);
        if(intent_open_file.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent_open_file, AppConfig.OPEN_FILE_REQUEST_CODE);
            startFileObserverService(file_name);
        } else {
            DebugLog.w("No app can open this file.");
            Toast.makeText(getApplicationContext(), "No app can open this file.", Toast.LENGTH_LONG).show();
        }
    }

    private void startFileObserverService(String file_name) {
        intent_file_observer = new Intent(MainActivity.this, LocalFileObserverService.class);
        intent_file_observer.setData(Uri.parse(file_name));
        intent_file_observer.putExtra(AppConfig.CURRENT_PATH, Utils.combineCurrentPath(current_path_array));
        startService(intent_file_observer);
        DebugLog.i("Start file observer service.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == AppConfig.OPEN_FILE_REQUEST_CODE) {
            if(resultCode == RESULT_CANCELED) {
                stopFileObserverService();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // if this activity destroyed, stop the file observer service anyway.
        // Otherwise, it may cause memory leaking.
        stopFileObserverService();
    }

    private void stopFileObserverService() {
        if(intent_file_observer != null) {
            stopService(intent_file_observer);
            DebugLog.i("stopService called.");
        }
    }

    // compare local revision to the remote one while clicking list item.
    // If difference, start downloading remote one and open it automatically.
    // Otherwise, try to open local file.
    private void checkRemoteFileRevision(final String file_name, final String mime_type) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress_dialog.setMessage(KeepSyncApplication.app_resources.getString(R.string.checking_rev));
                progress_dialog.show();
            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    DropboxAPI.Entry entry = dropbox_api.metadata(
                            Utils.combineCurrentPath(current_path_array) + params[0],
                            1,
                            null,
                            false,
                            null
                    );
                    if(entry != null)
                        return entry.rev;
                    else
                        return "";
                } catch (DropboxException e) {
                    DebugLog.e(e.getMessage());
                    return "";
                }
            }

            @Override
            protected void onPostExecute(String param) {
                remote_file_rev = param;
                progress_dialog.dismiss();
                String local_rev = KeepSyncApplication.shared_preferences.getString(Utils.combineCurrentPath(current_path_array) + file_name, "");
                DebugLog.i("local: " + local_rev + "\tremote: " + remote_file_rev);
                if (local_rev.equals(remote_file_rev)) {
                    openLocalFile(file_name, mime_type);
                    DebugLog.i(file_name + " opened by other app.");
                } else {
                    if (!KeepSyncApplication.is_downloading) {
                        downloadFile(file_name);
                    } else {
                        DebugLog.w("Another file is downloading, please try it again later.");
                        Toast.makeText(MainActivity.this, "Another file is downloading, please try it again later.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }.execute(file_name);
    }

    // Callback method when delete service completed.
    @Override
    public void deleteCompleted() {
        DebugLog.i("Finally delete service completed. \\^_^/");
        unbindSpecifiedService(delete_service_connection);
        DropboxAPI.Entry deleted_entry = filter_dbx_file_list.get(selected_file_index);
        filter_dbx_file_list.remove(deleted_entry);
        main_thread_handler.sendEmptyMessage(AppConfig.UPDATE_LIST_VIEW_MSG_ID);
    }

    // Callback method while delete service failed.
    @Override
    public void deleteFailed(String message) {
        DebugLog.i("Oops, delete file failed...");
        unbindSpecifiedService(delete_service_connection);
    }

    private void unbindSpecifiedService(ServiceConnection param_service_connection) {
        unbindService(param_service_connection);
        progress_dialog.dismiss();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId() == R.id.dropbox_online_file_list_view) {
            String[] file_context_menu_items = KeepSyncApplication.app_resources.getStringArray(R.array.file_context_menu_items);
            for(int i = 0; i < file_context_menu_items.length; ++i) {
                menu.add(Menu.NONE, i, i, file_context_menu_items[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menu_info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        // deleted_entry = filter_dbx_file_list.get(menu_info.position);
        selected_file_index = menu_info.position;
        switch (item.getItemId()) {
            case FILE_RENAME:
                renameFile();
                return true;
            case FILE_DELETE:
                DropboxAPI.Entry deleted_entry = filter_dbx_file_list.get(selected_file_index);
                DebugLog.i("deleted path: " + deleted_entry.path + "\tdeleted name: " + deleted_entry.fileName());
                deleteSelectedFile(deleted_entry.path, Utils.combineCurrentPath(current_path_array) + deleted_entry.fileName());
                return true;
            case FILE_DOWNLOAD_ANYWAY:
                DropboxAPI.Entry downloaded_entry = filter_dbx_file_list.get(selected_file_index);
                downloadFile(downloaded_entry.fileName());
                return true;
            case FILE_MOVE_TO:
                moveFile();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void moveFile() {
        DirectoryChooserDialogFragment directory_chooser_dialog_fragment = DirectoryChooserDialogFragment.newInstance(current_path_array);
        directory_chooser_dialog_fragment.show(getFragmentManager(), "choose_directory_tag");
    }

    private void renameFile() {
        FileRenameDialogFragment file_rename_dialogFragment = new FileRenameDialogFragment();
        file_rename_dialogFragment.show(getFragmentManager(), "FileRename");
    }

    private void deleteSelectedFile(String param_path, String param_file_name) {
        progress_dialog.setMessage(KeepSyncApplication.app_resources.getString(R.string.deleting));
        progress_dialog.show();
        Intent intent_delete_service = new Intent(MainActivity.this, DeleteService.class);
        intent_delete_service.putExtra(AppConfig.DELETED_FILE_PATH_KEY, param_path);
        intent_delete_service.putExtra(AppConfig.DELETED_FILE_NAME_KEY, param_file_name);
        bindService(intent_delete_service, delete_service_connection, BIND_AUTO_CREATE);
    }

    // Callback method when download service completed.
    @Override
    public void downloadCompleted(String param_file_name, String param_mime_type) {
        DebugLog.i("Download service completed.");
        unbindSpecifiedService(download_service_connection);
        LocalFile local_file = new LocalFile(param_file_name, param_mime_type);
        main_thread_handler.obtainMessage(AppConfig.OPEN_FILE_MSG_ID, local_file).sendToTarget();
    }

    // Callback method while download service failed.
    @Override
    public void downloadFailed() {
        DebugLog.w("Download service failed.");
        unbindSpecifiedService(download_service_connection);
    }

    // Callback method while user confirm to rename file.
    @Override
    public void onFileRename(String new_file_name) {

        // If new file's name is the same as the old one, then return.
        DropboxAPI.Entry selected_entry = filter_dbx_file_list.get(selected_file_index);
        if(selected_entry.fileName().equals(new_file_name)) {
            DebugLog.i("The new file's name is the same as old one.");
            return;
        }

        progress_dialog.setMessage(KeepSyncApplication.app_resources.getString(R.string.renaming));
        progress_dialog.show();
        String old_file_name = selected_entry.fileName();
        Intent intent_rename_service = new Intent(MainActivity.this, FileRenameService.class);
        intent_rename_service.putExtra(AppConfig.RENAME_OLD_FILE_NAME_KEY, old_file_name);
        intent_rename_service.putExtra(AppConfig.RENAME_NEW_FILE_NAME_KEY, new_file_name);
        intent_rename_service.putExtra(AppConfig.CURRENT_PATH, Utils.combineCurrentPath(current_path_array));
        bindService(intent_rename_service, rename_service_connection, BIND_AUTO_CREATE);
    }

    // Callback method while file rename completed.
    @Override
    public void renameCompleted() {
        unbindSpecifiedService(rename_service_connection);
        DebugLog.i("File rename completed.");
//        getFileList(true);
        main_thread_handler.sendEmptyMessage(AppConfig.REFRESH_FILE_LIST_MSG_ID);
    }

    // Callback method while file rename failed.
    @Override
    public void renameFailed() {
        unbindSpecifiedService(rename_service_connection);
        DebugLog.w("File rename failed.");
    }

    // Callback method while moving file to another directory
    @Override
    public void onChooseDirectory(String new_path) {
        DropboxAPI.Entry temp_entry = filter_dbx_file_list.get(selected_file_index);
        if(temp_entry.parentPath().equals(new_path)) {
            DebugLog.i("No need to move.");
        } else {
            progress_dialog.setMessage(KeepSyncApplication.app_resources.getString(R.string.moving));
            progress_dialog.show();
            Intent intent_move_file_service = new Intent(MainActivity.this, MoveFileService.class);
            intent_move_file_service.putExtra(AppConfig.MOVE_OLD_FILE_PATH_KEY, temp_entry.path);
            intent_move_file_service.putExtra(AppConfig.MOVE_NEW_FILE_PATH_KEY, new_path + temp_entry.fileName());
            bindService(intent_move_file_service, move_file_service_connection, BIND_AUTO_CREATE);
        }
    }

    private void updateCurrentPath() {
        DebugLog.i(Utils.combineCurrentPath(current_path_array));
        for(String s : current_path_array)
            DebugLog.i(s + "\n");
        current_path_text_view.setText(KeepSyncApplication.app_resources.getString(R.string.current_path) + Utils.combineCurrentPath(current_path_array));
    }

    @Override
    public void onBackPressed() {
        if(current_path_array.size() > 1) {
            current_path_array.remove(current_path_array.size() - 1);
            updateCurrentPath();
            getFileList(true);
        } else
            super.onBackPressed();
    }

    @Override
    public void moveFileCompleted() {
        unbindSpecifiedService(move_file_service_connection);
        DebugLog.i("Move file completed.");
//        getFileList(true);
        main_thread_handler.sendEmptyMessage(AppConfig.REFRESH_FILE_LIST_MSG_ID);
    }

    @Override
    public void moveFileFailed() {
        unbindSpecifiedService(move_file_service_connection);
        DebugLog.w("Move file failed.");
    }
}
