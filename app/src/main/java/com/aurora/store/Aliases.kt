package com.aurora.store

import androidx.activity.result.ActivityResult
import com.aurora.gplayapi.data.models.Category
import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.contracts.TopChartsContract

typealias MR = com.google.android.material.R.attr

typealias TopChartStash = MutableMap<TopChartsContract.Type, MutableMap<TopChartsContract.Chart, StreamCluster>>
typealias HomeStash = MutableMap<StreamContract.Category, StreamBundle>
typealias CategoryStash = MutableMap<Category.Type, List<Category>>
typealias AppStreamStash = MutableMap<String, StreamBundle>

typealias PermissionCallback = (ActivityResult) -> Unit
