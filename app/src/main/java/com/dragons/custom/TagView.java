package com.dragons.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;

public class TagView extends RelativeLayout {

    private int style;
    private TextView mono, dual0, dual1;

    public TagView(Context context) {
        super(context);
        init(context, null);
    }

    public TagView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TagView, 0, 0);
        try {
            style = a.getInteger(R.styleable.TagView_TagStyle, 0);
        } finally {
            a.recycle();
        }
        switch (getStyle()) {
            case 0:
                MonoTagView(context);
                break;
            case 1:
                DualTagView(context);
                break;
        }
    }

    private void MonoTagView(Context context) {
        View root = inflate(context, R.layout.tagview_mono, this);
        mono = root.findViewById(R.id.tag_mono_txt);
    }

    private void DualTagView(Context context) {
        View root = inflate(context, R.layout.tagview_duo, this);
        dual0 = root.findViewById(R.id.tag_dual_txt0);
        dual1 = root.findViewById(R.id.tag_dual_txt1);
    }

    public void setMono_title(String mono_title) {
        if (mono != null) mono.setText(mono_title);
    }

    public void setDual_title0(String dual_title0) {
        if (dual0 != null) dual0.setText(dual_title0);
    }

    public void setDual_title1(String dual_title1) {
        if (dual1 != null) dual1.setText(dual_title1);
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }
}
