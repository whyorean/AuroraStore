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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.manager.FilterManager;
import com.aurora.store.model.FilterModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FilterBottomSheet extends BaseBottomSheet {

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

    private FilterModel filterModel;

    public FilterBottomSheet() {
    }

    @NonNull
    @Override
    public View onCreateContentView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_filter, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);
        filterModel = FilterManager.getFilterPreferences(requireContext());
        setupMultipleChips();
        setupSingleChips();
    }

    @OnClick(R.id.btn_positive)
    public void applyFilter() {
        FilterManager.saveFilterPreferences(requireContext(), filterModel);
        dismissAllowingStateLoss();
    }

    @OnClick(R.id.btn_negative)
    public void closeFilter() {
        dismissAllowingStateLoss();
    }

    private void setupSingleChips() {
        chip_gsf.setChecked(filterModel.isGsfDependentApps());
        chip_paid.setChecked(filterModel.isPaidApps());
        chip_ads.setChecked(filterModel.isAppsWithAds());

        chip_gsf.setOnCheckedChangeListener((v, isChecked) -> filterModel.setGsfDependentApps(isChecked));
        chip_paid.setOnCheckedChangeListener((v, isChecked) -> filterModel.setPaidApps(isChecked));
        chip_ads.setOnCheckedChangeListener((v, isChecked) -> filterModel.setAppsWithAds(isChecked));
    }

    private void setupMultipleChips() {
        String[] downloadLabels = getResources().getStringArray(R.array.filterDownloadsLabels);
        String[] downloadValues = getResources().getStringArray(R.array.filterDownloadsValues);
        String[] ratingLabels = getResources().getStringArray(R.array.filterRatingLabels);
        String[] ratingValues = getResources().getStringArray(R.array.filterRatingValues);

        int i = 0;
        for (String downloadLabel : downloadLabels) {
            Chip chip = new Chip(requireContext());
            chip.setId(i);
            chip.setText(downloadLabel);
            chip.setChecked(filterModel.getDownloads() == Integer.parseInt(downloadValues[i]));
            download_chips.addView(chip);
            i++;
        }

        download_chips.setOnCheckedChangeListener((group, checkedId) ->
                filterModel.setDownloads(Integer.parseInt(downloadValues[checkedId])));

        i = 0;
        for (String ratingLabel : ratingLabels) {
            Chip chip = new Chip(requireContext());
            chip.setId(i);
            chip.setText(ratingLabel);
            chip.setChecked(filterModel.getRating() == Float.parseFloat(ratingValues[i]));
            rating_chips.addView(chip);
            i++;
        }

        rating_chips.setOnCheckedChangeListener((group, checkedId) ->
                filterModel.setRating(Float.parseFloat(ratingValues[checkedId])));

    }
}
