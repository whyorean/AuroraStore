/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

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
