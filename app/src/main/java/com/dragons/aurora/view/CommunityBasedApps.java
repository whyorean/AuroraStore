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

package com.dragons.aurora.view;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dragons.aurora.R;
import com.dragons.aurora.adapters.CommunityBasedAppsAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CommunityBasedApps extends RelativeLayout {

    private RecyclerView cbased_recycler;

    public CommunityBasedApps(Context context) {
        super(context);
        init(context, null, 0);
    }

    public CommunityBasedApps(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public CommunityBasedApps(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        View view = inflate(context, R.layout.community_based_card, this);
        cbased_recycler = view.findViewById(R.id.cbased_recycler);
        JsonParser(context);
    }

    private void JsonParser(Context context) {
        List<CommunityBasedAppsAdapter.FeaturedHolder> FeaturedAppsHolder = new ArrayList<>();
        RequestQueue mRequestQueue = Volley.newRequestQueue(context);
        String JSON_PATH = "https://raw.githubusercontent.com/GalaxyStore/MetaData/master/community_apps.json";
        JsonArrayRequest req = new JsonArrayRequest(JSON_PATH,
                response -> {
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject inst = (JSONObject) response.get(i);
                            CommunityBasedAppsAdapter adapter = new CommunityBasedAppsAdapter(FeaturedAppsHolder);
                            CommunityBasedAppsAdapter.FeaturedHolder apps = new CommunityBasedAppsAdapter.FeaturedHolder(
                                    inst.getString("title"),
                                    inst.getString("id"),
                                    inst.getString("developer"),
                                    inst.getString("icon"),
                                    inst.getDouble("rating"),
                                    inst.getString("price"));
                            FeaturedAppsHolder.add(apps);
                            cbased_recycler.setAdapter(adapter);
                            cbased_recycler.setLayoutManager(new GridLayoutManager(context, 4));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.w("JSON_ERROR", "Error: " + e.getMessage());
                    }
                }, error -> Log.w("JSON_ERROR", "Error: " + error.getMessage()));
        mRequestQueue.add(req);
    }
}
