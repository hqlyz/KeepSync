package com.example.lyz.keepsync.adapters;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.example.lyz.keepsync.R;
import com.example.lyz.keepsync.utils.KeepSyncApplication;

import java.util.List;

/**
 * Created by lyz on 2015/1/17.
 *
 */
public class JumpParentDirectoryAdapter extends ArrayAdapter<String> {

    private Context context;
    private int resource_id;
    private List<String> items;

    public JumpParentDirectoryAdapter(Context context, int resource_id, List<String> items) {
        super(context, resource_id, items);
        this.context = context;
        this.resource_id = resource_id;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource_id, null);
        }

        ImageView jump_parent_dir_image_view = (ImageView)convertView.findViewById(R.id.jump_parent_imageview);
        jump_parent_dir_image_view.setImageDrawable(KeepSyncApplication.app_resources.getDrawable(R.drawable.ic_upload));

        return convertView;
    }
}
