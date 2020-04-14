package com.aurora.store.util.diff;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.aurora.store.ui.accounts.AccountsActivity;
import com.aurora.store.ui.intro.IntroActivity;
import com.aurora.store.ui.main.AuroraActivity;

public class NavigationUtil {

    public static void launchAuroraActivity(Context context) {
        Intent intent = new Intent(context, AuroraActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static void launchAccountsActivity(Context context) {
        Intent intent = new Intent(context, AccountsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static void launchIntroActivity(Context context) {
        Intent intent = new Intent(context, IntroActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}
