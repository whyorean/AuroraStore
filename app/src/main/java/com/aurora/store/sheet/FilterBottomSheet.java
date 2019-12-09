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
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.aurora.store.R;
import com.aurora.store.manager.FilterManager;
import com.aurora.store.model.FilterModel;
import com.aurora.store.util.ImageUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FilterBottomSheet extends BottomSheetDialogFragment {

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
    private FilterModel filterModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_filter, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        filterModel = FilterManager.getFilterPreferences(context);
        setupMultipleChips();
        setupSingleChips();
    }

    @OnClick(R.id.filter_apply)
    public void applyFilter() {
        FilterManager.saveFilterPreferences(context, filterModel);
        dismiss();
    }

    private void setupSingleChips() {
        chip_gsf.setChecked(filterModel.isGsfDependentApps());
        chip_paid.setChecked(filterModel.isPaidApps());
        chip_ads.setChecked(filterModel.isAppsWithAds());

        chip_gsf.setOnCheckedChangeListener((v, isChecked) -> filterModel.setGsfDependentApps(isChecked));
        chip_paid.setOnCheckedChangeListener((v, isChecked) -> filterModel.setPaidApps(isChecked));
        chip_ads.setOnCheckedChangeListener((v, isChecked) -> filterModel.setAppsWithAds(isChecked));

        applyStyles(chip_gsf, 0);
        applyStyles(chip_ads, 1);
        applyStyles(chip_paid, 2);
    }

    private void setupMultipleChips() {
        String[] downloadLabels = getResources().getStringArray(R.array.filterDownloadsLabels);
        String[] downloadValues = getResources().getStringArray(R.array.filterDownloadsValues);
        String[] ratingLabels = getResources().getStringArray(R.array.filterRatingLabels);
        String[] ratingValues = getResources().getStringArray(R.array.filterRatingValues);

        int i = 0;
        for (String downloadLabel : downloadLabels) {
            final int pos = i;
            Chip chip = new Chip(context);
            applyStyles(chip, i);
            chip.setText(downloadLabel);
            chip.setOnCheckedChangeListener((v, isChecked) -> {
                download_chips.clearCheck();
                chip.setChecked(isChecked);
                if (isChecked) {
                    filterModel.setDownloads(Integer.parseInt(downloadValues[pos]));
                }
            });
            chip.setChecked(filterModel.getDownloads() == Integer.parseInt(downloadValues[i]));
            download_chips.addView(chip);
            i++;
        }

        i = 0;
        for (String ratingLabel : ratingLabels) {
            final int pos = i;
            Chip chip = new Chip(context);
            applyStyles(chip, i);
            chip.setText(ratingLabel);
            chip.setOnCheckedChangeListener((v, isChecked) -> {
                rating_chips.clearCheck();
                chip.setChecked(isChecked);
                if (isChecked) {
                    filterModel.setRating(Float.parseFloat(ratingValues[pos]));
                }
            });
            chip.setChecked(filterModel.getRating() == Float.parseFloat(ratingValues[i]));
            rating_chips.addView(chip);
            i++;
        }
    }

    private void applyStyles(Chip chip, int index) {
        int color = ImageUtil.getSolidColor(index);
        chip.setChipIcon(ImageUtil.getDrawable(index, GradientDrawable.OVAL));
        chip.setCheckedIcon(context.getDrawable(R.drawable.ic_filter_check));
        chip.setChipIconVisible(true);
        chip.setChipBackgroundColor(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 100)));
        chip.setChipStrokeColor(ColorStateList.valueOf(color));
    }
}
