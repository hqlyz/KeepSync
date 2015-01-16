package com.example.lyz.keepsync;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.example.lyz.keepsync.utils.KeepSyncApplication;
import com.example.lyz.keepsync.utils.Utils;

import java.util.List;

/**
 * Created by lyz on 2015/1/8.
 */
public class DbxFileListAdapter extends ArrayAdapter<DropboxAPI.Entry> {

    private Context context;
    private int resource;
    private List<DropboxAPI.Entry> lists;
    private int show_mask;

    public DbxFileListAdapter(Context context, int resource, List<DropboxAPI.Entry> lists, int show_mask) {
        super(context, resource, lists);
        this.context = context;
        this.resource = resource;
        this.lists = lists;
        this.show_mask = show_mask;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource, null);
        }

        TextView file_name_textview = (TextView)convertView.findViewById(R.id.file_name_textview);
        TextView modify_date_textview = (TextView)convertView.findViewById(R.id.modify_date_textview);
        TextView file_size_textview = (TextView)convertView.findViewById(R.id.file_size_textview);
        ImageView file_dir_imageview = (ImageView)convertView.findViewById(R.id.file_dir_image);

        DropboxAPI.Entry entry = lists.get(position);
        if(show_mask == AppConfig.SHOW_ALL) {
            file_name_textview.setText(Utils.formatFileName(entry.fileName()));
            modify_date_textview.setText(Utils.dateToFormatString(RESTUtility.parseDate(entry.modified)));
            file_size_textview.setText(entry.size);
            file_dir_imageview.setImageDrawable(KeepSyncApplication.app_resources.getDrawable(R.drawable.ic_file_document));
        } else if(show_mask == AppConfig.SHOW_DIRECTORY_ONLY) {
            if(entry.isDir) {
                file_name_textview.setText(Utils.formatFileName(entry.fileName()));
                modify_date_textview.setText(Utils.dateToFormatString(RESTUtility.parseDate(entry.modified)));
                file_dir_imageview.setImageDrawable(KeepSyncApplication.app_resources.getDrawable(R.drawable.ic_file_document));
            }
        }

        return convertView;
    }
}
