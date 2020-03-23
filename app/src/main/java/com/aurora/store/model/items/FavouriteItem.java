package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.util.PackageUtil;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.listeners.ClickEventHook;
import com.mikepenz.fastadapter.select.SelectExtension;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavouriteItem extends AbstractItem<FavouriteItem.ViewHolder> {
    private App app;
    private String packageName;
    private boolean checked;

    public FavouriteItem(App app) {
        this.app = app;
        this.packageName = app.getPackageName();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_checkbox;
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

    public static class ViewHolder extends FastAdapter.ViewHolder<FavouriteItem> {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;
        @BindView(R.id.line2)
        TextView line2;
        @BindView(R.id.checkbox)
        MaterialCheckBox checkBox;

        private Context context;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull FavouriteItem item, @NotNull List<?> list) {
            final App app = item.getApp();

            line1.setText(app.getDisplayName());
            line2.setText(context.getText(PackageUtil.isInstalled(context, app)
                    ? R.string.list_installed
                    : R.string.list_not_installd));
            checkBox.setChecked(item.checked);

            GlideApp
                    .with(context)
                    .load(app.getIconUrl())
                    .transition(new DrawableTransitionOptions().crossFade())
                    .transforms(new CenterCrop(), new RoundedCorners(30))
                    .into(img);

        }

        @Override
        public void unbindView(@NotNull FavouriteItem item) {
            line1.setText(null);
            line2.setText(null);
        }
    }

    public static final class CheckBoxClickEvent extends ClickEventHook<FavouriteItem> {
        @Nullable
        public View onBind(@NotNull RecyclerView.ViewHolder viewHolder) {
            return viewHolder instanceof ViewHolder
                    ? ((ViewHolder) viewHolder).checkBox
                    : null;
        }

        @Override
        public void onClick(@NotNull View view, int position, @NotNull FastAdapter<FavouriteItem> fastAdapter, @NotNull FavouriteItem item) {
            SelectExtension<FavouriteItem> selectExtension = fastAdapter.getExtension(SelectExtension.class);
            if (selectExtension != null) {
                selectExtension.toggleSelection(position);
                item.checked = !item.checked;
            }
        }
    }
}
