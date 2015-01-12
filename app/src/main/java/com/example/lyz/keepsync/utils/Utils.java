package com.example.lyz.keepsync.utils;

import com.example.lyz.keepsync.AppConfig;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
}
