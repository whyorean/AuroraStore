package com.aurora.store.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.aurora.store.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DevInfoLayout extends RelativeLayout {

    @BindView(R.id.img)
    AppCompatImageView img;
    @BindView(R.id.txt_title)
    AppCompatTextView txtTitle;
    @BindView(R.id.txt_subtitle)
    AppCompatTextView txtSubtitle;

    public DevInfoLayout(Context context) {
        super(context);
        init(context, null);
    }

    public DevInfoLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DevInfoLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public DevInfoLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_dev_info, this);
        ButterKnife.bind(this, view);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DevInfoLayout);
        final int icon = typedArray.getResourceId(R.styleable.DevInfoLayout_imgIcon, R.drawable.ic_permission_unknown);
        final String textPrimary = typedArray.getString(R.styleable.DevInfoLayout_txtTitle);
        final String textSecondary = typedArray.getString(R.styleable.DevInfoLayout_txtSubtitle);

        img.setImageResource(icon);
        txtTitle.setText(textPrimary);
        txtSubtitle.setText(textSecondary);

        typedArray.recycle();
    }

    public void setTxtSubtitle(String text) {
        txtSubtitle.setText(text);
        invalidate();
    }
}
