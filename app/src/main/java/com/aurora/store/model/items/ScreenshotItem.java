package com.aurora.store.model.items;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ScreenshotItem extends AbstractItem<ScreenshotItem.ViewHolder> {

    private String url;
    private boolean isLarge;

    public ScreenshotItem(String url) {
        this.url = url;
        this.isLarge = false;
    }

    public ScreenshotItem(String url, boolean isLarge) {
        this.url = url;
        this.isLarge = isLarge;
    }

    @Override
    public int getLayoutRes() {
        if (isLarge)
            return R.layout.item_screenshots_big;
        else
            return R.layout.item_screenshots_small;
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

    public static class ViewHolder extends FastAdapter.ViewHolder<ScreenshotItem> {
        @BindView(R.id.screenshot_img)
        ImageView imageView;

        private Context context;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull ScreenshotItem item, @NotNull List<?> list) {
            if (item.isLarge) {

            } else {
                GlideApp.with(context)
                        .load(item.url).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .transition(new DrawableTransitionOptions().crossFade())
                        .transforms(new CenterCrop(), new RoundedCorners(25))
                        .into(new SimpleTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable drawable, @Nullable Transition<? super Drawable> transition) {
                                imageView.getLayoutParams().width = drawable.getIntrinsicWidth();
                                imageView.getLayoutParams().height = drawable.getIntrinsicHeight();
                                imageView.setImageDrawable(drawable);
                            }
                        });
            }
        }

        @Override
        public void unbindView(@NotNull ScreenshotItem item) {

        }
    }
}
