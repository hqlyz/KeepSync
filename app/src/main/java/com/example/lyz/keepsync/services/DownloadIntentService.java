package com.example.lyz.keepsync.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxException;
import com.example.lyz.keepsync.AppConfig;
import com.example.lyz.keepsync.utils.DebugLog;
import com.example.lyz.keepsync.ui.MainActivity;
import com.example.lyz.keepsync.R;
import com.example.lyz.keepsync.utils.KeepSyncApplication;
import com.example.lyz.keepsync.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by lyz on 15-1-8.
 *
 */
public class DownloadIntentService extends IntentService implements Handler.Callback {

    private HandlerThread handler_thread;
    private NotificationManager notification_manager;
    private NotificationCompat.Builder builder;

    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String file_name = intent.getDataString();
        File local_file = new File(KeepSyncApplication.file_path_dir, file_name);
        DebugLog.i(local_file.getPath());
        FileOutputStream file_output_stream;
        try {
            file_output_stream = new FileOutputStream(local_file);

            notification_manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            builder = new NotificationCompat.Builder(this)
                    .setContentTitle("Downloading " + file_name)
                    .setTicker("Start downloading " + file_name)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setAutoCancel(true);

            handler_thread = new HandlerThread("BackgroundDownload");
            handler_thread.start();
            final Handler notification_handler = new Handler(handler_thread.getLooper(), this);
            Message.obtain(notification_handler, AppConfig.DOWNLOAD_MSG_ID, 0, 0).sendToTarget();

            ProgressListener progress_listener = new ProgressListener() {
                @Override
                public void onProgress(long l, long l2) {
                    Message message = notification_handler.obtainMessage(AppConfig.DOWNLOAD_MSG_ID, (int)l, (int)l2);
                    notification_handler.sendMessage(message);
                }
            };
            KeepSyncApplication.is_downloading = true;
            KeepSyncApplication.shared_preferences.edit().putString(file_name, "");
            DropboxAPI.DropboxFileInfo dbx_file_info = MainActivity.dropbox_api.getFile(
                    "/" + file_name,
                    null,
                    file_output_stream,
                    progress_listener
            );

            DebugLog.i("Downloaded file's rev: " + dbx_file_info.getMetadata().rev);
            Message.obtain(notification_handler, AppConfig.DOWNLOAD_MSG_ID, 100, 100).sendToTarget();
            file_output_stream.flush();
            file_output_stream.close();
            KeepSyncApplication.shared_preferences.edit().putString(file_name, dbx_file_info.getMetadata().rev).apply();
            tryToOpenDownloadedFile(dbx_file_info);
        } catch (DropboxException | IOException e) {
            DebugLog.e(e.getMessage());
        }
    }

    private void tryToOpenDownloadedFile(DropboxAPI.DropboxFileInfo dbx_file_info) {
        File downloadedFile = new File(KeepSyncApplication.file_path_dir, dbx_file_info.getMetadata().fileName());
        Intent intent_open_file = new Intent(Intent.ACTION_VIEW);
        intent_open_file.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent_open_file.setDataAndType(Uri.fromFile(downloadedFile), dbx_file_info.getMimeType());
        if(intent_open_file.resolveActivity(getPackageManager()) != null) {
            startActivity(intent_open_file);
            startFileObserverService(dbx_file_info.getMetadata().fileName());
        } else {
            DebugLog.w("No app can open this file.");
            Toast.makeText(getApplicationContext(), "No app can open this file.", Toast.LENGTH_LONG).show();
        }
    }

    private void startFileObserverService(String file_name) {
        Intent intent_file_observer_service = new Intent(DownloadIntentService.this, LocalFileObserverService.class);
        intent_file_observer_service.setData(Uri.parse(file_name));
        startService(intent_file_observer_service);
        DebugLog.i("Start file observer service after downloading automatically.");
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case AppConfig.DOWNLOAD_MSG_ID:
                DebugLog.i("transfered: " + Utils.bytesToReadableString(msg.arg1) + "\ttotal: " + Utils.bytesToReadableString(msg.arg2));
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(handler_thread != null) {
            handler_thread.quit();
            DebugLog.i("handler_thread quit.");
        }
        DebugLog.i("Download Intent Service destroyed.");
        KeepSyncApplication.is_downloading = false;
        notification_manager.cancel(AppConfig.DOWNLOAD_NOTIFY_ID);
    }
}
