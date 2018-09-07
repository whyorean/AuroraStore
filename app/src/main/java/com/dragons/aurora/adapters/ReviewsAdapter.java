package com.dragons.aurora.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.R;
import com.dragons.aurora.model.Review;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {

    private Context mContext;
    private List<Review> mReviewList;

    public ReviewsAdapter(Context mContext, List<Review> mReviewList) {
        this.mContext = mContext;
        this.mReviewList = mReviewList;
    }

    //Methods to Update List (Add or Remove)
    public void add(int position, Review review) {
        mReviewList.add(position, review);
        notifyItemInserted(position);
    }

    public void add(Review review) {
        mReviewList.add(review);
    }

    public void remove(int position) {
        mReviewList.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review mReview = mReviewList.get(position);
        holder.Author.setText(mReview.getUserName());
        holder.Title.setText(mContext.getString(
                R.string.two_items,
                mContext.getString(R.string.details_rating, (double) mReview.getRating()),
                mReview.getTitle()
        ));
        holder.Comment.setText(mReview.getComment());
        Glide
                .with(mContext)
                .load(mReview.getUserPhotoUrl())
                .apply(new RequestOptions()
                        .placeholder(R.color.transparent)
                        .circleCrop())
                .transition(new DrawableTransitionOptions().crossFade())
                .into(holder.Avatar);
    }

    @Override
    public int getItemCount() {
        return mReviewList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.avatar)
        ImageView Avatar;
        @BindView(R.id.author)
        TextView Author;
        @BindView(R.id.title)
        TextView Title;
        @BindView(R.id.comment)
        TextView Comment;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
