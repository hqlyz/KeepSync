package com.example.lyz.keepsync;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.example.lyz.keepsync.ui.MainActivity;
import com.example.lyz.keepsync.utils.DebugLog;

import java.util.List;

/**
 * Created by lyz on 15-1-16.
 *
 */
public class FileLoader extends AsyncTaskLoader<List<DropboxAPI.Entry>> {

    private String path;

    public FileLoader(Context context, String path) {
        super(context);
        this.path = path;
    }

    @Override
    public List<DropboxAPI.Entry> loadInBackground() {
        try {
            DebugLog.i("File loader is working.");
            return MainActivity.dropbox_api.metadata(
                    path,
                    0,
                    null,
                    true,
                    null
            ).contents;
        } catch (DropboxException e) {
            DebugLog.i("File loader occurred error.");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    public void deliverResult(List<DropboxAPI.Entry> data) {
        super.deliverResult(data);
    }
}
