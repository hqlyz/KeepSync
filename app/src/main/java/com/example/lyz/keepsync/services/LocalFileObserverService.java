package com.example.lyz.keepsync.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;

import com.example.lyz.keepsync.AppConfig;
import com.example.lyz.keepsync.utils.DebugLog;
import com.example.lyz.keepsync.LocalFileObserver;
import com.example.lyz.keepsync.utils.KeepSyncApplication;

/**
 * Created by lyz on 15-1-9.
 * Keep watching local file system with specified path and file name.
 * Stop watching when all done. (Will called by LocalFileObserver)
 */
public class LocalFileObserverService extends Service implements Handler.Callback {

    private HandlerThread handler_thread;
    private LocalFileObserver local_file_observer;
    private String file_name;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        file_name = intent.getDataString();
        handler_thread = new HandlerThread("BackgroundFileObserver");
        handler_thread.start();
        Handler service_handler = new Handler(handler_thread.getLooper(), this);
        service_handler.sendEmptyMessage(AppConfig.FILE_OBSERVER_MSG_ID);
        service_handler.sendEmptyMessageDelayed(AppConfig.FILE_OBSERVER_KILL_SELF_MSG_ID, 1000 * 60 * 10);
        DebugLog.i("File Observer Service onStartCommand called.");

        return START_STICKY;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case AppConfig.FILE_OBSERVER_MSG_ID:
                local_file_observer = new LocalFileObserver(this, KeepSyncApplication.file_path_dir.getPath(), file_name);
                local_file_observer.startWatching();
                DebugLog.i("Start watching " + KeepSyncApplication.file_path_dir.getPath());
                return true;
            case AppConfig.FILE_OBSERVER_KILL_SELF_MSG_ID:
                stopSelf();
                return true;
            default:
                DebugLog.w("Not defined message.");
                return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(local_file_observer != null) {
            local_file_observer.stopWatching();
            if(handler_thread != null) {
                handler_thread.quit();
                DebugLog.i("File observer service destroyed.");
            }
        }
    }
}
