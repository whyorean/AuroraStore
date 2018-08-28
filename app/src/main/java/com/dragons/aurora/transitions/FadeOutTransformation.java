package com.dragons.aurora.transitions;

import android.view.View;

import androidx.viewpager.widget.ViewPager;

public class FadeOutTransformation implements ViewPager.PageTransformer {
    @Override
    public void transformPage(View page, float position) {

        page.setTranslationX(-position * page.getWidth());

        page.setAlpha(1 - Math.abs(position));
    }
}