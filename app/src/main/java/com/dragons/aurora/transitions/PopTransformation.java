package com.dragons.aurora.transitions;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

public class PopTransformation implements ViewPager.PageTransformer {
    @Override
    public void transformPage(@NonNull View page, float position) {

        page.setTranslationX(-position * page.getWidth());

        if (Math.abs(position) < 0.5) {
            page.setVisibility(View.VISIBLE);
            page.setScaleX(1 - Math.abs(position));
            page.setScaleY(1 - Math.abs(position));
        } else if (Math.abs(position) > 0.5) {
            page.setVisibility(View.GONE);
        }
    }
}
