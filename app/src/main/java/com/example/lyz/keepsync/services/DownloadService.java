package com.example.lyz.keepsync.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.example.lyz.keepsync.R;
import com.example.lyz.keepsync.ui.MainActivity;
import com.example.lyz.keepsync.utils.DebugLog;
import com.example.lyz.keepsync.AppConfig;
import com.example.lyz.keepsync.utils.KeepSyncApplication;
import com.example.lyz.keepsync.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by lyz on 15-1-13.
 * Bind this service while downloading file
 */
public class DownloadService extends Service implements Handler.Callback {

    private IBinder download_binder = new DownloadBinder();
    private String file_name;
    private String file_parent_path;
    private HandlerThread download_handler_thread;
    private HandlerThread notification_handler_thread;
    private Handler download_service_handler;
    private Handler notification_handler;
    private DownloadServiceCallback download_callback;
    private NotificationManager notification_manager;
    private NotificationCompat.Builder builder;

    @Override
    public void onCreate() {
        super.onCreate();
        DebugLog.i("Download Service onCreate called.");
        download_handler_thread = new HandlerThread("DownloadServiceBackground");
        download_handler_thread.start();
        download_service_handler = new Handler(download_handler_thread.getLooper(), this);

        notification_handler_thread = new HandlerThread("NotificationServiceBackground");
        notification_handler_thread.start();
        notification_handler = new Handler(notification_handler_thread.getLooper(), this);
    }

    public void startDownloadingFile() {
        download_service_handler.sendEmptyMessage(AppConfig.DOWNLOAD_MSG_ID);
    }

    public void setCallback(DownloadServiceCallback callback) {
        download_callback = callback;
    }

    @Override
    public IBinder onBind(Intent intent) {
        DebugLog.i("Download service onBind called.");
//        file_name = intent.getDataString();
        file_name = intent.getStringExtra(AppConfig.DOWNLOAD_FILE_NAME_KEY);
        file_parent_path = intent.getStringExtra(AppConfig.DOWNLOAD_FILE_PARENT_PATH_KEY);
        return download_binder;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case AppConfig.DOWNLOAD_MSG_ID:
                downloadFile();
                return true;
            case AppConfig.DOWNLOAD_NOTIFICATION_MSG_ID:
                builder.setProgress(msg.arg2, msg.arg1, false);
                if(msg.arg1 > 0 && msg.arg1 < msg.arg2)
                    builder.setContentText(Utils.bytesToReadableString(msg.arg1) + " / " + Utils.bytesToReadableString(msg.arg2));
                else if(msg.arg1 > 0 && msg.arg1 == msg.arg2)
                    builder.setContentText("Download complete.");
                notification_manager.notify(AppConfig.DOWNLOAD_NOTIFY_ID, builder.build());
                return true;
            default:
                return false;
        }
    }

    private void downloadFile() {
        File local_file = new File(KeepSyncApplication.file_path_dir.getPath() + file_parent_path + file_name);
        DebugLog.i(local_file.getPath());
        FileOutputStream file_output_stream;
        try {
            if(local_file.exists() || local_file.getParentFile().exists() || (!local_file.getParentFile().exists() && local_file.getParentFile().mkdirs())) {
                file_output_stream = new FileOutputStream(local_file);

                notification_manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                builder = new NotificationCompat.Builder(this)
                        .setContentTitle("Downloading " + file_name)
                        .setTicker("Start downloading " + file_name)
                        .setSmallIcon(R.drawable.ic_download)
                        .setAutoCancel(true);

                Message.obtain(notification_handler, AppConfig.DOWNLOAD_NOTIFICATION_MSG_ID, 0, 0).sendToTarget();

                ProgressListener progress_listener = new ProgressListener() {
                    @Override
                    public void onProgress(long l, long l2) {
                        Message message = notification_handler.obtainMessage(AppConfig.DOWNLOAD_NOTIFICATION_MSG_ID, (int)l, (int)l2);
                        notification_handler.sendMessage(message);
                    }
                };
                KeepSyncApplication.is_downloading = true;
                KeepSyncApplication.shared_preferences.edit().putString(file_parent_path + file_name, "");
                DropboxAPI.DropboxFileInfo dbx_file_info = MainActivity.dropbox_api.getFile(
                        file_parent_path + file_name,
                        null,
                        file_output_stream,
                        progress_listener
                );

                DebugLog.i("Downloaded file's rev: " + dbx_file_info.getMetadata().rev);
                Message.obtain(notification_handler, AppConfig.DOWNLOAD_NOTIFICATION_MSG_ID, 100, 100).sendToTarget();
                file_output_stream.flush();
                file_output_stream.close();
                KeepSyncApplication.shared_preferences.edit().putString(file_parent_path + file_name, dbx_file_info.getMetadata().rev).apply();
                download_callback.downloadCompleted(dbx_file_info.getMetadata().fileName(), dbx_file_info.getMimeType());
            }
        } catch (DropboxException | IOException e) {
            DebugLog.e(e.getMessage());
            download_callback.downloadFailed();
        }
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        DebugLog.i("Download service onUnbind called.");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(download_handler_thread != null) {
            download_handler_thread.getLooper().quit();
            DebugLog.i("Download service's download handler thread quit.");
        }

        if(notification_handler_thread != null) {
            notification_handler_thread.getLooper().quit();
            DebugLog.i("Download service's notification handler thread quit.");
        }
        DebugLog.i("Download service onDestroy called.");
        KeepSyncApplication.is_downloading = false;
        notification_manager.cancel(AppConfig.DOWNLOAD_NOTIFY_ID);
    }

    public interface DownloadServiceCallback {
        public void downloadCompleted(String param_file_name, String param_mime_type);
        public void downloadFailed();
    }
}
