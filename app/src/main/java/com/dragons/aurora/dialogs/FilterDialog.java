package com.dragons.aurora.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.dragons.aurora.Aurora;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.aurora.helpers.Prefs;
import com.dragons.custom.AuroraDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.graphics.ColorUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FilterDialog extends AppCompatDialogFragment {

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
    @BindView(R.id.close_filter)
    ImageView close_view;

    private View.OnClickListener onClickListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_filter, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AuroraDialog(getContext(), Util.isDark(getContext())
                ? R.style.Theme_Aurora_Dialog_Dark
                : R.style.Theme_Aurora_Dialog);
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
        int color_gsf = getResources().getColor(R.color.colorRed);
        chip_gsf.setChipBackgroundColor(ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(color_gsf, 100)));
        chip_gsf.setChipStrokeColor(ColorStateList.valueOf(color_gsf));
        chip_gsf.setChipStrokeWidth(2);
        chip_gsf.setOnCheckedChangeListener((v, isChecked) -> Prefs.putBoolean(getContext(),
                Aurora.FILTER_GSF_DEPENDENT_APPS, isChecked));
        chip_gsf.setChecked(Prefs.getBoolean(getContext(), Aurora.FILTER_GSF_DEPENDENT_APPS));

        int color_paid = getResources().getColor(R.color.colorPurple);
        chip_paid.setChipBackgroundColor(ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(color_paid, 100)));
        chip_paid.setChipStrokeColor(ColorStateList.valueOf(color_paid));
        chip_paid.setChipStrokeWidth(2);
        chip_paid.setOnCheckedChangeListener((v, isChecked) -> Prefs.putBoolean(getContext(),
                Aurora.FILTER_PAID_APPS, isChecked));
        chip_paid.setChecked(Prefs.getBoolean(getContext(), Aurora.FILTER_PAID_APPS));

        int color_ads = getResources().getColor(R.color.colorOrange);
        chip_ads.setChipBackgroundColor(ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(color_ads, 100)));
        chip_ads.setChipStrokeColor(ColorStateList.valueOf(color_ads));
        chip_ads.setChipStrokeWidth(2);
        chip_ads.setOnCheckedChangeListener((v, isChecked) -> Prefs.putBoolean(getContext(),
                Aurora.FILTER_APPS_WITH_ADS, isChecked));
        chip_ads.setChecked(Prefs.getBoolean(getContext(), Aurora.FILTER_APPS_WITH_ADS));
    }

    private void setupMultipleChips(Context mContext) {
        String downloadLabels[] = getResources().getStringArray(R.array.filterDownloadsLabels);
        String downloadValues[] = getResources().getStringArray(R.array.filterDownloadsValues);
        String ratingLabels[] = getResources().getStringArray(R.array.filterRatingLabels);
        String ratingValues[] = getResources().getStringArray(R.array.filterRatingValues);
        int colorShades[] = getResources().getIntArray(R.array.color_shades);

        int i = 0;
        for (String downloadLabel : downloadLabels) {
            final int pos = i;
            Chip mChip = new Chip(mContext);
            mChip.setChipIcon(getResources().getDrawable(R.drawable.circle_bg));
            mChip.setText(downloadLabel);
            mChip.setChipBackgroundColor(ColorStateList.valueOf(
                    ColorUtils.setAlphaComponent(colorShades[i], 100)));
            mChip.setChipStrokeColor(ColorStateList.valueOf(colorShades[i]));
            mChip.setChipStrokeWidth(2);
            mChip.setOnCheckedChangeListener((v, isChecked) -> {
                download_chips.clearCheck();
                mChip.setChecked(isChecked);
                if (isChecked) {
                    Prefs.putInteger(v.getContext(), Aurora.FILTER_DOWNLOADS,
                            Integer.parseInt(downloadValues[pos]));
                }
            });
            mChip.setChecked(Prefs.getInteger(getContext(), Aurora.FILTER_DOWNLOADS)
                    == Integer.parseInt(downloadValues[i]));
            download_chips.addView(mChip);
            i++;
        }

        i = 0;
        for (String ratingLabel : ratingLabels) {
            final int pos = i;
            Chip mChip = new Chip(mContext);
            mChip.setChipIcon(getResources().getDrawable(R.drawable.circle_bg));
            mChip.setText(ratingLabel);
            mChip.setChipBackgroundColor(ColorStateList.valueOf(
                    ColorUtils.setAlphaComponent(colorShades[i], 100)));
            mChip.setChipStrokeColor(ColorStateList.valueOf(colorShades[i]));
            mChip.setChipStrokeWidth(2);
            mChip.setOnCheckedChangeListener((v, isChecked) -> {
                rating_chips.clearCheck();
                mChip.setChecked(isChecked);
                if (isChecked) {
                    Prefs.putFloat(v.getContext(), Aurora.FILTER_RATING,
                            Float.parseFloat(ratingValues[pos]));
                }
            });
            mChip.setChecked(Prefs.getFloat(getContext(), Aurora.FILTER_RATING) ==
                    Float.parseFloat(ratingValues[i]));
            rating_chips.addView(mChip);
            i++;
        }
    }

    private void setupActions() {
        if (filter_apply != null) {
            filter_apply.setOnClickListener(onClickListener);
        }
        close_view.setOnClickListener(v -> dismiss());
    }

    public void setOnApplyListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }
}
