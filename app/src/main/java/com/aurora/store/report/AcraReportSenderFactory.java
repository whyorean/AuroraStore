package com.aurora.store.report;

import android.content.Context;

import androidx.annotation.NonNull;

import org.acra.config.CoreConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;

public class AcraReportSenderFactory implements ReportSenderFactory {
    @NonNull
    @Override
    public ReportSender create(@NonNull Context context, @NonNull CoreConfiguration config) {
        return new AcraReportSender();
    }

    @Override
    public boolean enabled(@NonNull CoreConfiguration config) {
        return true;
    }
}
