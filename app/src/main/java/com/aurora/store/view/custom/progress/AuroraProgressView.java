package com.aurora.store.view.custom.progress;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.aurora.store.R;
import com.aurora.store.view.custom.progress.indicators.BallPulseIndicator;
import com.aurora.store.view.custom.progress.indicators.Indicator;


public class AuroraProgressView extends View {

    private static final String TAG = "AuroraProgressView";

    private static final BallPulseIndicator DEFAULT_INDICATOR = new BallPulseIndicator();

    private static final int MIN_SHOW_TIME = 500; // ms
    private static final int MIN_DELAY = 500; // ms

    private long startTime = -1;

    private boolean postedHide = false;
    private boolean postedShow = false;
    private boolean dismissed = false;

    private final Runnable mDelayedHide = new Runnable() {

        @Override
        public void run() {
            postedHide = false;
            startTime = -1;
            setVisibility(View.GONE);
        }
    };

    private final Runnable mDelayedShow = new Runnable() {

        @Override
        public void run() {
            postedShow = false;
            if (!dismissed) {
                startTime = System.currentTimeMillis();
                setVisibility(View.VISIBLE);
            }
        }
    };

    int minWidth;
    int maxWidth;
    int minHeight;
    int maxHeight;

    private Indicator indicator;
    private int indicatorColor;

    private boolean mShouldStartAnimationDrawable;

    public AuroraProgressView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public AuroraProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, R.style.AuroraProgressView);
    }

    public AuroraProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.AuroraProgressView);
    }

    public AuroraProgressView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, R.style.AuroraProgressView);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        minWidth = 24;
        maxWidth = 48;
        minHeight = 24;
        maxHeight = 48;

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.AuroraProgressView, defStyleAttr, defStyleRes);

        minWidth = a.getDimensionPixelSize(R.styleable.AuroraProgressView_minWidth, minWidth);
        maxWidth = a.getDimensionPixelSize(R.styleable.AuroraProgressView_maxWidth, maxWidth);
        minHeight = a.getDimensionPixelSize(R.styleable.AuroraProgressView_minHeight, minHeight);
        maxHeight = a.getDimensionPixelSize(R.styleable.AuroraProgressView_maxHeight, maxHeight);
        String indicatorName = a.getString(R.styleable.AuroraProgressView_indicatorName);
        indicatorColor = a.getColor(R.styleable.AuroraProgressView_indicatorColor, Color.WHITE);
        setIndicator(indicatorName);

        if (indicator == null) {
            setIndicator(DEFAULT_INDICATOR);
        }

        a.recycle();
    }

    public Indicator getIndicator() {
        return indicator;
    }

    public void setIndicator(Indicator d) {
        if (indicator != d) {
            if (indicator != null) {
                indicator.setCallback(null);
                unscheduleDrawable(indicator);
            }

            indicator = d;
            //need to set indicator color again if you didn't specified when you update the indicator .
            setIndicatorColor(indicatorColor);
            if (d != null) {
                d.setCallback(this);
            }
            postInvalidate();
        }
    }

    public void setIndicatorColor(int color) {
        this.indicatorColor = color;
        indicator.setColor(color);
    }

    public void setIndicator(String indicatorName) {
        if (TextUtils.isEmpty(indicatorName)) {
            return;
        }
        StringBuilder drawableClassName = new StringBuilder();
        if (!indicatorName.contains(".")) {
            String defaultPackageName = getClass().getPackage().getName();
            drawableClassName.append(defaultPackageName)
                    .append(".indicators")
                    .append(".");
        }
        drawableClassName.append(indicatorName);
        try {
            Class<?> drawableClass = Class.forName(drawableClassName.toString());
            Indicator indicator = (Indicator) drawableClass.getDeclaredConstructor().newInstance();
            setIndicator(indicator);
        } catch (Exception exception) {
            Log.e(TAG, "Failed to set indicator!", exception);
        }
    }

    public void smoothToShow() {
        startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        setVisibility(VISIBLE);
    }

    public void smoothToHide() {
        startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
        setVisibility(GONE);
    }

    public void hide() {
        dismissed = true;
        removeCallbacks(mDelayedShow);
        long diff = System.currentTimeMillis() - startTime;
        if (diff >= MIN_SHOW_TIME || startTime == -1) {
            // The progress spinner has been shown long enough
            // OR was not shown yet. If it wasn't shown yet,
            // it will just never be shown.
            setVisibility(View.GONE);
        } else {
            // The progress spinner is shown, but not long enough,
            // so put a delayed message in to hide it when its been
            // shown long enough.
            if (!postedHide) {
                postDelayed(mDelayedHide, MIN_SHOW_TIME - diff);
                postedHide = true;
            }
        }
    }

    public void show() {
        // Reset the start time.
        startTime = -1;
        dismissed = false;
        removeCallbacks(mDelayedHide);
        if (!postedShow) {
            postDelayed(mDelayedShow, MIN_DELAY);
            postedShow = true;
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == indicator
                || super.verifyDrawable(who);
    }

    void startAnimation() {
        if (getVisibility() != VISIBLE) {
            return;
        }

        if (indicator != null) {
            mShouldStartAnimationDrawable = true;
        }
        postInvalidate();
    }

    void stopAnimation() {
        if (indicator != null) {
            indicator.stop();
            mShouldStartAnimationDrawable = false;
        }
        postInvalidate();
    }

    @Override
    public void setVisibility(int v) {
        if (getVisibility() != v) {
            super.setVisibility(v);
            if (v == GONE || v == INVISIBLE) {
                stopAnimation();
            } else {
                startAnimation();
            }
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == GONE || visibility == INVISIBLE) {
            stopAnimation();
        } else {
            startAnimation();
        }
    }

    @Override
    public void invalidateDrawable(Drawable dr) {
        if (verifyDrawable(dr)) {
            invalidate();
        } else {
            super.invalidateDrawable(dr);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateDrawableBounds(w, h);
    }

    private void updateDrawableBounds(int w, int h) {
        // onDraw will translate the canvas so we draw starting at 0,0.
        // Subtract out padding for the purposes of the calculations below.
        w -= getPaddingRight() + getPaddingLeft();
        h -= getPaddingTop() + getPaddingBottom();

        int right = w;
        int bottom = h;
        int top = 0;
        int left = 0;

        if (indicator != null) {
            // Maintain aspect ratio. Certain kinds of animated drawables
            // get very confused otherwise.
            final int intrinsicWidth = indicator.getIntrinsicWidth();
            final int intrinsicHeight = indicator.getIntrinsicHeight();
            final float intrinsicAspect = (float) intrinsicWidth / intrinsicHeight;
            final float boundAspect = (float) w / h;
            if (intrinsicAspect != boundAspect) {
                if (boundAspect > intrinsicAspect) {
                    // New width is larger. Make it smaller to match height.
                    final int width = (int) (h * intrinsicAspect);
                    left = (w - width) / 2;
                    right = left + width;
                } else {
                    // New height is larger. Make it smaller to match width.
                    final int height = (int) (w * (1 / intrinsicAspect));
                    top = (h - height) / 2;
                    bottom = top + height;
                }
            }
            indicator.setBounds(left, top, right, bottom);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTrack(canvas);
    }

    void drawTrack(Canvas canvas) {
        final Drawable d = indicator;
        if (d != null) {
            // Translate canvas so a indeterminate circular progress bar with padding
            // rotates properly in its animation
            final int saveCount = canvas.save();

            canvas.translate(getPaddingLeft(), getPaddingTop());

            d.draw(canvas);
            canvas.restoreToCount(saveCount);

            if (mShouldStartAnimationDrawable && d instanceof Animatable) {
                ((Animatable) d).start();
                mShouldStartAnimationDrawable = false;
            }
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dw = 0;
        int dh = 0;

        final Drawable d = indicator;
        if (d != null) {
            dw = Math.max(minWidth, Math.min(maxWidth, d.getIntrinsicWidth()));
            dh = Math.max(minHeight, Math.min(maxHeight, d.getIntrinsicHeight()));
        }

        updateDrawableState();

        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        final int measuredWidth = resolveSizeAndState(dw, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(dh, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    private void updateDrawableState() {
        final int[] state = getDrawableState();
        if (indicator != null && indicator.isStateful()) {
            indicator.setState(state);
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (indicator != null) {
            indicator.setHotspot(x, y);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
        removeCallbacks();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        // This should come after stopAnimation(), otherwise an invalidate message remains in the
        // queue, which can prevent the entire view hierarchy from being GC'ed during a rotation
        super.onDetachedFromWindow();
        removeCallbacks();
    }

    private void removeCallbacks() {
        removeCallbacks(mDelayedHide);
        removeCallbacks(mDelayedShow);
    }


}

