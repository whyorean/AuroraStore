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

package com.aurora.store.sheet;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.utility.PrefUtil;
import com.aurora.store.view.CustomBottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FilterBottomSheet extends CustomBottomSheetDialogFragment {

    @BindView(R.id.rating_chips)
    ChipGroup rating_chips;
    @BindView(R.id.download_chips)
    ChipGroup download_chips;
    @BindView(R.id.filter_gfs)
    Chip chip_gsf;
    @BindView(R.id.filter_ads)
    Chip chip_ads;
    @BindView(R.id.filter_paid)
    Chip chip_paid;
    @BindView(R.id.filter_apply)
    Button filter_apply;

    private Context context;
    private View.OnClickListener onClickListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        setupMultipleChips(getContext());
        setupSingleChips();
        setupActions();
    }

    private void setupSingleChips() {
        applyStyles(chip_gsf, getResources().getColor(R.color.colorRed));
        chip_gsf.setOnCheckedChangeListener((v, isChecked) ->
                PrefUtil.putBoolean(context, Constants.FILTER_GSF_DEPENDENT_APPS, isChecked));
        chip_gsf.setChecked(PrefUtil.getBoolean(context, Constants.FILTER_GSF_DEPENDENT_APPS));

        applyStyles(chip_paid, getResources().getColor(R.color.colorPurple));
        chip_paid.setOnCheckedChangeListener((v, isChecked) ->
                PrefUtil.putBoolean(context, Constants.FILTER_PAID_APPS, isChecked));
        chip_paid.setChecked(PrefUtil.getBoolean(context, Constants.FILTER_PAID_APPS));

        applyStyles(chip_ads, getResources().getColor(R.color.colorOrange));
        chip_ads.setOnCheckedChangeListener((v, isChecked) ->
                PrefUtil.putBoolean(context, Constants.FILTER_APPS_WITH_ADS, isChecked));
        chip_ads.setChecked(PrefUtil.getBoolean(context, Constants.FILTER_APPS_WITH_ADS));
    }

    private void setupMultipleChips(Context mContext) {
        String[] downloadLabels = getResources().getStringArray(R.array.filterDownloadsLabels);
        String[] downloadValues = getResources().getStringArray(R.array.filterDownloadsValues);
        String[] ratingLabels = getResources().getStringArray(R.array.filterRatingLabels);
        String[] ratingValues = getResources().getStringArray(R.array.filterRatingValues);
        int[] colorShades = getResources().getIntArray(R.array.colorShades);

        int i = 0;
        for (String downloadLabel : downloadLabels) {
            final int pos = i;
            Chip chip = new Chip(mContext);
            chip.setText(downloadLabel);
            applyStyles(chip, colorShades[i]);
            chip.setOnCheckedChangeListener((v, isChecked) -> {
                download_chips.clearCheck();
                chip.setChecked(isChecked);
                if (isChecked) {
                    PrefUtil.putInteger(v.getContext(), Constants.FILTER_DOWNLOADS,
                            Integer.parseInt(downloadValues[pos]));
                }
            });
            chip.setChecked(PrefUtil.getInteger(mContext, Constants.FILTER_DOWNLOADS)
                    == Integer.parseInt(downloadValues[i]));
            download_chips.addView(chip);
            i++;
        }

        i = 0;
        for (String ratingLabel : ratingLabels) {
            final int pos = i;
            Chip chip = new Chip(mContext);
            applyStyles(chip, colorShades[i]);
            chip.setText(ratingLabel);
            chip.setOnCheckedChangeListener((v, isChecked) -> {
                rating_chips.clearCheck();
                chip.setChecked(isChecked);
                if (isChecked) {
                    PrefUtil.putFloat(v.getContext(), Constants.FILTER_RATING,
                            Float.parseFloat(ratingValues[pos]));
                }
            });
            chip.setChecked(PrefUtil.getFloat(mContext, Constants.FILTER_RATING) ==
                    Float.parseFloat(ratingValues[i]));
            rating_chips.addView(chip);
            i++;
        }
    }

    private void applyStyles(Chip chip, int color) {
        chip.setChipIconSize(64);
        chip.setChipIcon(context.getDrawable(R.drawable.circle_bg));
        chip.setChipIconTint(ColorStateList.valueOf(color));
        chip.setChipIconVisible(true);
        chip.setChipBackgroundColor(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 100)));
        chip.setChipStrokeColor(ColorStateList.valueOf(color));
        chip.setChipStrokeWidth(2);
        chip.setCheckedIcon(context.getDrawable(R.drawable.ic_chip_checked));
    }

    private void setupActions() {
        if (filter_apply != null) {
            filter_apply.setOnClickListener(onClickListener);
        }
    }

    public void setOnApplyListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }
}
