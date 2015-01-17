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
        old_file_path = intent.getStringExtra(AppConfig.OLD_FILE_PATH_KEY);
        new_file_path = intent.getStringExtra(AppConfig.NEW_FILE_PATH_KEY);
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
            MainActivity.dropbox_api.move(param_old_path, param_new_path);
            move_file_callback.moveFileCompleted();
        } catch (DropboxException e) {
            e.printStackTrace();
            move_file_callback.moveFileFailed();
        }
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
