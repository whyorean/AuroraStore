package com.aurora.store.report;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.aurora.store.BuildConfig;
import com.aurora.store.R;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.util.ThemeUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.apache.commons.lang3.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.kbiakov.codeview.CodeView;
import io.github.kbiakov.codeview.adapters.Options;
import io.github.kbiakov.codeview.highlight.ColorTheme;
import lombok.SneakyThrows;

public class AcraErrorActivity extends BaseActivity {

    @BindView(R.id.code_view)
    CodeView codeView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private StringBuilder reportBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bug);
        ButterKnife.bind(this);
        setupActionBar();
        onNewIntent(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.crash_title);
        }
    }

    @SneakyThrows
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final String errorJson = intent.getStringExtra("ERROR_CONTENT");

        if (errorJson != null) {
            Gson gson = new Gson();
            reportBuilder = new StringBuilder();
            CrashReportData crashReportData = gson.fromJson(errorJson, CrashReportData.class);

            reportBuilder.append(crashReportData.getString(ReportField.BUILD_CONFIG))
                    .append(StringUtils.LF)
                    .append(crashReportData.getString(ReportField.BUILD))
                    .append(StringUtils.LF)
                    .append(crashReportData.getString(ReportField.ENVIRONMENT))
                    .append(StringUtils.LF)
                    .append(crashReportData.getString(ReportField.STACK_TRACE))
                    .append(StringUtils.LF);

            String stack_trace = crashReportData.getString(ReportField.STACK_TRACE);

            codeView.setOptions(Options.Default.get(this)
                    .withLanguage("java")
                    .withCode(stack_trace)
                    .withTheme(getColorTheme())
            );
        }
    }

    private ColorTheme getColorTheme() {
        if (ThemeUtil.isLightTheme(this))
            return ColorTheme.DEFAULT;
        else
            return ColorTheme.MONOKAI;
    }

    @OnClick(R.id.btn_positive)
    public void btnPositiveEvent() {
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_logo)
                .setTitle(R.string.privacy_title)
                .setMessage(R.string.privacy_policy_desc)
                .setCancelable(false)
                .setNeutralButton(R.string.privacy_policy, (dialog, which) -> {
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_url)));
                    startActivity(webIntent);
                })
                .setPositiveButton(R.string.action_accept, (dialog, which) -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("text/plain");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"crashreports.aurora@gmail.com"});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Aurora Store : Bug Report (" + BuildConfig.VERSION_NAME + ")");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, reportBuilder.toString());
                    startActivity(Intent.createChooser(emailIntent, "Send Email"));
                })
                .setNegativeButton(R.string.action_decline, (dialog, which) -> {

                })
                .show();
    }

    @OnClick(R.id.btn_negative)
    public void setBtnNegative() {
        finishAfterTransition();
    }
}
