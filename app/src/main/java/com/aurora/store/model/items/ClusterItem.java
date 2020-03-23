package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.util.PackageUtil;
import com.aurora.store.util.Util;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClusterItem extends AbstractItem<ClusterItem.ViewHolder> {

    private App app;
    private String packageName;

    public ClusterItem(App app) {
        this.app = app;
        this.packageName = app.getPackageName();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_cluster;
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getType() {
        return R.id.fastadapter_item;
    }

    class ViewHolder extends FastAdapter.ViewHolder<ClusterItem> {
        @BindView(R.id.img_icon)
        ImageView imgIcon;
        @BindView(R.id.app_name)
        TextView txtName;
        @BindView(R.id.app_size)
        TextView txtSize;
        @BindView(R.id.txt_indicator)
        TextView txtIndicator;

        private Context context;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull ClusterItem item, @NotNull List<?> list) {
            final App app = item.getApp();
            txtName.setText(app.getDisplayName());
            txtSize.setText(Util.humanReadableByteValue(app.getSize(), true));
            txtIndicator.setVisibility(PackageUtil.isInstalled(context, app)
                    ? View.VISIBLE
                    : View.GONE);

            GlideApp
                    .with(context)
                    .asBitmap()
                    .load(app.getIconUrl())
                    .placeholder(R.color.colorTransparent)
                    .transition(new BitmapTransitionOptions().crossFade())
                    .transforms(new CenterCrop(), new RoundedCorners(50))
                    .into(imgIcon);
        }

        @Override
        public void unbindView(@NotNull ClusterItem item) {
            txtName.setText(null);
            txtSize.setText(null);
            txtIndicator.setText(null);
            imgIcon.setImageDrawable(null);
        }
    }
}
