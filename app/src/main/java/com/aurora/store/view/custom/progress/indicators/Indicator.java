package com.aurora.store.view.custom.progress.indicators;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Indicator extends Drawable implements Animatable {

    private static final Rect ZERO_BOUNDS_RECT = new Rect();

    private final HashMap<ValueAnimator, ValueAnimator.AnimatorUpdateListener> updateListenerHashMap = new HashMap<>();
    private final Paint paint = new Paint();

    private int alpha = 255;
    private ArrayList<ValueAnimator> animators;
    private boolean hasAnimators;

    protected Rect drawBounds = ZERO_BOUNDS_RECT;

    public Indicator() {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    public int getColor() {
        return paint.getColor();
    }

    public void setColor(int color) {
        paint.setColor(color);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    @SuppressWarnings({"RedundantSuppression", "deprecation"})
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public void draw(Canvas canvas) {
        draw(canvas, paint);
    }

    public abstract void draw(Canvas canvas, Paint paint);

    public abstract ArrayList<ValueAnimator> onCreateAnimators();

    @Override
    public void start() {
        ensureAnimators();

        if (animators == null) {
            return;
        }

        // If the animators has not ended, do nothing.
        if (isStarted()) {
            return;
        }
        startAnimators();
        invalidateSelf();
    }

    private void startAnimators() {
        for (int i = 0; i < animators.size(); i++) {
            ValueAnimator animator = animators.get(i);

            //when the animator restart , add the updateListener again because they
            // was removed by animator stop .
            ValueAnimator.AnimatorUpdateListener updateListener = updateListenerHashMap.get(animator);
            if (updateListener != null) {
                animator.addUpdateListener(updateListener);
            }

            animator.start();
        }
    }

    private void stopAnimators() {
        if (animators != null) {
            for (ValueAnimator animator : animators) {
                if (animator != null && animator.isStarted()) {
                    animator.removeAllUpdateListeners();
                    animator.end();
                }
            }
        }
    }

    private void ensureAnimators() {
        if (!hasAnimators) {
            animators = onCreateAnimators();
            hasAnimators = true;
        }
    }

    @Override
    public void stop() {
        stopAnimators();
    }

    private boolean isStarted() {
        for (ValueAnimator animator : animators) {
            return animator.isStarted();
        }
        return false;
    }

    @Override
    public boolean isRunning() {
        for (ValueAnimator animator : animators) {
            return animator.isRunning();
        }
        return false;
    }

    public void addUpdateListener(ValueAnimator animator, ValueAnimator.AnimatorUpdateListener updateListener) {
        updateListenerHashMap.put(animator, updateListener);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        setDrawBounds(bounds);
    }

    public void setDrawBounds(Rect drawBounds) {
        setDrawBounds(drawBounds.left, drawBounds.top, drawBounds.right, drawBounds.bottom);
    }

    public void setDrawBounds(int left, int top, int right, int bottom) {
        this.drawBounds = new Rect(left, top, right, bottom);
    }

    public void postInvalidate() {
        invalidateSelf();
    }

    public Rect getDrawBounds() {
        return drawBounds;
    }

    public int getWidth() {
        return drawBounds.width();
    }

    public int getHeight() {
        return drawBounds.height();
    }

    public int centerX() {
        return drawBounds.centerX();
    }

    public int centerY() {
        return drawBounds.centerY();
    }

    public float exactCenterX() {
        return drawBounds.exactCenterX();
    }

    public float exactCenterY() {
        return drawBounds.exactCenterY();
    }

}
