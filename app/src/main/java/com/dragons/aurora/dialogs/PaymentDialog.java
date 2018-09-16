package com.dragons.aurora.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.custom.AuroraDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class PaymentDialog extends AppCompatDialogFragment {

    private static final String paytmUrl = "https://image.ibb.co/hvvOge/paytm.jpg";

    @BindView(R.id.paytm_qr)
    ImageView paytm_qr;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_payment, container, false);
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

        Glide.with(getContext())
                .load(paytmUrl)
                .apply(new RequestOptions()
                        .centerCrop()
                        .priority(Priority.HIGH))
                .into(paytm_qr);

    }
}
