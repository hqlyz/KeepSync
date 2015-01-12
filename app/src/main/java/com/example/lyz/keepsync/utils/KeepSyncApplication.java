package com.example.lyz.keepsync.utils;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.example.lyz.keepsync.AppConfig;

import java.io.File;

/**
 * Created by lyz on 15-1-9.
 */
public class KeepSyncApplication extends Application {

    public static SharedPreferences shared_preferences;
    public static boolean is_downloading;
    public static boolean is_uploading;
    public static File file_path_dir;
    public static Resources app_resources;

    @Override
    public void onCreate() {
        super.onCreate();
        shared_preferences = getSharedPreferences(AppConfig.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        is_downloading = false;
        is_uploading = false;
        file_path_dir = getExternalFilesDir(null);
        app_resources = getResources();
    }
}
