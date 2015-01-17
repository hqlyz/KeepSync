package com.example.lyz.keepsync;

/**
 * Created by lyz on 2015/1/13.
 *
 */
public class LocalFile {
    String file_name;
    String mime_type;

    public LocalFile(String file_name, String mime_type) {
        this.file_name = file_name;
        this.mime_type = mime_type;
    }

    public String getFileName() {
        return file_name;
    }

    public String getMimeType() {
        return mime_type;
    }
}
