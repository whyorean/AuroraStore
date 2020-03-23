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

package com.aurora.store.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;

import androidx.appcompat.app.AppCompatActivity;

public class ViewUtil {

    private static int ANIMATION_DURATION_SHORT = 400;

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int getStyledAttribute(Context context, int styleID) {
        TypedArray arr = context.obtainStyledAttributes(new TypedValue().data, new int[]{styleID});
        int styledColor = arr.getColor(0, Color.WHITE);
        arr.recycle();
        return styledColor;
    }

    public static void showWithAnimation(View view) {
        final int mShortAnimationDuration = view.getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(mShortAnimationDuration)
                .setListener(null);

    }

    public static void hideWithAnimation(View view) {
        final int duration = view.getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        view.animate()
                .alpha(0f)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }
                });
    }

    public static void rotateView(View view, boolean reverse) {
        final RotateAnimation animation = new RotateAnimation(
                reverse ? 180 : 0,
                reverse ? 0 : 180,
                (float) view.getWidth() / 2,
                (float) view.getHeight() / 2);
        animation.setDuration(ANIMATION_DURATION_SHORT);
        animation.setFillAfter(true);
        view.startAnimation(animation);
    }

    public static void expand(final View view) {
        view.measure(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        final int targetHeight = view.getMeasuredHeight();
        view.getLayoutParams().height = 1;
        view.setVisibility(View.VISIBLE);
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                view.getLayoutParams().height = interpolatedTime == 1
                        ? WindowManager.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setDuration((int) (targetHeight / view.getContext().getResources().getDisplayMetrics().density));
        //animation.setDuration(ANIMATION_DURATION_SHORT);
        view.startAnimation(animation);
    }

    public static void collapse(final View view) {
        final int initialHeight = view.getMeasuredHeight();
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                } else {
                    view.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    view.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setDuration((int) (initialHeight / view.getContext().getResources().getDisplayMetrics().density));
        //animation.setDuration(ANIMATION_DURATION_SHORT);
        view.startAnimation(animation);
    }

    public static void setVisibility(View view, boolean visibility) {
        if (visibility)
            showWithAnimation(view);
        else
            hideWithAnimation(view);
    }

    public static void setVisibility(View view, boolean visibility, boolean noAnim) {
        if (noAnim)
            view.setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
        else
            setVisibility(view, visibility);
    }

    public static Bundle getEmptyActivityBundle(AppCompatActivity activity) {
        return ActivityOptions.makeSceneTransitionAnimation(activity).toBundle();
    }
}
