package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.dragons.aurora.playstoreapiv2.SearchSuggestEntry;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Getter;

public class SearchSuggestionItem extends AbstractItem<SearchSuggestionItem.ViewHolder> {

    @Getter
    private SearchSuggestEntry suggestEntry;

    public SearchSuggestionItem(SearchSuggestEntry suggestEntry) {
        this.suggestEntry = suggestEntry;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_suggestion;
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

    public static class ViewHolder extends FastAdapter.ViewHolder<SearchSuggestionItem> {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;

        private Context context;

        public ViewHolder(@NotNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull SearchSuggestionItem item, @NotNull List<?> list) {
            line1.setText(item.getSuggestEntry().getTitle());
            GlideApp
                    .with(context)
                    .load(item.getSuggestEntry().getImageContainer().getImageUrl())
                    .placeholder(R.drawable.ic_round_search)
                    .transforms(new CenterCrop(), new RoundedCorners(30))
                    .into(img);
        }

        @Override
        public void unbindView(@NotNull SearchSuggestionItem item) {
            line1.setText(null);
            img.setImageDrawable(null);
        }
    }
}
