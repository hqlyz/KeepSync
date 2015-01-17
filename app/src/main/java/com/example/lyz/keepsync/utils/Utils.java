package com.example.lyz.keepsync.utils;

import com.dropbox.client2.DropboxAPI;
import com.example.lyz.keepsync.AppConfig;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by lyz on 2015/1/8.
 * Some utility methods for other classes
 */
public class Utils {

    public static String dateToFormatString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }

    public static String formatFileName(String origin_file_name) {
        if(origin_file_name.length() <= AppConfig.MAX_FILE_NAME_LENGTH)
            return origin_file_name;

        return origin_file_name.substring(0, AppConfig.MAX_FILE_NAME_LENGTH - 3) + "...";
    }

    public static String bytesToReadableString(int bytes) {
        DecimalFormat decimal_format = new DecimalFormat("0.00");
        decimal_format.setRoundingMode(RoundingMode.HALF_EVEN);
        if(bytes >= AppConfig.GB_BYTE) {
            return decimal_format.format((double)bytes / (double)AppConfig.GB_BYTE) + "GB";
        } else if (bytes >= AppConfig.MB_BYTE) {
            return decimal_format.format((double)bytes / (double)AppConfig.MB_BYTE) + "MB";
        } else if (bytes >= AppConfig.KB_BYTE) {
            return decimal_format.format((double)bytes / (double)AppConfig.KB_BYTE) + "KB";
        } else {
            return bytes <= 1 ? bytes + "byte" : bytes + "bytes";
        }
    }

    public static List<DropboxAPI.Entry> filterFileList(List<DropboxAPI.Entry> origin_list, int mask) {
        if(mask == AppConfig.SHOW_DIRECTORY_ONLY) {
            List<DropboxAPI.Entry> filtered_list = new ArrayList<>();
            DropboxAPI.Entry origin_entry;
            for(int i = 0; i < origin_list.size(); ++i) {
                origin_entry = origin_list.get(i);
                if(origin_entry.isDir)
                    filtered_list.add(origin_entry);
            }
            return filtered_list;
        } else {
            return origin_list;
        }
    }

    public static String combineCurrentPath(ArrayList<String> path_array) {
        if(path_array == null || path_array.size() == 1)
            return AppConfig.DBX_PATH_ROOT;
        String result_path = AppConfig.DBX_PATH_ROOT;
        for(int i = 1; i < path_array.size(); ++i)
            result_path += path_array.get(i) + "/";
        return result_path;
    }
}
