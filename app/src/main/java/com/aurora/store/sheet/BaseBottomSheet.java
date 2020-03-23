package com.aurora.store.sheet;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.util.ThemeUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;

public class BaseBottomSheet extends BottomSheetDialogFragment {

    protected int intExtra;
    protected String stringExtra;
    protected Gson gson = new Gson();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(),
                ThemeUtil.isLightTheme(requireContext())
                        ? R.style.Aurora_BottomSheetDialog_Light
                        : R.style.Aurora_BottomSheetDialog);

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.sheet_base, null);
        bottomSheetDialog.setContentView(dialogView);

        FrameLayout container = dialogView.findViewById(R.id.container);
        View contentView = onCreateContentView(LayoutInflater.from(requireContext()), container, savedInstanceState);
        if (contentView != null) {
            onContentViewCreated(contentView, savedInstanceState);
            container.addView(contentView);
        }

        bottomSheetDialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null)
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        return bottomSheetDialog;
    }

    @Nullable
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {

    }
}
