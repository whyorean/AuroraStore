package com.aurora.store.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.Review;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {

    private Context context;
    private List<Review> reviewList;

    public ReviewsAdapter(Context context, List<Review> reviewList) {
        this.context = context;
        this.reviewList = reviewList;
    }

    public void add(int position, Review review) {
        reviewList.add(position, review);
        notifyItemInserted(position);
    }

    public void add(Review review) {
        reviewList.add(review);
    }

    public void remove(int position) {
        reviewList.remove(position);
        notifyItemRemoved(position);
    }

    public void sort() {
        Collections.sort(reviewList, (lhs, rhs) -> lhs.getRating() > rhs.getRating() ? -1 : 1);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review mReview = reviewList.get(position);
        holder.Author.setText(mReview.getUserName());
        holder.Rating.setRating(mReview.getRating());
        holder.Date.setText(getDate(mReview));
        holder.Comment.setText(mReview.getComment());


        GlideApp
                .with(context)
                .load(mReview.getUserPhotoUrl())
                .placeholder(R.color.colorTransparent)
                .circleCrop()
                .transition(new DrawableTransitionOptions().crossFade())
                .into(holder.Avatar);
    }

    private String getDate(Review review) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(review.getTimeStamp());
        return DateFormat.format("dd/MM/yyyy", calendar).toString();
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.avatar)
        ImageView Avatar;
        @BindView(R.id.author)
        TextView Author;
        @BindView(R.id.rating)
        RatingBar Rating;
        @BindView(R.id.date)
        TextView Date;
        @BindView(R.id.comment)
        TextView Comment;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
