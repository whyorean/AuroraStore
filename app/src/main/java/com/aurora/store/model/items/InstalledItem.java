package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aurora.store.R;
import com.aurora.store.model.App;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class InstalledItem extends BaseItem {

    public InstalledItem(App app) {
        super(app);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_installed;
    }

    @NotNull
    @Override
    public BaseViewHolder getViewHolder(@NotNull View view) {
        return new ViewHolder(view);
    }

    public static class ViewHolder extends BaseViewHolder {

        @BindView(R.id.img_icon)
        ImageView imgIcon;
        @BindView(R.id.line1)
        TextView txtTitle;
        @BindView(R.id.line2)
        TextView txtVersion;
        @BindView(R.id.line3)
        TextView txtExtra;

        private Context context;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull BaseItem item, @NotNull List<?> list) {
            List<String> versionStringList = new ArrayList<>();
            List<String> extraStringList = new ArrayList<>();

            fillAppDetails(context, versionStringList, extraStringList, item.getApp());

            txtTitle.setText(item.getApp().getDisplayName());
            txtVersion.setText(StringUtils.join(versionStringList.toArray(), " • "));
            txtExtra.setText(StringUtils.join(extraStringList.toArray(), " • "));

            if (item.getApp().getIconDrawable() != null)
                imgIcon.setImageDrawable(item.getApp().getIconDrawable());
        }

        @Override
        public void unbindView(@NotNull BaseItem item) {
            txtTitle.setText(null);
            txtVersion.setText(null);
            txtExtra.setText(null);
            imgIcon.setImageDrawable(null);
        }

        @Override
        public void fillAppDetails(Context context, List<String> versionStringList, List<String> extraStringList, App app) {
            versionStringList.add("v" + app.getVersionName() + "." + app.getVersionCode());
            if (app.isSystem())
                extraStringList.add(context.getString(R.string.list_app_system));
            else
                extraStringList.add(context.getString(R.string.list_app_user));
        }
    }
}
