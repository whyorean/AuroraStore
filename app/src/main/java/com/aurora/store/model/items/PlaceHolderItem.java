package com.aurora.store.model.items;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aurora.store.R;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlaceHolderItem extends AbstractItem<PlaceHolderItem.ViewHolder> {

    @Override
    public int getLayoutRes() {
        return R.layout.item_loading;
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

    public static class ViewHolder extends FastAdapter.ViewHolder<PlaceHolderItem> {
        @BindView(R.id.progress_bar)
        ProgressBar progressBar;
        @BindView(R.id.line1)
        TextView line1;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void bindView(@NotNull PlaceHolderItem item, @NotNull List<?> list) {
            progressBar.setIndeterminate(true);
        }

        @Override
        public void unbindView(@NotNull PlaceHolderItem item) {
            progressBar.setIndeterminate(false);
            line1.setText(null);
        }
    }
}
