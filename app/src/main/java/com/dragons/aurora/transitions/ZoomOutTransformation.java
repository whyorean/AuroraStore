package com.dragons.aurora.transitions;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

public class ZoomOutTransformation implements ViewPager.PageTransformer {

    private static final float MIN_SCALE = 0.65f;
    private static final float MIN_ALPHA = 0.3f;

    @Override
    public void transformPage(@NonNull View page, float position) {

        if (position < -1) {
            page.setAlpha(0);

        } else if (position <= 1) {
            page.setScaleX(Math.max(MIN_SCALE, 1 - Math.abs(position)));
            page.setScaleY(Math.max(MIN_SCALE, 1 - Math.abs(position)));
            page.setAlpha(Math.max(MIN_ALPHA, 1 - Math.abs(position)));
        } else {
            page.setAlpha(0);
        }
    }
}
