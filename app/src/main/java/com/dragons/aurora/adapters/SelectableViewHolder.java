package com.dragons.aurora.adapters;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.dragons.aurora.R;
import com.dragons.aurora.model.FavouriteItem;

public class SelectableViewHolder extends RecyclerView.ViewHolder {
    public RelativeLayout viewForeground;
    public View view;
    RelativeLayout viewBackground;
    FavouriteItem favouriteItem;
    OnItemSelectedListener itemSelectedListener;
    ImageView AppIcon;
    TextView AppTitle;
    TextView AppVersion;
    TextView AppExtra;
    CheckBox AppCheckbox;

    SelectableViewHolder(View view, OnItemSelectedListener itemSelectedListener) {
        super(view);
        this.view = view;
        this.itemSelectedListener = itemSelectedListener;
        AppIcon = view.findViewById(R.id.app_icon);
        AppTitle = view.findViewById(R.id.app_title);
        AppVersion = view.findViewById(R.id.app_version);
        AppExtra = view.findViewById(R.id.app_extra);
        AppCheckbox = view.findViewById(R.id.app_checkbox);
        viewBackground = view.findViewById(R.id.view_background);
        viewForeground = view.findViewById(R.id.view_foreground);

        AppCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setChecked(isChecked);
            itemSelectedListener.onItemSelected(favouriteItem);
        });
    }

    public void setChecked(boolean value) {
        favouriteItem.setSelected(value);
        AppCheckbox.setChecked(value);
    }

    public interface OnItemSelectedListener {
        void onItemSelected(FavouriteItem favouriteItem);
    }
}
