package com.example.lyz.keepsync.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

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
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by lyz on 15-1-8.
 *
 */
public class UploadIntentService extends IntentService implements Handler.Callback {

    private HandlerThread notification_handler_thread;
    private NotificationCompat.Builder builder;
    private NotificationManager notification_manager;

    public UploadIntentService() {
        super("UploadIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        File uploaded_file = new File(intent.getData().getPath());
        String file_name = uploaded_file.getName();
        FileInputStream file_input_stream;
        DebugLog.i(uploaded_file.getPath() + "\tlength: " + uploaded_file.length());
        try {
            file_input_stream = new FileInputStream(uploaded_file);
            DebugLog.i("File Input Stream: " + file_input_stream.available());
            if(MainActivity.dropbox_api != null) {
                notification_manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                builder = new NotificationCompat.Builder(this)
                        .setContentTitle("Uploading " + file_name)
                        .setTicker("Start uploading " + file_name)
                        .setSmallIcon(R.drawable.ic_upload)
                        .setAutoCancel(true);

                notification_handler_thread = new HandlerThread("BackgroundUpload");
                notification_handler_thread.start();
                final Handler notification_handler = new Handler(notification_handler_thread.getLooper(), this);
                Message.obtain(notification_handler, AppConfig.UPLOAD_MSG_ID, 0, 0).sendToTarget();

                ProgressListener progress_listener = new ProgressListener() {
                    @Override
                    public void onProgress(long l, long l2) {
                        Message message = notification_handler.obtainMessage(AppConfig.UPLOAD_MSG_ID, (int)l, (int)l2);
                        notification_handler.sendMessage(message);
                    }
                };
                KeepSyncApplication.is_uploading = true;
                DropboxAPI.Entry response = MainActivity.dropbox_api.putFileOverwrite(
                        "/" + file_name,
                        file_input_stream,
                        uploaded_file.length(),
                        progress_listener
                );

                DebugLog.i("The uploaded file's rev is: " + response.rev);
                file_input_stream.close();
                KeepSyncApplication.shared_preferences.edit().putString(file_name, response.rev).apply();
                DebugLog.i("Shared preferences changed after uploading.");
                Message.obtain(notification_handler, AppConfig.UPLOAD_MSG_ID, 100, 100).sendToTarget();
            } else {
                DebugLog.w("dropbox_api is null.");
            }

        } catch (DropboxException | IOException e) {
            DebugLog.e(e.getMessage());
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case AppConfig.UPLOAD_MSG_ID:
                DebugLog.i("transfered: " + Utils.bytesToReadableString(msg.arg1) + "\ttotal: " + Utils.bytesToReadableString(msg.arg2));
                builder.setProgress(msg.arg2, msg.arg1, false);
                if(msg.arg1 > 0 && msg.arg1 < msg.arg2)
                    builder.setContentText(Utils.bytesToReadableString(msg.arg1) + " / " + Utils.bytesToReadableString(msg.arg2));
                else if(msg.arg1 > 0 && msg.arg1 == msg.arg2)
                    builder.setContentText("Upload complete.");
                notification_manager.notify(AppConfig.UPLOAD_NOTIFY_ID, builder.build());
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(notification_handler_thread != null) {
            notification_handler_thread.getLooper().quit();
            DebugLog.i("notification_handler_thread quit.");
        }
        DebugLog.i("Upload Intent Service destroyed.");
        KeepSyncApplication.is_uploading = false;
//        notification_manager.cancel(AppConfig.UPLOAD_NOTIFY_ID);
    }
}
