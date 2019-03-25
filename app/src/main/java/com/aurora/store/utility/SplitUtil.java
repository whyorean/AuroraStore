package com.aurora.store.utility;

import android.content.Context;

import java.util.ArrayList;

public class SplitUtil {

    private static final String PSEUDO_SPLIT_LIST = "PSEUDO_SPLIT_LIST";

    public static void addToList(Context context, String packageName) {
        ArrayList<String> splitList = PrefUtil.getListString(context, PSEUDO_SPLIT_LIST);
        splitList.add(packageName);
        PrefUtil.putListString(context, PSEUDO_SPLIT_LIST, splitList);
    }

    public static boolean isSplit(Context context, String packageName) {
        return PrefUtil.getListString(context, PSEUDO_SPLIT_LIST).contains(packageName);
    }
}