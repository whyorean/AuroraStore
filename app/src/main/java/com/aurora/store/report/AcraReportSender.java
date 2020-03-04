package com.aurora.store.report;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

public class AcraReportSender implements ReportSender {
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData errorContent) throws ReportSenderException {
        try {
            Gson gson = new Gson();
            Intent intent = new Intent(context, AcraErrorActivity.class);
            intent.putExtra("ERROR_CONTENT", gson.toJson(errorContent));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
