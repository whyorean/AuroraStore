package com.aurora.store.manager;

import android.content.Context;

import com.aurora.store.Constants;
import com.aurora.store.utility.PrefUtil;

import java.util.HashSet;
import java.util.Set;

public class BlacklistManager {

    private Context context;

    public BlacklistManager(Context context) {
        this.context = context;
    }

    public Set<String> getBlacklistedApps() {
        return PrefUtil.getStringSet(context, Constants.PREFERENCE_BLACKLIST_APPS_SET);
    }

    public void addSelectionsToBlackList(Set<String> set) {
        PrefUtil.putStringSet(context, Constants.PREFERENCE_BLACKLIST_APPS_SET, set);
    }

    public void removeSelectionsFromBlackList(Set<String> set) {
        Set<String> hiddenApps = PrefUtil.getStringSet(context, Constants.PREFERENCE_BLACKLIST_APPS_SET);
        if (hiddenApps != null && !hiddenApps.isEmpty()) {
            set.removeAll(hiddenApps);
        }
        PrefUtil.putStringSet(context, Constants.PREFERENCE_BLACKLIST_APPS_SET, set);
    }

    public Set<String> getGoogleApps() {
        Set<String> shitSet = new HashSet<>();
        shitSet.add("com.google.android.marvin.talkback");
        shitSet.add("com.google.android.projection.gearhead");
        shitSet.add("com.google.android.webview");
        shitSet.add("com.google.android.tv.remote");
        shitSet.add("com.google.ar.core");
        shitSet.add("com.google.android.apps.blogger");
        shitSet.add("com.google.android.calculator");
        shitSet.add("com.google.samples.apps.cardboarddemo");
        shitSet.add("com.google.vr.cyclops");
        shitSet.add("com.google.android.ims");
        shitSet.add("com.chrome.beta");
        shitSet.add("com.chrome.canary");
        shitSet.add("com.chrome.dev");
        shitSet.add("com.google.chromeremotedesktop");
        shitSet.add("com.google.android.deskclock");
        shitSet.add("com.google.android.apps.ads.express");
        shitSet.add("com.google.android.apps.cloudconsole");
        shitSet.add("com.google.android.apps.cloudprint");
        shitSet.add("com.google.android.contacts");
        shitSet.add("com.google.android.apps.freighter");
        shitSet.add("com.google.android.apps.wellbeing");
        shitSet.add("com.google.android.apps.nbu.files");
        shitSet.add("com.google.android.inputmethod.latin");
        shitSet.add("com.google.android.gm");
        shitSet.add("com.google.android.apps.enterprise.cpanel");
        shitSet.add("com.google.android.apps.adwords");
        shitSet.add("com.google.android.apps.ads.publisher");
        shitSet.add("com.google.android.apps.fireball");
        shitSet.add("com.google.android.apps.giant");
        shitSet.add("com.google.android.apps.enterprise.dmagent");
        shitSet.add("com.google.android.apps.cultural");
        shitSet.add("com.google.android.apps.googleassistant");
        shitSet.add("com.google.android.apps.authenticator2");
        shitSet.add("com.google.android.calendar");
        shitSet.add("com.android.chrome");
        shitSet.add("com.google.android.apps.classroom");
        shitSet.add("com.google.android.apps.docs.editors.docs");
        shitSet.add("com.google.android.apps.docs");
        shitSet.add("com.google.android.apps.tachyon");
        shitSet.add("com.google.earth");
        shitSet.add("com.google.android.apps.kids.familylinkhelper");
        shitSet.add("com.google.android.apps.kids.familylink");
        shitSet.add("com.google.android.apps.adm");
        shitSet.add("com.google.android.apps.fitness");
        shitSet.add("com.google.android.apps.searchlite");
        shitSet.add("com.google.android.apps.handwriting.ime");
        shitSet.add("com.google.android.apps.chromecast.app");
        shitSet.add("com.google.android.apps.inputmethod.hindi");
        shitSet.add("com.google.android.inputmethod.japanese");
        shitSet.add("com.google.android.keep");
        shitSet.add("com.google.ar.lens");
        shitSet.add("com.google.android.apps.mapslite");
        shitSet.add("com.google.android.apps.vega");
        shitSet.add("com.google.android.apps.m4b");
        shitSet.add("com.google.android.apps.magazines");
        shitSet.add("com.google.android.launcher");
        shitSet.add("com.google.android.apps.subscriptions.red");
        shitSet.add("com.google.android.apps.paidtasks");
        shitSet.add("com.google.android.apps.nbu.paisa.user");
        shitSet.add("com.google.android.apps.pdfviewer");
        shitSet.add("com.google.android.apps.photos");
        shitSet.add("com.google.android.inputmethod.pinyin");
        shitSet.add("com.google.android.apps.books");
        shitSet.add("com.google.android.apps.playconsole");
        shitSet.add("com.google.android.play.games");
        shitSet.add("com.google.android.videos");
        shitSet.add("com.google.android.music");
        shitSet.add("com.google.android.apps.podcasts");
        shitSet.add("com.google.android.apps.docs.editors.sheets");
        shitSet.add("com.google.android.apps.docs.editors.slides");
        shitSet.add("com.google.android.street");
        shitSet.add("com.google.android.apps.tasks");
        shitSet.add("com.google.android.tts");
        shitSet.add("com.google.android.apps.translate");
        shitSet.add("com.google.android.apps.travel.onthego");
        shitSet.add("com.google.vr.vrcore");
        shitSet.add("com.google.android.apps.access.wifi.consumer");
        shitSet.add("com.google.android.apps.plus");
        shitSet.add("com.google.android.talk");
        shitSet.add("com.google.android.apps.dynamite");
        shitSet.add("com.google.android.apps.hangoutsdialer");
        shitSet.add("com.google.android.apps.meetings");
        shitSet.add("com.google.android.apps.maps");
        shitSet.add("com.google.tango.measure");
        shitSet.add("com.google.android.apps.messaging");
        shitSet.add("com.google.android.apps.navlite");
        shitSet.add("com.google.android.apps.nbu.society");
        shitSet.add("com.google.android.apps.photos.scanner");
        shitSet.add("com.google.android.apps.forscience.whistlepunk");
        shitSet.add("com.niksoftware.snapseed");
        shitSet.add("com.google.toontastic");
        shitSet.add("com.google.android.apps.accessibility.voiceaccess");
        shitSet.add("com.google.android.apps.wallpaper");
        shitSet.add("com.google.android.wearable.app");
        shitSet.add("com.google.android.youtube");
        shitSet.add("com.google.android.apps.youtube.gaming");
        shitSet.add("com.google.android.apps.youtube.mango");
        shitSet.add("com.google.android.apps.youtube.kids");
        shitSet.add("com.google.android.apps.youtube.creator");
        shitSet.add("com.google.android.googlequicksearchbox");
        shitSet.add("com.google.samples.apps.iosched");
        return shitSet;
    }
}
