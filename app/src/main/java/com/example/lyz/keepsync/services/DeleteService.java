package com.example.lyz.keepsync.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;

import com.dropbox.client2.exception.DropboxException;
import com.example.lyz.keepsync.AppConfig;
import com.example.lyz.keepsync.ui.MainActivity;
import com.example.lyz.keepsync.utils.DebugLog;
import com.example.lyz.keepsync.utils.KeepSyncApplication;

import java.io.File;

/**
 * Created by lyz on 15-1-12.
 *
 */
public class DeleteService extends Service implements Handler.Callback {

    private IBinder delete_binder = new DeleteBinder();
    private DeleteServiceCallback delete_callback;
    private HandlerThread handler_thread;
    private Handler service_handler;
    private String file_dir_path;
    private String file_dir_name;
    private boolean is_delete_remote_success = false;
    private boolean is_delete_local_success = true;
    private boolean is_delete_shared_preferences = false;

    public void deleteSelectedFileDir() {
        service_handler.sendEmptyMessage(AppConfig.DELETE_MSG_ID);
    }

    public void setCallback(DeleteServiceCallback callback) {
        this.delete_callback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler_thread = new HandlerThread("DeleteServiceBackground");
        handler_thread.start();
        service_handler = new Handler(handler_thread.getLooper(), this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        file_dir_path = intent.getStringExtra(AppConfig.DELETED_FILE_PATH_KEY);
        file_dir_name = intent.getStringExtra(AppConfig.DELETED_FILE_NAME_KEY);
        DebugLog.i("DeleteService onBind called.");
        return delete_binder;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case AppConfig.DELETE_MSG_ID:
                deleteFileDir(file_dir_path, file_dir_name);
                return true;
            default:
                return false;
        }
    }

    private void deleteFileDir(String path, String name) {
        // delete remote file or dir
        deleteRemoteFileDir(path);

        // delete local file or dir
        deleteLocalFileDir(name);

        // delete corresponding shared preferences
        deleteCorrespondingSharedPreferences(name);

        if(is_delete_remote_success && is_delete_local_success && is_delete_shared_preferences)
            delete_callback.deleteCompleted();
        else if(!is_delete_remote_success)
            delete_callback.deleteFailed("Delete remote file failed.");
        else if(!is_delete_local_success)
            delete_callback.deleteFailed("Delete local file failed.");
        else
            delete_callback.deleteFailed("Delete shared preferences failed.");
    }

    private void deleteRemoteFileDir(String path) {
        try {
            MainActivity.dropbox_api.delete(path);
            is_delete_remote_success = true;
        } catch (DropboxException e) {
            DebugLog.e(e.getMessage());
        }
    }

    private void deleteLocalFileDir(String name) {
        File local_file = new File(KeepSyncApplication.file_path_dir, name);
        if(local_file.exists() && !local_file.delete()) {
            is_delete_local_success = false;
        }
    }

    private void deleteCorrespondingSharedPreferences(String name) {
        if(KeepSyncApplication.shared_preferences.edit().remove(name).commit())
            is_delete_shared_preferences = true;
    }

    public class DeleteBinder extends Binder {
        public DeleteService getService() {
            return DeleteService.this;
        }
    }

    public interface DeleteServiceCallback {
        public void deleteCompleted();

        public void deleteFailed(String message);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        DebugLog.i("DeleteService onUnbind called.");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(handler_thread != null) {
            handler_thread.getLooper().quit();
            DebugLog.i("Delete Service's handler thread quit.");
        }
        DebugLog.i("DeleteService onDestroy called.");
    }
}
