package com.aurora.store.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aurora.store.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RatingView extends RelativeLayout {

    @BindView(R.id.avg_num)
    TextView avg_num;
    @BindView(R.id.avg_rating)
    ProgressBar avg_rating;
    int number;
    int max;
    int rating;

    public RatingView(Context context, int number, int max, int rating) {
        super(context);
        this.number = number;
        this.max = max;
        this.rating = rating;
        init(context);
    }

    public RatingView(Context context) {
        super(context);
    }

    public RatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RatingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.item_rating, this);
        ButterKnife.bind(this, view);

        avg_num.setText(String.valueOf(number));
        avg_rating.setMax(max);
        avg_rating.setProgress(rating);
    }
}
