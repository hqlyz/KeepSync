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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by lyz on 2015/1/17.
 *
 */
public class MoveFileService extends Service implements Handler.Callback {

    private IBinder move_file_binder = new MoveFileBinder();
    private HandlerThread move_file_handler_thread;
    private Handler move_file_handler;
    private String old_file_path;
    private String new_file_path;
    private MoveFileCallback move_file_callback;

    public void startMoving() {
        DebugLog.i("Start moving.");
        move_file_handler.sendEmptyMessage(AppConfig.MOVE_FILE_MSG_ID);
    }

    public void setCallback(MoveFileCallback callback) {
        move_file_callback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        move_file_handler_thread = new HandlerThread("MoveFileBackground");
        move_file_handler_thread.start();
        move_file_handler = new Handler(move_file_handler_thread.getLooper(), this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        old_file_path = intent.getStringExtra(AppConfig.MOVE_OLD_FILE_PATH_KEY);
        new_file_path = intent.getStringExtra(AppConfig.MOVE_NEW_FILE_PATH_KEY);
        return move_file_binder;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case AppConfig.MOVE_FILE_MSG_ID:
                moveFile(old_file_path, new_file_path);
                return true;
            default:
                DebugLog.i("hello world");
                return false;
        }
    }

    private void moveFile(String param_old_path, String param_new_path){
        try {
            // Move remote file
            DropboxAPI.Entry new_entry = MainActivity.dropbox_api.move(param_old_path, param_new_path);

            // Move local file
            moveLocalFile(param_old_path, param_new_path);
            
            // Edit shared preferences
            editSharedPreferences(param_old_path, param_new_path, new_entry);

            move_file_callback.moveFileCompleted();
        } catch (DropboxException e) {
            e.printStackTrace();
            move_file_callback.moveFileFailed();
        }
    }

    private void moveLocalFile(String param_old_path, String param_new_path) {
        File local_old_file = new File(KeepSyncApplication.file_path_dir.getPath() + param_old_path);
        if(!local_old_file.exists())
            return;

        File local_new_file = new File(KeepSyncApplication.file_path_dir.getPath() + param_new_path);
        if(local_new_file.getParentFile().exists() || (!local_new_file.getParentFile().exists() && local_new_file.getParentFile().mkdirs())) {
            try {
                FileInputStream file_input_stream = new FileInputStream(local_old_file);
                FileOutputStream file_out_stream = new FileOutputStream(local_new_file);
                byte[] buffer = new byte[1024];
                int length;
                while((length = file_input_stream.read(buffer)) > 0) {
                    file_out_stream.write(buffer, 0, length);
                }

                file_input_stream.close();
                file_out_stream.flush();
                file_out_stream.close();

                local_old_file.delete();
            } catch (IOException e) {
                DebugLog.e(e.getMessage());

            }
        }
    }

    public void editSharedPreferences(String param_old_path, String param_new_path, DropboxAPI.Entry param_new_entry) {
        if(!KeepSyncApplication.shared_preferences.contains(param_old_path))
            return;

        KeepSyncApplication.shared_preferences.edit().remove(param_old_path).commit();
        KeepSyncApplication.shared_preferences.edit().putString(param_new_path, param_new_entry.rev).apply();
    }

    public class MoveFileBinder extends Binder {
        public MoveFileService getService() {
            return MoveFileService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(move_file_handler_thread != null) {
            move_file_handler_thread.getLooper().quit();
            DebugLog.i("Move file handler thread quit.");
        }
    }

    public interface MoveFileCallback {
        public void moveFileCompleted();
        public void moveFileFailed();
    }
}
