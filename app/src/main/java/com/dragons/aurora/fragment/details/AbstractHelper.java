/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.dragons.aurora.fragment.details;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.model.App;
import com.percolate.caffeine.ViewUtils;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;

public abstract class AbstractHelper {

    public static int color;
    public static int colorText;

    protected DetailsFragment fragment;
    protected App app;
    protected View view;
    protected Context context;

    public AbstractHelper(DetailsFragment fragment, App app) {
        this.fragment = fragment;
        this.app = app;
        this.view = fragment.getView();
        this.context = fragment.getContext();
    }

    abstract public void draw();

    protected void setText(View v, int viewId, String text) {
        TextView textView = ViewUtils.findViewById(v, viewId);
        if (null != textView)
            textView.setText(text);
    }

    protected void setText(View v, int viewId, int stringId, Object... text) {
        if (v != null)
            setText(v, viewId, v.getResources().getString(stringId, text));
    }

    protected void hide(View v, int viewID) {
        ViewUtils.findViewById(v, viewID).setVisibility(View.GONE);
    }

    protected void show(View v, int viewID) {
        ViewUtils.findViewById(v, viewID).setVisibility(View.VISIBLE);
    }

    protected void paintButton(int buttonId) {
        android.widget.Button button = view.findViewById(buttonId);
        if (button != null)
            ViewCompat.setBackgroundTintList(button, ColorStateList.valueOf(color));
    }

    protected void paintRLayout(int layoutId) {
        RelativeLayout relativeLayout = view.findViewById(layoutId);
        if (relativeLayout != null)
            relativeLayout.setBackgroundColor(color);
    }

    void paintLLayout(int viewID) {
        LinearLayout layout = view.findViewById(viewID);
        layout.setBackgroundColor(ColorUtils.setAlphaComponent(color, 100));
    }

    protected void paintTextView(int textViewId) {
        TextView textView = view.findViewById(textViewId);
        if (textView != null)
            textView.setTextColor(color);
    }

    protected void paintTextViewTxt(int textViewId) {
        TextView textView = view.findViewById(textViewId);
        if (textView != null)
            textView.setTextColor(colorText);
    }

    protected void paintImageView(int imageViewId) {
        ImageView imageView = view.findViewById(imageViewId);
        if (imageView != null)
            imageView.setColorFilter(color);
    }

    protected void paintImageViewBackground(int imageViewId) {
        ImageView imageView = view.findViewById(imageViewId);
        if (imageView != null)
            ViewCompat.setBackgroundTintList(imageView, ColorStateList.valueOf(color));
    }

    @NonNull
    protected Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }
}
