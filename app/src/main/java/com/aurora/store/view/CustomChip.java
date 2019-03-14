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
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.aurora.store.R;
import com.aurora.store.activity.LeaderBoardActivity;
import com.aurora.store.utility.Log;
import com.google.android.material.chip.Chip;

public class CustomChip extends Chip {

    private String category;
    private String subCategory;

    public CustomChip(Context context) {
        super(context);
        init(context, null, 0);
    }

    public CustomChip(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public CustomChip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomChip, defStyleAttr, 0);
        category = a.getString(R.styleable.CustomChip_category);
        subCategory = a.getString(R.styleable.CustomChip_subCategory);
        a.recycle();
        setOnClickListener(openLeaderBoardActivity());
    }

    private View.OnClickListener openLeaderBoardActivity() {
        return v -> {
            Intent intent = new Intent(getContext(), LeaderBoardActivity.class);
            intent.putExtra("INTENT_CATEGORY", category);
            intent.putExtra("INTENT_SUBCATEGORY", subCategory);
            intent.putExtra("INTENT_TITLE", getText().toString());
            getContext().startActivity(intent);
        };
    }
}
