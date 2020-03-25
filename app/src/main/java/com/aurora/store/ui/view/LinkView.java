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
import android.widget.Toast;

import com.aurora.store.R;
import com.aurora.store.util.Util;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LinkView extends RelativeLayout {

    @BindView(R.id.img_icon)
    ImageView linkIcon;
    @BindView(R.id.line1)
    TextView line1;
    @BindView(R.id.line2)
    TextView line2;
    @BindView(R.id.line3)
    TextView line3;

    private String title;
    private String summary;
    private String linkURL;
    private int iconId;

    public LinkView(Context context, String url, String title, String summary, int iconId) {
        super(context);
        this.linkURL = url;
        this.title = title;
        this.summary = summary;
        this.iconId = iconId;
        init();
    }

    public LinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.item_link, this);
        ButterKnife.bind(this, view);

        linkIcon.setImageResource(iconId);
        line1.setText(title);
        line2.setText(summary);

        final Uri uri = Uri.parse(linkURL);

        if (uri == null || uri.getScheme() == null) {
            line3.setVisibility(VISIBLE);
            line3.setText(linkURL);
            view.setOnClickListener(v -> {
                Util.copyToClipBoard(getContext(), linkURL);
                Toast.makeText(getContext(), getContext().getString(R.string.action_copied), Toast.LENGTH_LONG).show();
            });
        } else {
            view.setOnClickListener(click -> {
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(linkURL));
                getContext().startActivity(browserIntent);
            });
        }
    }
}
