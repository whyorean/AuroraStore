package com.aurora.store.manager;

import android.content.Context;

import com.aurora.store.Constants;
import com.aurora.store.utility.PrefUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FavouriteListManager {

    private Context context;
    private ArrayList<String> favouriteList;

    public FavouriteListManager(Context context) {
        this.context = context;
        favouriteList = PrefUtil.getListString(context, Constants.PREFERENCE_FAVOURITE_LIST);
    }

    public boolean add(String s) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(s);
        boolean result = addAll(arrayList);
        save();
        return result;
    }

    public boolean addAll(ArrayList<String> arrayList) {
        boolean result = favouriteList.addAll(arrayList);
        Set<String> mAppSet = new HashSet<>(favouriteList);
        favouriteList.clear();
        favouriteList.addAll(mAppSet);
        save();
        return result;
    }

    public ArrayList<String> get() {
        return favouriteList;
    }

    public boolean contains(String packageName) {
        return favouriteList.contains(packageName);
    }

    public boolean remove(String packageName) {
        boolean result = favouriteList.remove(packageName);
        save();
        return result;
    }

    private void save() {
        PrefUtil.putListString(context, Constants.PREFERENCE_FAVOURITE_LIST, favouriteList);
    }
}
