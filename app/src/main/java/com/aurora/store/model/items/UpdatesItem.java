package com.aurora.store.model.items;

import android.content.Context;
import android.text.Html;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.util.ViewUtil;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.listeners.ClickEventHook;
import com.mikepenz.fastadapter.select.SelectExtension;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Getter;
import lombok.Setter;

public class UpdatesItem extends AbstractItem<UpdatesItem.ViewHolder> {

    @Getter
    @Setter
    private App app;
    @Getter
    @Setter
    private String packageName;

    private boolean checked;
    private boolean expanded;

    public UpdatesItem(App app) {
        this.app = app;
        this.packageName = app.getPackageName();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_updates;
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getType() {
        return 0;
    }

    public static class ViewHolder extends FastAdapter.ViewHolder<UpdatesItem> {

        @BindView(R.id.img_icon)
        ImageView imgIcon;
        @BindView(R.id.line1)
        TextView txtTitle;
        @BindView(R.id.line2)
        TextView txtVersion;
        @BindView(R.id.line3)
        TextView txtExtra;
        @BindView(R.id.txt_changes)
        TextView txtChanges;
        @BindView(R.id.layout_changes)
        RelativeLayout layoutChanges;
        @BindView(R.id.img_expand)
        ImageView imgExpand;
        @BindView(R.id.checkbox)
        MaterialCheckBox checkBox;

        private Context context;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull UpdatesItem item, @NotNull List<?> list) {
            final App app = item.getApp();
            final List<String> versionStringList = new ArrayList<>();
            final List<String> extraStringList = new ArrayList<>();

            fillAppDetails(context, versionStringList, extraStringList, item.getApp());

            txtTitle.setText(app.getDisplayName());
            txtVersion.setText(StringUtils.join(versionStringList.toArray(), " • "));
            txtExtra.setText(StringUtils.join(extraStringList.toArray(), " • "));
            txtChanges.setText(app.getChanges().isEmpty()
                    ? context.getString(R.string.details_no_changes)
                    : Html.fromHtml(app.getChanges()).toString());

            GlideApp
                    .with(context)
                    .load(app.getIconUrl())
                    .transition(new DrawableTransitionOptions().crossFade())
                    .transforms(new CenterCrop(), new RoundedCorners(30))
                    .into(imgIcon);

            imgExpand.setOnClickListener(v -> {
                if (item.expanded) {
                    ViewUtil.collapse(layoutChanges);
                    ViewUtil.rotateView(imgExpand, true);
                    item.expanded = false;
                } else {
                    ViewUtil.rotateView(imgExpand, false);
                    ViewUtil.expand(layoutChanges);
                    item.expanded = true;
                }
            });
            if (item.expanded) {
                imgExpand.setRotation(180);
                layoutChanges.setVisibility(View.VISIBLE);
            } else {
                imgExpand.setRotation(0);
                layoutChanges.setVisibility(View.GONE);
            }
            checkBox.setChecked(item.checked);
        }

        @Override
        public void unbindView(@NotNull UpdatesItem item) {
            txtTitle.setText(null);
            txtVersion.setText(null);
            txtExtra.setText(null);
            txtExtra.setText(null);
            imgIcon.setImageDrawable(null);
        }

        public void fillAppDetails(Context context, List<String> versionStringList, List<String> extraStringList, App app) {
            versionStringList.add(app.getVersionName() + "." + app.getVersionCode());
            extraStringList.add(app.getUpdated());
            extraStringList.add(app.getSize() == 0 ? "N/A" : Formatter.formatShortFileSize(context, app.getSize()));
        }
    }

    public static final class CheckBoxClickEvent extends ClickEventHook<UpdatesItem> {
        @Nullable
        public View onBind(@NotNull RecyclerView.ViewHolder viewHolder) {
            return viewHolder instanceof UpdatesItem.ViewHolder
                    ? ((UpdatesItem.ViewHolder) viewHolder).checkBox
                    : null;
        }

        @Override
        public void onClick(@NotNull View view, int position, @NotNull FastAdapter<UpdatesItem> fastAdapter, @NotNull UpdatesItem item) {
            SelectExtension<UpdatesItem> selectExtension = fastAdapter.getExtension(SelectExtension.class);
            if (selectExtension != null) {
                selectExtension.toggleSelection(position);
                item.checked = !item.checked;
            }
        }
    }
}
