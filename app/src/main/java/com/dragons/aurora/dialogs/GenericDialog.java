package com.dragons.aurora.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dragons.aurora.R;
import com.dragons.aurora.Util;
import com.dragons.custom.AuroraDialog;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;

public class GenericDialog extends AppCompatDialogFragment {

    @BindView(R.id.dialog_title)
    TextView DialogTitle;
    @BindView(R.id.dialog_message)
    TextView DialogMessage;
    @BindView(R.id.button_negative)
    Button NegativeButton;
    @BindView(R.id.button_positive)
    Button PositiveButton;

    private View view;
    private String dialogTitle;
    private String dialogMessage;
    private String buttonPositiveText;
    private String buttonNegativeText;
    private View.OnClickListener buttonPositiveListener;
    private View.OnClickListener buttonNegativeListener;

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    public String getDialogMessage() {
        return dialogMessage;
    }

    public void setDialogMessage(String dialogMessage) {
        this.dialogMessage = dialogMessage;
    }

    public void setPositiveButton(String Text, View.OnClickListener onClickListener) {
        this.buttonPositiveText = Text;
        this.buttonPositiveListener = onClickListener;
    }

    public void setNegativeButton(String Text, View.OnClickListener onClickListener) {
        this.buttonNegativeText = Text;
        this.buttonNegativeListener = onClickListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.dialog_generic, container, false);
        return view;
    }

    @NotNull
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
        draw();
    }

    private void draw() {
        DialogTitle.setText(getDialogTitle().isEmpty() ? "" : getDialogTitle());
        DialogMessage.setText(getDialogMessage().isEmpty() ? "" : getDialogMessage());
        NegativeButton.setText(buttonNegativeText);
        NegativeButton.setOnClickListener(buttonNegativeListener);
        PositiveButton.setText(buttonPositiveText);
        PositiveButton.setOnClickListener(buttonPositiveListener);
    }
}
