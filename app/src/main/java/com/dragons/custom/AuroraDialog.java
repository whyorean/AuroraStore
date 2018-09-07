package com.dragons.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.dragons.aurora.R;

import androidx.appcompat.app.AppCompatDialog;
import androidx.core.content.res.ResourcesCompat;

public class AuroraDialog extends AppCompatDialog {

    public AuroraDialog(Context context) {
        super(context);
        init();
    }

    public AuroraDialog(Context context, int theme) {
        super(context, theme);
        init();
    }

    private void init() {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(wrap(view));
    }

    @SuppressLint("ClickableViewAccessibility")
    private View wrap(View view) {
        Context context = getContext();
        Resources resources = context.getResources();
        int verticalMargin = resources.getDimensionPixelSize(R.dimen.dialog_vertical_margin);
        int horizontalMargin = resources.getDimensionPixelSize(R.dimen.dialog_horizontal_margin);

        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
        params.gravity = Gravity.CENTER;
        frameLayout.addView(view, params);
        Rect rect = new Rect();
        frameLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.getGlobalVisibleRect(rect);
                if (!rect.contains((int) event.getX(), (int) event.getY())) {
                    cancel();
                    return true;
                } else {
                    return false;
                }
            } else
                return false;
        });
        frameLayout.setBackground(new ColorDrawable(ResourcesCompat.getColor(resources, R.color.scrim, context.getTheme())));
        return frameLayout;
    }
}
