package com.aurora.store.section;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.GlideApp;
import com.aurora.store.R;
import com.aurora.store.model.Review;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;

public class ReviewsSection extends Section {

    private Context context;
    private List<Review> reviewList = new ArrayList<>();

    public ReviewsSection(Context context) {
        super(SectionParameters.builder()
                .itemResourceId(R.layout.item_review)
                .loadingResourceId(R.layout.item_loading)
                .emptyResourceId(R.layout.item_empty)
                .build());
        this.context = context;

        setState(State.LOADING);
    }

    private static String getDate(long millisecond) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(millisecond);
        return DateFormat.format("dd/MM/yy", calendar).toString();
    }

    public void updateList(List<Review> appList) {
        this.reviewList.clear();
        this.reviewList.addAll(appList);
        if (!appList.isEmpty())
            setState(State.LOADED);
    }

    public List<Review> getList() {
        return reviewList;
    }

    public void add(Review review) {
        reviewList.add(review);
    }

    public void clearReviews() {
        this.reviewList.clear();
        setState(State.LOADING);
    }

    public int getCount() {
        return reviewList.size();
    }

    @Override
    public int getContentItemsTotal() {
        return reviewList.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new ContentHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder getEmptyViewHolder(View view) {
        return new EmptyHolder(view);
    }

    @Override
    public RecyclerView.ViewHolder getLoadingViewHolder(View view) {
        return new LoadingHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        final ContentHolder contentHolder = (ContentHolder) holder;
        final Review review = reviewList.get(position);
        contentHolder.line1.setText(review.getUserName());
        contentHolder.rating.setRating(review.getRating());
        contentHolder.line2.setText(getDate(review.getTimeStamp()));
        contentHolder.line3.setText(review.getComment());

        GlideApp
                .with(context)
                .load(review.getUserPhotoUrl())
                .placeholder(R.color.colorTransparent)
                .circleCrop()
                .transition(new DrawableTransitionOptions().crossFade())
                .into(contentHolder.img);
    }

    public static class ContentHolder extends RecyclerView.ViewHolder {
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

        ContentHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class EmptyHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;

        EmptyHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            line1.setText(itemView.getContext().getString(R.string.list_empty_reviews));
        }
    }

    static class LoadingHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.progress_bar)
        ProgressBar progressBar;
        @BindView(R.id.line1)
        TextView line1;

        LoadingHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
