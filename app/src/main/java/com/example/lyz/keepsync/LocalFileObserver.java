package com.example.lyz.keepsync;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.FileObserver;

import com.example.lyz.keepsync.services.LocalFileObserverService;
import com.example.lyz.keepsync.services.UploadIntentService;
import com.example.lyz.keepsync.utils.DebugLog;

import java.io.File;

/**
 * Created by lyz on 15-1-9.
 * When the specified file was modified, upload it to the cloud automatically.
 *
 */
public class LocalFileObserver extends FileObserver {

    private Context context;
    private String path;
    private String file_name;

    public LocalFileObserver(Context context, String path, String file_name) {
        super(path);
        this.context = context;
        this.path = path;
        this.file_name = file_name;
    }

    @Override
    public void onEvent(int event, String param_file_name) {
        switch (event) {
            case FileObserver.MODIFY:
            case FileObserver.MOVED_TO:
                if(this.file_name.equals(param_file_name)) {
                    DebugLog.i("The file was modified.");
                    Intent upload_intent = new Intent(context, UploadIntentService.class);
                    File uploaded_file = new File(this.path + "/" + param_file_name);
                    upload_intent.setData(Uri.fromFile(uploaded_file));
                    context.startService(upload_intent);
                    DebugLog.i("Start uploading service.");
                    ((LocalFileObserverService)context).stopSelf();
                }
                break;
        }
    }
}
