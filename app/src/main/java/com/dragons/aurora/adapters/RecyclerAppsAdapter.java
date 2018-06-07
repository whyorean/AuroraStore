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

package com.dragons.aurora.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.fragment.details.ButtonDownload;
import com.dragons.aurora.fragment.details.ButtonUninstall;
import com.dragons.aurora.fragment.details.DownloadOptions;
import com.dragons.aurora.model.App;
import com.squareup.picasso.Picasso;

import java.util.List;

public class RecyclerAppsAdapter extends RecyclerView.Adapter<RecyclerAppsAdapter.ViewHolder> {

    private List<App> appsToAdd;
    private Context context;

    public RecyclerAppsAdapter(Context context, List<App> appsToAdd) {
        this.context = context;
        this.appsToAdd = appsToAdd;
    }

    public void add(int position, App app) {
        appsToAdd.add(position, app);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        appsToAdd.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public RecyclerAppsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.recycler_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAppsAdapter.ViewHolder holder, int position) {
        final App app = appsToAdd.get(position);

        holder.appName.setText(app.getDisplayName());
        setText(holder.view, holder.appRating, R.string.details_rating, app.getRating().getAverage());
        holder.appRatingBar.setRating(app.getRating().getStars(1));

        Picasso
                .with(context)
                .load(app.getIconInfo().getUrl())
                .placeholder(R.color.transparent)
                .into(holder.appIcon);

        holder.appContainer.setOnClickListener(v -> {
            Context context = holder.view.getContext();
            context.startActivity(DetailsActivity.getDetailsIntent(context, app.getPackageName()));
        });

        holder.menu_3dot.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.menu_download);
            new DownloadOptions((AuroraActivity) context, app).inflate(popup.getMenu());
            popup.getMenu().findItem(R.id.action_download).setVisible(new ButtonDownload((AuroraActivity) context, app).shouldBeVisible());
            popup.getMenu().findItem(R.id.action_uninstall).setVisible(app.isInstalled());
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.action_download:
                        new ButtonDownload((AuroraActivity) context, app).checkAndDownload();
                        break;
                    case R.id.action_uninstall:
                        new ButtonUninstall((AuroraActivity) context, app).uninstall();
                        remove(position);
                        break;
                    default:
                        return new DownloadOptions((AuroraActivity) context, app).onContextItemSelected(item);
                }
                return false;
            });
            popup.show();
        });
    }

    protected void setText(TextView textView, String text) {
        if (null != textView)
            textView.setText(text);
    }

    protected void setText(View v, TextView textView, int stringId, Object... text) {
        setText(textView, v.getResources().getString(stringId, text));
    }

    @Override
    public int getItemCount() {
        return appsToAdd.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View view;
        private RelativeLayout appContainer;
        private TextView appName;
        private TextView appRating;
        private RatingBar appRatingBar;
        private ImageView appIcon;
        private ImageView menu_3dot;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            appContainer = view.findViewById(R.id.app_container);
            appName = view.findViewById(R.id.app_name);
            appRating = view.findViewById(R.id.app_rating);
            appRatingBar = view.findViewById(R.id.app_ratingbar);
            appIcon = view.findViewById(R.id.app_icon);
            menu_3dot = view.findViewById(R.id.menu_3dot);
        }
    }

}
