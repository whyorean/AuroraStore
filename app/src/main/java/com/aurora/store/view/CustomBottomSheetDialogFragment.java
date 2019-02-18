package com.aurora.store.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.aurora.store.R;
import com.aurora.store.utility.Util;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CustomBottomSheetDialogFragment extends BottomSheetDialogFragment {

    @Override
    public int getTheme() {
        return getSelectedTheme(getContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    private int getSelectedTheme(Context context) {
        String theme = Util.getTheme(context);
        switch (theme) {
            case "light":
                return R.style.BottomSheetDialogTheme;
            case "dark":
                return R.style.BottomSheetDialogTheme_Dark;
            case "black":
                return R.style.BottomSheetDialogTheme_Black;
            default:
                return R.style.BottomSheetDialogTheme;
        }
    }
}
