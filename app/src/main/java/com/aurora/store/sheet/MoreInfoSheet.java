package com.aurora.store.sheet;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.aurora.store.view.CustomBottomSheetDialogFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MoreInfoSheet extends CustomBottomSheetDialogFragment {

    @BindView(R.id.content_readMore)
    TextView contentReadMore;

    private App app;

    public MoreInfoSheet() {
    }

    public void setApp(App app) {
        this.app = app;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_read_more, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contentReadMore.setText(Html.fromHtml(app.getDescription()).toString());
    }
}
