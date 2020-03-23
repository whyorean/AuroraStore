package com.aurora.store.model.items;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.CategoryModel;
import com.aurora.store.util.ImageUtil;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryItem extends AbstractItem<CategoryItem.ViewHolder> {

    private CategoryModel categoryModel;

    public CategoryItem(CategoryModel categoryModel) {
        this.categoryModel = categoryModel;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_category_list;
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

    public static class ViewHolder extends FastAdapter.ViewHolder<CategoryItem> {
        @BindView(R.id.imgIcon)
        ImageView imgIcon;
        @BindView(R.id.line1)
        TextView txtLine1;

        private Context context;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull CategoryItem item, @NotNull List<?> list) {
            txtLine1.setText(item.categoryModel.getCategoryTitle());
            imgIcon.setBackground(ImageUtil.getDrawable(new Random().nextInt(5), GradientDrawable.OVAL));

            GlideApp
                    .with(context)
                    .load(item.categoryModel.getCategoryImageUrl())
                    .circleCrop()
                    .into(imgIcon);
        }

        @Override
        public void unbindView(@NotNull CategoryItem item) {
            txtLine1.setText(null);
            imgIcon.setImageDrawable(null);
        }
    }
}
