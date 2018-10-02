/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
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

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Vibrator;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.dragons.aurora.BlackWhiteListManager;
import com.dragons.aurora.OnAppInstalledListener;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.activities.DetailsActivity;
import com.dragons.aurora.database.Jessie;
import com.dragons.aurora.fragment.UpdatableAppsFragment;
import com.dragons.aurora.fragment.details.ButtonDownload;
import com.dragons.aurora.fragment.details.ButtonUninstall;
import com.dragons.aurora.fragment.details.DownloadOptions;
import com.dragons.aurora.model.App;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

public class UpdatableAppsGridAdapter extends RecyclerView.Adapter<UpdatableAppsGridAdapter.ViewHolder> implements OnAppInstalledListener {

    public List<App> appsToAdd;
    private UpdatableAppsFragment fragment;
    private Context mContext;
    private Boolean isGrid;

    //Database Manager
    private Jessie mJessie;

    //DialogViews
    private TextView AppTitle;
    private TextView AppSize;
    private TextView AppExtra;
    private TextView AppChangelog;
    private Button Blacklist;
    private Button Update;

    public UpdatableAppsGridAdapter(UpdatableAppsFragment fragment, List<App> appsToAdd, Boolean isGrid) {
        this.fragment = fragment;
        this.appsToAdd = appsToAdd;
        this.mContext = fragment.getContext();
        this.isGrid = isGrid;
        mJessie = new Jessie(mContext);
    }

    public void add(int position, App app) {
        appsToAdd.add(position, app);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        appsToAdd.remove(position);
        notifyItemRemoved(position);
    }

    public void remove(String packageName) {
        int i = 0;
        for (App app : appsToAdd) {
            if (app.getPackageName().equals(packageName)) {
                remove(i);
                mJessie.removeAppFromJson(Jessie.JSON_UPDATES, i);
                break;
            }
            i++;
        }
    }

    @NonNull
    @Override
    public UpdatableAppsGridAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (isGrid)
            return new ViewHolder(inflater.inflate(R.layout.item_updatable_grid, parent, false));
        else
            return new ViewHolder(inflater.inflate(R.layout.item_updatable, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UpdatableAppsGridAdapter.ViewHolder holder, int position) {
        App app = appsToAdd.get(position);
        Vibrator vibe = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        String date = "?";

        if (!TextUtils.isEmpty(app.getUpdated())) {
            date = app.getUpdated();
            if (date.contains(","))
                date = date.substring(0, date.indexOf(','));
        }

        holder.AppTitle.setText(app.getDisplayName());
        holder.AppSize.setText(Formatter.formatShortFileSize(mContext, app.getSize()));
        holder.AppExtra.setText(date);

        Glide
                .with(mContext)
                .load(app.getIconInfo().getUrl())
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                .transition(new DrawableTransitionOptions().crossFade())
                .into(holder.AppIcon);

        holder.AppCard.setOnClickListener(v -> {
            mContext.startActivity(DetailsActivity.getDetailsIntent(mContext, app.getPackageName()));
        });

        holder.AppCard.setOnLongClickListener(v -> {
            if (vibe != null) {
                vibe.vibrate(25);
            }
            getPopup(app);
            return true;
        });

        if ((!isGrid)) {
            holder.AppMenu.setVisibility(View.VISIBLE);
            setup3dotMenu(holder, app, position);
        }
    }

    private void setup3dotMenu(ViewHolder viewHolder, App app, int position) {
        viewHolder.AppMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.menu_download);
            new DownloadOptions(fragment.getContext(), fragment.getView(), app).inflate(popup.getMenu());
            popup.getMenu().findItem(R.id.action_download).setVisible(new ButtonDownload(fragment.getContext(), fragment.getView(), app).shouldBeVisible());
            popup.getMenu().findItem(R.id.action_uninstall).setVisible(app.isInstalled());
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.action_download:
                        new ButtonDownload(fragment.getContext(), fragment.getView(), app).checkAndDownload();
                        break;
                    case R.id.action_uninstall:
                        new ButtonUninstall(fragment.getContext(), fragment.getView(), app).uninstall();
                        remove(position);
                        break;
                    default:
                        return new DownloadOptions(fragment.getContext(), fragment.getView(), app).onContextItemSelected(item);
                }
                return false;
            });
            popup.show();
        });
    }

    private void getPopup(App app) {
        Dialog ad = new Dialog(mContext);
        ad.setContentView(R.layout.dialog_app_details);
        ad.setCancelable(true);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(ad.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER;

        ad.getWindow().setAttributes(layoutParams);

        ImageView AppBanner = ad.findViewById(R.id.app_banner);
        AppTitle = ad.findViewById(R.id.app_title);
        AppSize = ad.findViewById(R.id.app_size);
        AppExtra = ad.findViewById(R.id.app_extra);
        AppChangelog = ad.findViewById(R.id.app_changelog);

        Blacklist = ad.findViewById(R.id.btn_blacklist);
        Update = ad.findViewById(R.id.btn_update);

        AppTitle.setText(app.getDisplayName());
        AppSize.setText(Formatter.formatShortFileSize(mContext, app.getSize()));
        if (app.getChanges().isEmpty())
            AppChangelog.setText(R.string.details_changelog_empty);
        else
            AppChangelog.setText(Html.fromHtml(app.getChanges()).toString());
        AppExtra.setText(app.getUpdated());

        Glide.with(mContext)
                .asBitmap()
                .load(app.getPageBackgroundImage().getUrl())
                .apply(new RequestOptions()
                        .centerCrop()
                        .priority(Priority.HIGH))
                .transition(new BitmapTransitionOptions().crossFade())
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        getPalette(ad, resource);
                        return false;
                    }
                })
                .into(AppBanner);

        if (Util.isAlreadyQueued(app)) {
            Update.setText(mContext.getResources().getString(R.string.details_download_queued));
            Update.setEnabled(false);
        }

        Update.setOnClickListener(v -> {
            new ButtonDownload(fragment.getContext(), fragment.getView(), app).checkAndDownload();
            Update.setText(mContext.getResources().getString(R.string.details_download_queued));
            Update.setEnabled(false);
        });

        Blacklist.setOnClickListener(v -> {
            new BlackWhiteListManager(mContext).add(app.getPackageName());
            ad.dismiss();
            remove(app.getPackageName());
        });

        ad.show();
    }

    private void getPalette(Dialog dialog, Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> paintEmAll(dialog, palette));
    }

    private void paintEmAll(Dialog dialog, Palette mPalette) {
        Palette.Swatch mSwatch = mPalette.getDominantSwatch();
        if (mSwatch != null) {
            LinearLayout AppContainer = dialog.findViewById(R.id.app_container);
            LinearLayout ActionContainer = dialog.findViewById(R.id.action_container);
            View mView1 = dialog.findViewById(R.id.div1);
            View mView2 = dialog.findViewById(R.id.div2);
            View mView3 = dialog.findViewById(R.id.div3);

            AppContainer.setBackgroundColor(mSwatch.getRgb());
            ActionContainer.setBackgroundColor(mSwatch.getRgb());
            mView1.setBackgroundColor(mSwatch.getTitleTextColor());
            mView2.setBackgroundColor(mSwatch.getTitleTextColor());
            mView3.setBackgroundColor(mSwatch.getTitleTextColor());

            AppTitle.setTextColor(mSwatch.getBodyTextColor());
            AppSize.setTextColor(mSwatch.getBodyTextColor());
            AppExtra.setTextColor(mSwatch.getBodyTextColor());

            Blacklist.setTextColor(mSwatch.getBodyTextColor());
            Update.setTextColor(mSwatch.getBodyTextColor());
        }
    }

    @Override
    public int getItemCount() {
        return appsToAdd.size();
    }

    @Override
    public void removeApp(String packageName) {
        remove(packageName);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private CardView AppCard;
        private ImageView AppIcon;
        private ImageView AppMenu;
        private TextView AppTitle;
        private TextView AppSize;
        private TextView AppExtra;

        public ViewHolder(View view) {
            super(view);
            AppCard = view.findViewById(R.id.app_card);
            AppIcon = view.findViewById(R.id.app_icon);
            AppMenu = view.findViewById(R.id.menu_3dot);
            AppTitle = view.findViewById(R.id.app_title);
            AppSize = view.findViewById(R.id.app_size);
            AppExtra = view.findViewById(R.id.app_extra);
        }
    }
}
