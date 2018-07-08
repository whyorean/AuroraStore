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
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.Util;
import com.dragons.aurora.fragment.DetailsFragment;
import com.dragons.aurora.fragment.PreferenceFragment;
import com.dragons.aurora.model.App;
import com.percolate.caffeine.PhoneUtils;
import com.percolate.caffeine.ViewUtils;

public abstract class AbstractHelper {

    protected DetailsFragment fragment;
    protected App app;
    protected View view;
    protected Context context;

    public AbstractHelper(DetailsFragment fragment, App app) {
        this.fragment = fragment;
        this.app = app;
        this.view=fragment.getView();
        this.context=fragment.getContext();
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

    protected boolean isLoggedIn() {
        return PreferenceFragment.getBoolean(fragment.getActivity(), "LOGGED_IN");
    }

    protected boolean isDummy() {
        return PreferenceFragment.getBoolean(fragment.getActivity(), "DUMMY_ACC");
    }

    protected boolean isGoogle() {
        return PreferenceFragment.getBoolean(fragment.getActivity(), "GOOGLE_ACC");
    }

    protected boolean isConnected(Context c) {
        return PhoneUtils.isNetworkAvailable(c);
    }

    protected void hide(View v, int viewID) {
        ViewUtils.findViewById(v, viewID).setVisibility(View.GONE);
    }

    protected void show(View v, int viewID) {
        ViewUtils.findViewById(v, viewID).setVisibility(View.VISIBLE);
    }

    protected void paintButton(int color, int buttonId) {
        android.widget.Button button = view.findViewById(buttonId);
        if (button != null)
            ViewCompat.setBackgroundTintList(button, ColorStateList.valueOf(color));
    }

    protected void paintRLayout(int color, int layoutId) {
        RelativeLayout relativeLayout = view.findViewById(layoutId);
        if (relativeLayout != null)
            relativeLayout.setBackgroundColor(color);
    }

    void paintLLayout(int color, int viewID) {
        LinearLayout layout = view.findViewById(viewID);
        if (layout != null && !Util.isDark(fragment.getContext()))
            ViewCompat.setBackgroundTintList(layout, ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 50)));
    }

    protected void paintTextView(int color, int textViewId) {
        TextView textView = view.findViewById(textViewId);
        if (textView != null)
            textView.setTextColor(color);
    }

    protected void paintImageView(int color, int imageViewId) {
        ImageView imageView = view.findViewById(imageViewId);
        if (imageView != null)
            imageView.setColorFilter(color);
    }

    protected void paintImageViewBg(int color, int imageViewId) {
        ImageView imageView = view.findViewById(imageViewId);
        if (imageView != null)
            imageView.setBackgroundColor(color);
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
