/*
 * Aurora Droid
 * Copyright (C) 2019-20, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Droid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Aurora Droid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Droid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.ui.view.placeholder;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.aurora.store.R;

import org.apache.commons.lang3.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EmptyView extends RelativeLayout {

    @BindView(R.id.img_empty)
    AppCompatImageView imgEmpty;
    @BindView(R.id.txt_empty)
    AppCompatTextView txtEmpty;

    public EmptyView(Context context) {
        super(context);
    }

    public EmptyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public EmptyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public EmptyView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.view_empty, this);
        ButterKnife.bind(this, view);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EmptyView);
        final String txt = typedArray.getString(R.styleable.EmptyView_emptyText);
        final Drawable drawable = typedArray.getDrawable(R.styleable.EmptyView_emptyImage);

        txtEmpty.setText(StringUtils.isEmpty(txt) ? getResources().getString(R.string.action_loading) : txt);
        imgEmpty.setImageDrawable(drawable);
        typedArray.recycle();
    }
}
