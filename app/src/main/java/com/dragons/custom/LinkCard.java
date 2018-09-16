/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
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
 */

package com.dragons.custom;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;

import androidx.cardview.widget.CardView;

public class LinkCard extends RelativeLayout {

    Context context;
    CardView link_card;
    ImageView card_icon;
    TextView card_title;
    TextView card_summary;

    private String title;
    private String summary;
    private String linkURL;
    private boolean shouldClick;
    private int cardIconID;

    public LinkCard(Context context, String linkURL, String title, String summary, int cardIconID, boolean shouldClick) {
        super(context);
        this.context = context;
        this.linkURL = linkURL;
        this.title = title;
        this.summary = summary;
        this.cardIconID = cardIconID;
        this.shouldClick = shouldClick;
        init(context);
    }

    public LinkCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.item_link, this);
        link_card = view.findViewById(R.id.link_card);
        card_icon = view.findViewById(R.id.card_icon);
        card_title = view.findViewById(R.id.card_title);
        card_summary = view.findViewById(R.id.card_summary);
        card_title.setText(title);
        card_summary.setText(summary);
        card_icon.setImageResource(cardIconID);

        if (shouldClick)
            link_card.setOnClickListener(click -> {
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(linkURL));
                context.startActivity(browserIntent);
            });
    }
}
