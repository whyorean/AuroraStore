package com.aurora.store.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aurora.store.R;

public class PropertyView extends RelativeLayout {

    String key;
    String value;
    TextView card_key;
    TextView card_value;

    public PropertyView(Context context, String key, String value) {
        super(context);
        this.key = key;
        this.value = value;
        init(context);
    }

    public PropertyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.item_buildprop, this);
        card_key = view.findViewById(R.id.prop_key);
        card_value = view.findViewById(R.id.prop_value);
        card_key.setText(key);
        card_value.setText(value.isEmpty() ? "N/A" : value);
    }
}
