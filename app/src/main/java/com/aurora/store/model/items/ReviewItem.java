package com.aurora.store.model.items;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.Review;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Getter;
import lombok.Setter;

public class ReviewItem extends AbstractItem<ReviewItem.ViewHolder> {

    @Getter
    @Setter
    private Review review;

    public ReviewItem(Review review) {
        this.review = review;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_review;
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

    public static class ViewHolder extends FastItemAdapter.ViewHolder<ReviewItem> {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;
        @BindView(R.id.rating)
        RatingBar rating;
        @BindView(R.id.line2)
        TextView line2;
        @BindView(R.id.line3)
        TextView line3;

        private Context context;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            context = itemView.getContext();
        }

        private static String getDate(long millisecond) {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.setTimeInMillis(millisecond);
            return DateFormat.format("dd/MM/yy", calendar).toString();
        }

        @Override
        public void bindView(@NotNull ReviewItem item, @NotNull List<?> list) {
            final Review review = item.getReview();

            line1.setText(review.getUserName());
            line2.setText(getDate(review.getTimeStamp()));
            line3.setText(review.getComment());
            rating.setRating(review.getRating());

            GlideApp
                    .with(context)
                    .load(review.getUserPhotoUrl())
                    .placeholder(R.color.colorTransparent)
                    .circleCrop()
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(img);
        }

        @Override
        public void unbindView(@NotNull ReviewItem item) {
            img.setImageDrawable(null);
            line1.setText(null);
            line2.setText(null);
            line3.setText(null);
        }
    }
}
