package com.aurora.store.report;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aurora.store.util.Log;

import org.acra.ReportField;
import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

public class AcraLogSender implements ReportSender {
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData errorContent) throws ReportSenderException {
        Log.writeLogFile(errorContent.getString(ReportField.LOGCAT));
    }
}
