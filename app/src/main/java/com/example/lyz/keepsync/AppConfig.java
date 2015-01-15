package com.example.lyz.keepsync;

/**
 * Created by lyz on 15-1-8.
 * App's configuration
 */
public class AppConfig {

    public static final String APP_KEY = "6mc1s35xiu2gqu6";

    public static final String APP_SECRET = "gvwe082e7hx6w4f";

    public static final String SHARED_PREFERENCES_NAME = "com.example.lyz.keepsync.SHARED_PREFERENCES";

    public static final String ACCESS_TOKEN_KEY = "token_key";

    public static final int UPLOAD_MSG_ID = 0x10;

    public static final int UPLOAD_NOTIFY_ID = 0x11;

    public static final int DOWNLOAD_MSG_ID = 0x20;

    public static final int DOWNLOAD_NOTIFICATION_MSG_ID = 0x21;

    public static final int DOWNLOAD_NOTIFY_ID = 0x22;

    public static final int FILE_OBSERVER_MSG_ID = 0x30;

    public static final int FILE_OBSERVER_KILL_SELF_MSG_ID = 0x31;

    public static final int DELETE_MSG_ID = 0x40;

    public static final int UPDATE_LISTVIEW_MSG_ID = 0x50;

    public static final int OPEN_FILE_MSG_ID = 0x51;

    public static final int FILE_RENAME_MSG_ID = 0x60;

    public static final int OPEN_FILE_REQUEST_CODE = 0x100;

    public static final int MAX_FILE_NAME_LENGTH = 20;

    public static final long GB_BYTE = 1024 * 1024 * 1024;

    public static final long MB_BYTE = 1024 * 1024;

    public static final long KB_BYTE = 1024;

    public static final String DBX_PATH_ROOT = "/";

    public static final String DELETED_FILE_PATH_KEY = "deleted_path";

    public static final String DELETED_FILE_NAME_KEY = "deleted_name";

    public static final String OLD_FILE_NAME_KEY = "old_file_name";

    public static final String NEW_FILE_NAME_KEY = "new_file_name";
}
