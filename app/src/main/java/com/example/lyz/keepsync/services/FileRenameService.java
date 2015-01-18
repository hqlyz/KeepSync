package com.example.lyz.keepsync.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.example.lyz.keepsync.AppConfig;
import com.example.lyz.keepsync.ui.MainActivity;
import com.example.lyz.keepsync.utils.DebugLog;
import com.example.lyz.keepsync.utils.KeepSyncApplication;

import java.io.File;

/**
 * Created by lyz on 15-1-14.
 *
 */
public class FileRenameService extends Service implements Handler.Callback {
    private IBinder rename_binder = new RenameBinder();
    private HandlerThread rename_handler_thread;
    private Handler rename_service_handler;
    private RenameServiceCallback rename_service_callback;
    private String old_file_name;
    private String new_file_name;
    private String current_path;

    public void startFileRename() {
        rename_service_handler.sendEmptyMessage(AppConfig.FILE_RENAME_MSG_ID);
    }

    public void setRenameServiceCallback(RenameServiceCallback rename_service_callback) {
        this.rename_service_callback = rename_service_callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        rename_handler_thread = new HandlerThread("RenameServiceBackground");
        rename_handler_thread.start();
        rename_service_handler = new Handler(rename_handler_thread.getLooper(), this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        old_file_name = intent.getStringExtra(AppConfig.RENAME_OLD_FILE_NAME_KEY);
        new_file_name = intent.getStringExtra(AppConfig.RENAME_NEW_FILE_NAME_KEY);
        current_path = intent.getStringExtra(AppConfig.CURRENT_PATH);
        return rename_binder;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case AppConfig.FILE_RENAME_MSG_ID:
                fileRename();
                return true;
            default:
                return false;
        }
    }

    private void fileRename() {
        try {
            // Rename remote file
            DropboxAPI.Entry new_entry = MainActivity.dropbox_api.move(
                    current_path + old_file_name,
                    current_path + new_file_name
            );

            // Rename local file
            File old_local_file = new File(KeepSyncApplication.file_path_dir + current_path + old_file_name);
            File new_local_file = new File(KeepSyncApplication.file_path_dir + current_path + new_file_name);
            if(old_local_file.exists() && old_local_file.renameTo(new_local_file)) {
                // Add new file name's shared preferences
                KeepSyncApplication.shared_preferences.edit().putString(current_path + new_file_name, new_entry.rev).apply();
            }
            // Remove old file name's shared preferences
            KeepSyncApplication.shared_preferences.edit().remove(current_path + old_file_name).commit();
            rename_service_callback.renameCompleted();
        } catch (DropboxException e) {
            DebugLog.e(e.getMessage());
            rename_service_callback.renameFailed();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(rename_handler_thread != null) {
            DebugLog.i("rename_handler_thread quit.");
            rename_handler_thread.quit();
        }
    }

    public class RenameBinder extends Binder {
        public FileRenameService getService() {
            return FileRenameService.this;
        }
    }

    public interface RenameServiceCallback {
        public void renameCompleted();
        public void renameFailed();
    }
}
