package com.aurora.store.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.aurora.store.R;
import com.google.android.material.chip.Chip;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeatureChip extends RelativeLayout {

    @BindView(R.id.chip)
    public Chip chip;

    public FeatureChip(Context context) {
        super(context);
        init();
    }

    public FeatureChip(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FeatureChip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.item_feature_chip, this);
        ButterKnife.bind(this, view);
    }

    public void setLabel(String label) {
        chip.setText(label);
    }

    public void setProgress(int progress) {

    }
}
