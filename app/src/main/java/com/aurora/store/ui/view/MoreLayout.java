package com.aurora.store.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aurora.store.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MoreLayout extends LinearLayout {

    @BindView(R.id.txt_column1)
    public TextView txtColumn1;
    @BindView(R.id.txt_column2)
    public TextView txtColumn2;

    public MoreLayout(Context context) {
        super(context);
        init();
    }

    public MoreLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoreLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.item_more_two_column, this);
        ButterKnife.bind(this, view);
    }

    public void setLabel(String label) {
        txtColumn1.setText(label);
    }

    public void setValue(String value) {
        txtColumn2.setText(value);
    }
}
