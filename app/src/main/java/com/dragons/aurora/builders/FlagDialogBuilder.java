/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dragons.aurora.builders;

import android.content.Context;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.dragons.aurora.R;
import com.dragons.aurora.activities.AuroraActivity;
import com.dragons.aurora.model.App;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.task.playstore.FlagTask;

import androidx.appcompat.app.AlertDialog;

public class FlagDialogBuilder {

    static private final GooglePlayAPI.ABUSE[] reasonIds = new GooglePlayAPI.ABUSE[]{
            GooglePlayAPI.ABUSE.SEXUAL_CONTENT,
            GooglePlayAPI.ABUSE.GRAPHIC_VIOLENCE,
            GooglePlayAPI.ABUSE.HATEFUL_OR_ABUSIVE_CONTENT,
            GooglePlayAPI.ABUSE.HARMFUL_TO_DEVICE_OR_DATA,
            GooglePlayAPI.ABUSE.IMPROPER_CONTENT_RATING,
            GooglePlayAPI.ABUSE.ILLEGAL_PRESCRIPTION,
            GooglePlayAPI.ABUSE.IMPERSONATION,
            GooglePlayAPI.ABUSE.OTHER,
    };
    static private final String[] reasonLabels = new String[8];

    private AuroraActivity activity;
    private App app;

    public FlagDialogBuilder setActivity(AuroraActivity activity) {
        this.activity = activity;
        reasonLabels[0] = activity.getString(R.string.flag_sexual_content);
        reasonLabels[1] = activity.getString(R.string.flag_graphic_violence);
        reasonLabels[2] = activity.getString(R.string.flag_hateful_content);
        reasonLabels[3] = activity.getString(R.string.flag_harmful_to_device);
        reasonLabels[4] = activity.getString(R.string.flag_improper_content_rating);
        reasonLabels[5] = activity.getString(R.string.flag_pharma_content);
        reasonLabels[6] = activity.getString(R.string.flag_impersonation_copycat);
        reasonLabels[7] = activity.getString(R.string.flag_other_objection);
        return this;
    }

    public FlagDialogBuilder setApp(App app) {
        this.app = app;
        return this;
    }

    public AlertDialog build() {
        return new AlertDialog.Builder(activity, R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
                .setTitle(R.string.flag_page_description)
                .setNegativeButton(
                        android.R.string.cancel,
                        (dialog, which) -> dialog.dismiss()
                )
                .setAdapter(new ArrayAdapter<>(activity, R.layout.item_dialog_singlechoice, reasonLabels),
                        (dialog, which) -> {
                            FlagTask task = new FlagTask();
                            task.setContext(activity);
                            task.setApp(app);
                            GooglePlayAPI.ABUSE reason = reasonIds[which];
                            task.setReason(reason);
                            if (reason == GooglePlayAPI.ABUSE.HARMFUL_TO_DEVICE_OR_DATA || reason == GooglePlayAPI.ABUSE.OTHER) {
                                new ExplanationDialogBuilder().setContext(activity).setTask(task).setReason(reason).build();
                            } else {
                                task.execute();
                            }
                            dialog.dismiss();
                        }
                ).create();
    }

    private static class ExplanationDialogBuilder {

        private Context context;
        private FlagTask task;
        private GooglePlayAPI.ABUSE reason;

        public ExplanationDialogBuilder setContext(Context context) {
            this.context = context;
            return this;
        }

        public ExplanationDialogBuilder setTask(FlagTask task) {
            this.task = task;
            return this;
        }

        public ExplanationDialogBuilder setReason(GooglePlayAPI.ABUSE reason) {
            this.reason = reason;
            return this;
        }

        public void build() {
            LinearLayout container = new LinearLayout(context);
            final EditText editText = new EditText(context);
            editText.setGravity(android.view.Gravity.TOP | Gravity.START);
            container.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(15, 15, 15, 15);
            container.setLayoutParams(params);
            container.addView(editText, params);
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.ThemeOverlay_MaterialComponents_Dialog_Alert);
            builder.setTitle(reason == GooglePlayAPI.ABUSE.HARMFUL_TO_DEVICE_OR_DATA
                    ? R.string.flag_harmful_prompt
                    : R.string.flag_other_concern_prompt)
                    .setView(container)
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(
                            android.R.string.yes,
                            (dialog, which) -> {
                                task.setExplanation(editText.getText().toString());
                                task.execute();
                                dialog.dismiss();
                            })
                    .create()
                    .show();
        }
    }
}
