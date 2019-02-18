package com.aurora.store.task;

import android.content.Context;

import com.dragons.aurora.playstoreapiv2.UserProfile;

import java.io.IOException;

public class UserProfiler extends BaseTask {
    public UserProfiler(Context context) {
        super(context);
    }

    public UserProfile getUserProfile() throws IOException {
        return getApi().userProfile().getUserProfile();
    }
}
