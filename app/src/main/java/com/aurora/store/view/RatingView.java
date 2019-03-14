/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

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
