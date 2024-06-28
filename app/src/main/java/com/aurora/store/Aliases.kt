package com.aurora.store

import com.aurora.gplayapi.data.models.StreamBundle
import com.aurora.gplayapi.data.models.StreamCluster
import com.aurora.gplayapi.helpers.contracts.StreamContract
import com.aurora.gplayapi.helpers.contracts.TopChartsContract

typealias TopChartStash = Map<TopChartsContract.Type, Map<TopChartsContract.Chart, StreamCluster>>
typealias HomeStash = MutableMap<StreamContract.Category, StreamBundle>