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

package com.aurora.store.ui.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.aurora.store.R;

public class LinkView extends RelativeLayout {

    Context context;
    CardView link_card;
    ImageView card_icon;
    TextView card_title;
    TextView card_summary;

    private String title;
    private String summary;
    private String linkURL;
    private int cardIconID;

    public LinkView(Context context, String linkURL, String title, String summary, int cardIconID) {
        super(context);
        this.context = context;
        this.linkURL = linkURL;
        this.title = title;
        this.summary = summary;
        this.cardIconID = cardIconID;
        init(context);
    }

    public LinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.item_link, this);
        card_icon = view.findViewById(R.id.link_icon);
        card_title = view.findViewById(R.id.link_title);
        card_summary = view.findViewById(R.id.link_summary);
        card_title.setText(title);
        card_summary.setText(summary);
        card_icon.setImageResource(cardIconID);

        view.setOnClickListener(click -> {
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse(linkURL));
            context.startActivity(browserIntent);
        });
    }
}
